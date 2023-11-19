package org.jrd.backend.data.cli;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jrd.backend.communication.ErrorCandidate;
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
import org.jrd.backend.data.cli.utils.FqnAndClassToJar;
import org.jrd.backend.data.cli.utils.PluginWithOptions;
import org.jrd.backend.data.cli.utils.PluginWrapperWithMetaInfo;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.frontend.frame.main.decompilerview.verifiers.ClassVerifier;
import org.jrd.frontend.frame.main.decompilerview.verifiers.GetSetText;
import org.objectweb.asm.ClassReader;

public final class Lib {

    private Lib() {
    }

    public static void initClass(VmInfo vmInfo, VmManager vmManager, String fqn, PrintStream outputMessageStream) {
        String response = initClassNoThrow(vmInfo, vmManager, fqn);

        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            outputMessageStream.println("Initialization of class '" + fqn + "' successful.");
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static String initClassNoThrow(VmInfo vmInfo, VmManager vmManager, String fqn) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.INIT_CLASS, fqn);
        return DecompilationController.submitRequest(vmManager, request);
    }

    public static String guessName(byte[] fileContents) throws IOException {
        String[] r = guessNameImpl(fileContents);
        if (r.length == 1) {
            return r[0];
        } else {
            return r[0] + "." + r[1];
        }
    }

    @SuppressWarnings("CyclomaticComplexity") // un-refactorable
    public static String[] guessNameImpl(byte[] fileContents) throws IOException {
        String pkg = null;
        String clazz = null;

        try (
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContents), StandardCharsets.UTF_8))
        ) {
            while (true) {
                if (clazz != null && pkg != null) {
                    return new String[]{pkg, clazz}; // this return should be most likely everywhere inline
                }

                String line = br.readLine();
                if (line == null) { // reached end of reader
                    if (pkg == null && clazz == null) {
                        throw new RuntimeException("Neither package nor class was found.");
                    }
                    if (clazz == null) {
                        throw new RuntimeException("Class not found for package '" + pkg + "'.");
                    }
                    if (pkg == null) {
                        return new String[]{clazz};
                    }

                    return new String[]{pkg, clazz};
                }

                line = line.trim();
                line = line.replaceAll("/{2,}?", "// "); // jasm-g is using this, and si compilabel...
                // but jasm compielr do not eed class name...
                // but maye it woudl be better to get rid of most of the comemtns...
                String[] commands = line.split(";");

                for (String command : commands) {
                    String[] words = command.split("\\s+");

                    for (int i = 0; i < words.length; i++) {
                        String keyWord = words[i];

                        if ("0xCAFEBABE".equals(keyWord)) { // jcoder uses / and fully qualified class name
                            String fqn = clazz.replace("/", ".");
                            if (fqn.contains(".")) {
                                int lastDot = fqn.lastIndexOf('.');
                                return new String[]{fqn.substring(0, lastDot), fqn.substring(lastDot + 1)};
                            } else {
                                return new String[]{fqn};
                            }
                        }
                        if ("package".equals(keyWord)) {
                            if (pkg == null) {
                                pkg = words[i + 1].replace("/", "."); // jasm uses / instead of .
                            }
                        }
                        if ("class".equals(keyWord) || "interface".equals(keyWord) || "enum".equals(keyWord)) {
                            if (clazz == null) {
                                clazz = words[i + 1];
                            }
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

    public static List<ClassInfo> obtainFilteredClasses(
            VmInfo vmInfo, VmManager vmManager, List<Pattern> filter, boolean details, Optional<String> search, Optional<String> classloader
    ) throws IOException {
        List<ClassInfo> allClasses;
        if (search.isPresent()) {
            if (filter.size() != 1) {
                throw new RuntimeException("Search allows only one regex, you have " + filter.size());
            }
            String ecodedSearch = Base64.getEncoder().encodeToString(search.get().getBytes(StandardCharsets.UTF_8));
            if (details) {
                allClasses = Arrays.stream(searchWithClassesDetails(vmInfo, vmManager, ecodedSearch, filter.get(0).pattern(), classloader))
                        .collect(Collectors.toList());
            } else {
                allClasses = Arrays.stream(searchClasses(vmInfo, vmManager, ecodedSearch, filter.get(0).pattern(), classloader))
                        .map(a -> new ClassInfo(a, null, null)).collect(Collectors.toList());
            }
        } else {
            if (details) {
                allClasses = Arrays.stream(obtainClassesDetails(vmInfo, vmManager, classloader)).collect(Collectors.toList());
            } else {
                allClasses = Arrays.stream(obtainClasses(vmInfo, vmManager, classloader)).map(a -> new ClassInfo(a, null, null))
                        .collect(Collectors.toList());
            }
        }
        List<ClassInfo> filteredClasses = new ArrayList<>(allClasses.size());
        for (ClassInfo clazz : allClasses) {
            if (matchesAtLeastOne(clazz, filter)) {
                filteredClasses.add(clazz);
            }
        }
        return filteredClasses;
    }

    public static int[] getByteCodeVersions(ClassInfo clazz, VmInfo vmInfo, VmManager vmManager) {
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

    public static
            String[]
            searchClasses(VmInfo vmInfo, VmManager manager, String searchedSusbtring, String regex, Optional<String> classloader) {
        Pattern.compile(regex);
        if (searchedSusbtring == null || searchedSusbtring.trim().isEmpty()) {
            throw new RuntimeException("empty substring");
        }
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.SEARCH_CLASSES;
        String[] params = new String[]{searchedSusbtring, regex, "false"};
        if (classloader.isPresent()) {
            params = new String[]{searchedSusbtring, regex, "false", optionalLoaderToParam(classloader)};
        }
        AgentRequestAction request =
                DecompilationController.createRequest(vmInfo, requestType, Arrays.stream(params).collect(Collectors.joining(" ")));
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClassNames();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static String[] obtainClasses(VmInfo vmInfo, VmManager manager, Optional<String> classloader) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES;
        AgentRequestAction request;
        if (classloader.isPresent()) {
            request = DecompilationController.createRequest(vmInfo, requestType, optionalLoaderToParam(classloader));
        } else {
            request = DecompilationController.createRequest(vmInfo, requestType);
        }
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

    public static ClassInfo[] obtainClassesDetails(VmInfo vmInfo, VmManager manager, Optional<String> classloader) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES_WITH_INFO;
        AgentRequestAction request;
        if (classloader.isPresent()) {
            request = DecompilationController.createRequest(vmInfo, requestType, optionalLoaderToParam(classloader));
        } else {
            request = DecompilationController.createRequest(vmInfo, requestType);
        }
        String response = DecompilationController.submitRequest(manager, request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClasses();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static ClassInfo[] searchWithClassesDetails(
            VmInfo vmInfo, VmManager manager, String searchedSusbtring, String regex, Optional<String> classloader
    ) {
        Pattern.compile(regex);
        if (searchedSusbtring == null || searchedSusbtring.trim().isEmpty()) {
            throw new RuntimeException("empty substring");
        }
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.SEARCH_CLASSES;
        String[] params = new String[]{searchedSusbtring, regex, "true"};
        if (classloader.isPresent()) {
            params = new String[]{searchedSusbtring, regex, "true", optionalLoaderToParam(classloader)};
        }
        AgentRequestAction request =
                DecompilationController.createRequest(vmInfo, requestType, Arrays.stream(params).collect(Collectors.joining(" ")));
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

    public static String addJar(VmInfo vmInfo, boolean isBoot, String jarName, String jarBytesInBase64, VmManager vmManager) {
        AgentRequestAction request = DecompilationController
                .createRequest(vmInfo, AgentRequestAction.RequestAction.ADD_JAR, getPrefixByBoot(isBoot) + "/" + jarName, jarBytesInBase64);
        String response = DecompilationController.submitRequest(vmManager, request);
        return response;
    }

    public static PluginWrapperWithMetaInfo getPluginWrapper(PluginManager pluginManager, String pluginIdOrNonsense, boolean doThrow) {
        PluginWithOptions wrapper = null;
        boolean haveCompiler = false;
        try {
            wrapper = Lib.getDecompilerFromString(pluginIdOrNonsense, pluginManager);
            if (wrapper.getDecompiler() == null) {
                wrapper = null;
                throw new RuntimeException();
            }
            haveCompiler = pluginManager.hasBundledCompiler(wrapper.getDecompiler());
        } catch (Exception ex) {
            //plugin not found == default compiler
            if (doThrow) {
                throw ex;
            }
        }
        return new PluginWrapperWithMetaInfo(wrapper, haveCompiler);
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
        VmInfo vmInfo = vmManager.createRemoteVM(agent.getHost(), agent.getPort(), "" + agent.getPid(), false, null);
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

    public static InMemoryJar jarFromClasses(String[] fqnFilePairs, Object[] lastCarier) throws IOException {
        InMemoryJar imjar = new InMemoryJar();
        for (int x = 0; x < fqnFilePairs.length; x = x + 2) {
            String lastAddedFqn = fqnFilePairs[x];
            File lastAddedFile = new File(fqnFilePairs[x + 1]);
            lastCarier[0] = lastAddedFqn;
            lastCarier[1] = lastAddedFile;
            imjar.addFile(Files.readAllBytes(lastAddedFile.toPath()), lastAddedFqn);
        }
        imjar.close();
        return imjar;
    }

    public static String getPrefixByBoot(boolean boot) {
        String prefix = CliSwitches.SYSTEM_CLASS_LOADER;
        if (boot) {
            prefix = CliSwitches.BOOT_CLASS_LOADER;
        }
        return prefix;
    }

    public static String readClassNameFromClass(byte[] b) {
        ClassReader cr = new ClassReader(b);
        return cr.getClassName().replace('/', '.');
    }

    public static String addFileClassesViaJar(VmInfo vmInfo, List<FqnAndClassToJar> toJar, boolean isBoot, VmManager vmManager)
            throws IOException {
        InMemoryJar jar = new InMemoryJar();
        jar.open();
        for (FqnAndClassToJar item : toJar) {
            GetSetText fakeInput = new GetSetText.DummyGetSet(item.getFile().getAbsolutePath());
            GetSetText fakeOutput = new GetSetText.DummyGetSet("ok?");
            boolean passed = new ClassVerifier(fakeInput, fakeOutput).verifySource(null);
            if (!passed) {
                throw new RuntimeException(item.getFile().getAbsolutePath() + " " + fakeOutput.getText());
            }
            jar.addFile(Files.readAllBytes(item.getFile().toPath()), item.getFqn());
        }
        jar.close();
        //the jar is saved to tmp with random name via agent alter
        return Lib.addJar(
                vmInfo, isBoot, "custom" + toJar.size() + "classes.jar", Base64.getEncoder().encodeToString(jar.toBytes()), vmManager
        );
    }

    public static String addByteClassesViaJar(VmInfo vmInfo, List<IdentifiedBytecode> toJar, boolean isBoot, VmManager vmManager)
            throws IOException {
        InMemoryJar jar = new InMemoryJar();
        jar.open();
        for (IdentifiedBytecode item : toJar) {
            jar.addFile(item.getFile(), item.getClassIdentifier().getFullName());
        }
        jar.close();
        //the jar is saved to tmp with random name via agent alter
        return Lib.addJar(
                vmInfo, isBoot, "custom" + toJar.size() + "classes.jar", Base64.getEncoder().encodeToString(jar.toBytes()), vmManager
        );
    }

    public static Integer getDefaultRemoteBytecodelevelCatched(VmInfo vmInfo, VmManager vmManager) {
        try {
            return getDefaultRemoteBytecodelevel(vmInfo, vmManager);
        } catch (Exception ex) {
            Logger.getLogger().log(ex);
            return null;
        }
    }

    public static Integer getDefaultRemoteBytecodelevel(VmInfo vmInfo, VmManager vmManager) {
        String className = java.lang.Object.class.getName();
        return getDefaultRemoteBytecodelevel(vmInfo, vmManager, className);
    }

    public static Integer getDefaultRemoteBytecodelevel(VmInfo vmInfo, VmManager vmManager, String className) {
        String base64Bytes = Lib.obtainClass(vmInfo, className, vmManager).getLoadedClassBytes();
        ErrorCandidate errorCandidateRemote = new ErrorCandidate(base64Bytes);
        if (errorCandidateRemote.isError()) {
            throw new RuntimeException(className + " not found on local nor remote paths/vm"); //not probable, see the init check above
        }
        return Lib.getBuildJavaPerVersion(Base64.getDecoder().decode(base64Bytes));
    }

    public static String optionalLoaderToParam(Optional<String> classloader) {
        if (classloader.isPresent()) {
            return DecompilationController.stringToBase64(classloader.get());
        } else {
            return null;
        }
    }

}
