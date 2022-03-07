package org.jrd.backend.data.cli;

import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Lib {

    private Lib() {
    }

    public static void initClass(VmInfo vmInfo, VmManager vmManager, String fqn, PrintStream outputMessageStream) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.INIT_CLASS, fqn);
        String response = DecompilationController.submitRequest(vmManager, request);

        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            outputMessageStream.println("Initialization of class '" + fqn + "' successful.");
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    @SuppressWarnings("CyclomaticComplexity") // un-refactorable
    public static String guessName(byte[] fileContents) throws IOException {
        String pkg = null;
        String clazz = null;

        try (
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContents), StandardCharsets.UTF_8))
        ) {
            while (true) {
                if (clazz != null && pkg != null) {
                    return pkg + "." + clazz; // this return should be most likely everywhere inline
                }

                String line = br.readLine();
                if (line == null) { // reached end of reader
                    if (pkg == null && clazz == null) {
                        throw new RuntimeException("Neither package nor class was found.");
                    }
                    if (pkg == null) {
                        throw new RuntimeException("Package not found for class '" + clazz + "'.");
                    }
                    if (clazz == null) {
                        throw new RuntimeException("Class not found for package '" + pkg + "'.");
                    }

                    return pkg + "." + clazz;
                }

                line = line.trim();
                String[] commands = line.split(";");

                for (String command : commands) {
                    String[] words = command.split("\\s+");

                    for (int i = 0; i < words.length; i++) {
                        String keyWord = words[i];

                        if ("0xCAFEBABE".equals(keyWord)) { // jcoder uses / and fully qualified class name
                            return clazz.replace("/", ".");
                        }
                        if ("package".equals(keyWord)) {
                            pkg = words[i + 1].replace("/", "."); // jasm uses / instead of .
                        }
                        if ("class".equals(keyWord) || "interface".equals(keyWord) || "enum".equals(keyWord)) {
                            clazz = words[i + 1];
                        }
                    }
                }
            }
        }
    }

    public static DecompilerWrapper findDecompiler(String decompilerName, PluginManager pluginManager) {
        List<DecompilerWrapper> wrappers = pluginManager.getWrappers();
        DecompilerWrapper decompiler = null;

        for (DecompilerWrapper wrapper : wrappers) {
            if (!wrapper.isLocal() && wrapper.getName().equals(decompilerName)) {
                decompiler = wrapper;
            }
        }
        // LOCAL is preferred one
        for (DecompilerWrapper wrapper : wrappers) {
            if (wrapper.isLocal() && wrapper.getName().equals(decompilerName)) {
                decompiler = wrapper;
            }
        }

        return decompiler;
    }

    static List<ClassInfo> obtainFilteredClasses(VmInfo vmInfo, VmManager vmManager, List<Pattern> filter, boolean details)
            throws IOException {
        List<ClassInfo> allClasses;
        if (details) {
            allClasses = Arrays.stream(obtainClassesDetails(vmInfo, vmManager)).collect(Collectors.toList());
        } else {
            allClasses =
                    Arrays.stream(obtainClasses(vmInfo, vmManager)).map(a -> new ClassInfo(a, null, null)).collect(Collectors.toList());
        }
        List<ClassInfo> filteredClasses = new ArrayList<>(allClasses.size());
        for (ClassInfo clazz : allClasses) {
            if (matchesAtLeastOne(clazz, filter)) {
                filteredClasses.add(clazz);
            }
        }
        return filteredClasses;
    }

    static int[] getByteCodeVersions(ClassInfo clazz, VmInfo vmInfo, VmManager vmManager) {
        VmDecompilerStatus result = obtainClass(vmInfo, clazz.getName(), vmManager);
        byte[] source = Base64.getDecoder().decode(result.getLoadedClassBytes());
        int bytecodeVersion = BytecodeDecompilerView.getByteCodeVersion(source);
        int buildJavaPerVersion = BytecodeDecompilerView.getJavaFromBytelevel(bytecodeVersion);
        return new int[]{bytecodeVersion, buildJavaPerVersion};
    }

    static boolean matchesAtLeastOne(ClassInfo clazz, List<Pattern> filter) {
        for (Pattern p : filter) {
            if (p.matcher(clazz.getName()).matches()) {
                return true;
            }
        }

        return false;
    }

    public static String[] obtainClasses(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, (String) null);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClassNames();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static String[] obtainOverrides(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.OVERRIDES;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, (String) null);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClassNames();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static void removeOverrides(VmInfo vmInfo, VmManager manager, String regex) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.REMOVE_OVERRIDES;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, regex);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return;
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static ClassInfo[] obtainClassesDetails(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES_WITH_INFO;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, (String) null);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClasses();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static VmDecompilerStatus obtainClass(VmInfo vmInfo, String clazz, VmManager manager) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.BYTES, clazz);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static VmDecompilerStatus obtainVersion(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.VERSION);
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static void detach(String host, int port, VmManager vmManager) {
        DecompilerRequestReceiver.getHaltAction(host, port, "none", 0, new AgentAttachManager(vmManager), vmManager, false);
        Logger.getLogger().log(host + ":" + port + " should be detached successfully");
    }

}
