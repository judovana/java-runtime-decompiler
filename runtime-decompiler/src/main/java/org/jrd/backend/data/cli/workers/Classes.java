package org.jrd.backend.data.cli.workers;

import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.Saving;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class Classes {

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final boolean hex;
    private final Saving saving;
    private final String classloader;

    public Classes(List<String> filteredArgs, VmManager vmManager, boolean hex, Saving saving, String classloader) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
        this.hex = hex;
        this.saving = saving;
        this.classloader = classloader;
    }

    public VmInfo searchClasses() throws IOException {
        if (filteredArgs.size() != 5) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.SEARCH_FORMAT + "'.");
        }
        boolean details = Boolean.parseBoolean(filteredArgs.get(4));
        String substring = filteredArgs.get(3);
        filteredArgs.remove(4);
        filteredArgs.remove(3);
        return listClasses(details, hex, Optional.of(substring));
    }

    public VmInfo listClasses(boolean details, boolean bytecodeVersion, Optional<String> search) throws IOException {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.LIST_CLASSES_FORMAT + "'.");
        }
        VmInfo vmInfo = getVmInfo();
        List<Pattern> classRegexes = getPatterns();
        listClassesFromVmInfo(vmInfo, classRegexes, details, bytecodeVersion, search);
        return vmInfo;
    }

    private VmInfo getVmInfo() {
        return CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
    }

    public VmInfo countLoaders() throws IOException {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.LIST_CLASSLOADERS_FORMAT + "'.");
        }
        VmInfo vmInfo = getVmInfo();
        List<Pattern> classRegexes = getPatterns();
        countLoadersFromClassesFromVmInfo(vmInfo, classRegexes);
        return vmInfo;
    }

    private List<Pattern> getPatterns() {
        List<Pattern> classRegexes = new ArrayList<>(filteredArgs.size() - 1);

        if (filteredArgs.size() == 2) {
            classRegexes.add(Pattern.compile(".*"));
        } else {
            for (int i = 2; i < filteredArgs.size(); i++) {
                classRegexes.add(Pattern.compile(filteredArgs.get(i)));
            }
        }
        return classRegexes;
    }

    private void countLoadersFromClassesFromVmInfo(VmInfo vmInfo, List<Pattern> filter) throws IOException {
        List<ClassInfo> model =
                Lib.obtainFilteredClasses(vmInfo, vmManager, filter, true, Optional.empty(), Optional.ofNullable(classloader));
        Map<String, Integer> usedLoaders = new HashMap<>();
        for (int x = 0; x < model.size(); x++) {
            ClassInfo ci = model.get(x);
            Integer occurences = usedLoaders.get(ci.getClassLoader());
            if (occurences == null) {
                usedLoaders.put(ci.getClassLoader(), 1);
            } else {
                occurences = occurences.intValue() + 1;
                usedLoaders.put(ci.getClassLoader(), occurences);
            }
        }
        List<Map.Entry<String, Integer>> sorted = getSortedEntries(usedLoaders);
        for (Map.Entry<String, Integer> loader : sorted) {
            String s = loader.getKey() + " (" + loader.getValue() + ")";
            System.out.println(s);
        }

    }

    public static List<Map.Entry<String, Integer>> getSortedEntries(Map<String, Integer> usedLoaders) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(usedLoaders.entrySet());
        sorted.sort(new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> first, Map.Entry<String, Integer> second) {
                return first.getValue() - second.getValue();
            }
        });
        return sorted;
    }

    private void listClassesFromVmInfo(
            VmInfo vmInfo, List<Pattern> filter, boolean details, boolean bytecodeVersion, Optional<String> search
    ) throws IOException {
        List<ClassInfo> classes = Lib.obtainFilteredClasses(vmInfo, vmManager, filter, details, search, Optional.ofNullable(classloader));
        if (saving.shouldSave()) {
            if (saving.getLike().equals(Saving.DEFAULT) || saving.getLike().equals(Saving.EXACT)) {
                try (
                        PrintWriter pw =
                                new PrintWriter(new OutputStreamWriter(new FileOutputStream(saving.getAs()), StandardCharsets.UTF_8))
                ) {
                    for (ClassInfo clazz : classes) {
                        String bytecodes = getBytecodesString(vmInfo, details, bytecodeVersion, clazz, false);
                        pw.println(clazz.toPrint(details) + bytecodes);
                    }
                }
            } else {
                throw new RuntimeException(
                        "Only '" + Saving.DEFAULT + "' and '" + Saving.EXACT + "' are allowed for class listing saving."
                );
            }
        } else {
            for (ClassInfo clazz : classes) {
                String bytecodes = getBytecodesString(vmInfo, details, bytecodeVersion, clazz, false);
                System.out.println(clazz.toPrint(details) + bytecodes);
            }
        }
    }

    private String getBytecodesString(VmInfo vmInfo, boolean details, boolean bytecodeVersion, ClassInfo clazz, boolean doThrow) {
        String bytecodes = "";
        if (bytecodeVersion) {
            try {
                int[] versions = Lib.getByteCodeVersions(clazz, vmInfo, vmManager);
                //double space metters
                bytecodes = "  JDK " + versions[1] + " (bytecode: " + versions[0] + ")";
            } catch (Exception ex) {
                bytecodes = "  bytecode level unknown";
                if (doThrow) {
                    throw new RuntimeException(bytecodes, ex);
                }
            }
            if (details) {
                bytecodes = "\n" + bytecodes;
            }
        }
        return bytecodes;
    }
}
