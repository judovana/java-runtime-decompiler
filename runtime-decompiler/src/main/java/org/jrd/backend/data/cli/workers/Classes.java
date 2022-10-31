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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class Classes {

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final boolean hex;
    private final Saving saving;

    public Classes(List<String> filteredArgs, VmManager vmManager, boolean hex, Saving saving) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
        this.hex = hex;
        this.saving = saving;
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

        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        List<Pattern> classRegexes = new ArrayList<>(filteredArgs.size() - 1);

        if (filteredArgs.size() == 2) {
            classRegexes.add(Pattern.compile(".*"));
        } else {
            for (int i = 2; i < filteredArgs.size(); i++) {
                classRegexes.add(Pattern.compile(filteredArgs.get(i)));
            }
        }
        listClassesFromVmInfo(vmInfo, classRegexes, details, bytecodeVersion, search);
        return vmInfo;
    }

    private void listClassesFromVmInfo(
            VmInfo vmInfo, List<Pattern> filter, boolean details, boolean bytecodeVersion, Optional<String> search
    ) throws IOException {
        List<ClassInfo> classes = Lib.obtainFilteredClasses(vmInfo, vmManager, filter, details, search);
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
