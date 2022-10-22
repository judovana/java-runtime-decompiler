package org.jrd.backend.data.cli;

import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.core.agentstore.KnownAgent;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        int bytecodeVersion = Lib.getByteCodeVersion(source);
        int buildJavaPerVersion = Lib.getJavaFromBytelevel(bytecodeVersion);
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

    public static PluginWithOptions getDecompilerFromString(String plugin, PluginManager pm) {
        DecompilerWrapper decompiler;
        String[] options = null;
        if (plugin.startsWith(DecompilerWrapper.JAVAP_NAME)) {
            options = Arrays.stream(plugin.split("-")).skip(1) // do not include "javap" in options
                    .map(s -> "-" + s).toArray(String[]::new);
            decompiler = findDecompiler(DecompilerWrapper.JAVAP_NAME, pm);
        } else {
            decompiler = findDecompiler(plugin, pm);
        }
        if (decompiler == null) {
            throw new RuntimeException("Plugin '" + plugin + "' not found.");
        }
        return new PluginWithOptions(decompiler, options);
    }

    public static String decompileBytesByDecompilerName(
            String base64Bytes, String pluginName, String className, VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager
    ) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64Bytes);
        return decompileBytesByDecompilerName(bytes, pluginName, className, vmInfo, vmManager, pluginManager);
    }

    public static String decompileBytesByDecompilerName(
            byte[] bytes, String pluginName, String className, VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager
    ) throws Exception {
        PluginWithOptions pwo = Lib.getDecompilerFromString(pluginName, pluginManager);
        String decompilationResult = pluginManager.decompile(pwo.getDecompiler(), className, bytes, pwo.getOptions(), vmInfo, vmManager);
        return decompilationResult;
    }

    public static String uploadClass(VmInfo vmInfo, String className, byte[] bytes, VmManager vmManager) {
        return uploadClass(vmInfo, className, Base64.getEncoder().encodeToString(bytes), vmManager);
    }

    public static String uploadClass(VmInfo vmInfo, String className, String clazzBytesInBase64, VmManager vmManager) {
        AgentRequestAction request =
                DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.OVERWRITE, className, clazzBytesInBase64);
        String response = DecompilationController.submitRequest(vmManager, request);
        return response;
    }

    public static String addClass(VmInfo vmInfo, String className, String clazzBytesInBase64, VmManager vmManager) {
        AgentRequestAction request =
                DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.ADD_CLASS, className, clazzBytesInBase64);
        String response = DecompilationController.submitRequest(vmManager, request);
        return response;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "VmInfo is public class.. not sure..")
    public static class HandhshakeResult {
        private final VmInfo vmInfo;
        private final String agentVersion;
        private final String diff;

        public HandhshakeResult(VmInfo vmInfo, String loadedClassBytes, String compare) {
            this.vmInfo = vmInfo;
            this.agentVersion = loadedClassBytes;
            this.diff = compare;
        }

        public VmInfo getVmInfo() {
            return vmInfo;
        }

        public String getAgentVersion() {
            return agentVersion;
        }

        public String getDiff() {
            return diff;
        }
    }

    public static HandhshakeResult handshakeAgent(KnownAgent agent, VmManager vmManager) {
        VmInfo vmInfo = vmManager.createRemoteVM(agent.getHost(), agent.getPort(), "" + agent.getPid());
        return handshakeAgent(agent, vmInfo, vmManager);
    }

    public static HandhshakeResult handshakeAgent(KnownAgent agent, VmInfo vmInfo, VmManager vmManager) {
        VmDecompilerStatus vs = Lib.obtainVersion(vmInfo, vmManager);
        String version = vs.getLoadedClassBytes();
        String ortel = MetadataProperties.getInstance().compare(vs.getLoadedClassBytes());
        if (version == null || version.trim().isEmpty()) {
            version = "unknown version";
        }
        return new HandhshakeResult(vmInfo, version, ortel);
    }

    public static int getBuildJavaPerVersion(byte[] source) {
        int bytecodeVersion = getByteCodeVersion(source);
        int buildJavaPerVersion = getJavaFromBytelevel(bytecodeVersion);
        return buildJavaPerVersion;
    }

    public static int getJavaFromBytelevel(int bytecodeVersion) {
        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.1
        // Oracle's Java Virtual Machine implementation in JDK release 1.0.2 supports class file format
        // versions 45.0 through 45.3 inclusive.
        // JDK releases 1.1.* support class file format versions in the range 45.0 through 45.65535 inclusive.
        // For k ≥ 2, JDK release 1.k supports class file format versions in the range 45.0 through 44+k.0 inclusive.
        // https://javaalmanac.io/bytecode/versions/
        int r = bytecodeVersion - 44;
        if (r <= 1) {
            r = 1;
        }
        return r;
    }

    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE"}, justification = "the dead stores are here for clarity and possible future usage")
    public static int getByteCodeVersion(byte[] source) {
        if (source == null || source.length < 8) {
            return 0;
        }
        //https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
        //u4             magic;
        //u2             minor_version;
        //u2             major_version;
        int b1 = source[4]; //minor
        int b2 = source[5]; //minor
        int b3 = source[6]; //major
        int b4 = source[7]; //major
        return b4;
    }

}
