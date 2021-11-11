package org.jrd.backend.data;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.KnownAgents;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.filesystem.NewFsVmController;
import org.jrd.frontend.frame.main.DecompilationController;
import org.jrd.frontend.frame.overwrite.FileToClassValidator;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.AgentApiGenerator;
import org.jrd.frontend.utility.CommonUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cli {

    protected static final String VERBOSE = "-verbose";
    protected static final String SAVE_AS = "-saveas";
    protected static final String SAVE_LIKE = "-savelike";
    protected static final String LIST_JVMS = "-listjvms";
    protected static final String LIST_PLUGINS = "-listplugins";
    protected static final String LIST_CLASSES = "-listclasses";
    protected static final String LIST_CLASSESDETAILS = "-listdetails";
    protected static final String BASE64 = "-base64bytes";
    protected static final String BYTES = "-bytes";
    protected static final String DECOMPILE = "-decompile";
    protected static final String COMPILE = "-compile";
    protected static final String OVERWRITE = "-overwrite";
    protected static final String INIT = "-init";
    protected static final String ATTACH = "-attach";
    protected static final String DETACH = "-detach";
    protected static final String API = "-api";
    protected static final String VERSION = "-version";
    protected static final String HELP = "-help";
    protected static final String H = "-h";

    protected static final String R = "-r";
    protected static final String P = "-p";
    protected static final String CP = "-cp";

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private Saving saving;
    private boolean isVerbose;

    static class Saving implements CommonUtils.StatusKeeper {
        static final String DEFAULT = "default";
        static final String EXACT = "exact";
        static final String FQN = "fqn";
        static final String DIR = "dir";
        private final String as;
        private final String like;

        Saving(String as, String like) {
            this.as = as;
            if (like == null) {
                this.like = DEFAULT;
            } else {
                this.like = like;
            }
        }

        public boolean shouldSave() {
            return as != null;
        }

        @Override
        public void setText(String s) {
            if (shouldSave()) {
                System.out.println(s);
            } else {
                System.err.println(s);
            }
        }

        @Override
        public void onException(Exception ex) {
            Logger.getLogger().log(ex);
        }

        @SuppressWarnings("ReturnCount") // returns in switch cases
        public int toInt(String suffix) {
            switch (like) {
                case FQN:
                    return CommonUtils.FULLY_QUALIFIED_NAME;
                case EXACT:
                    return CommonUtils.CUSTOM_NAME;
                case DIR:
                    return CommonUtils.SRC_SUBDIRS_NAME;
                case DEFAULT:
                    if (".java".equals(suffix)) {
                        return CommonUtils.FULLY_QUALIFIED_NAME;
                    }
                    if (".class".equals(suffix)) {
                        return CommonUtils.SRC_SUBDIRS_NAME;
                    }
                    return CommonUtils.CUSTOM_NAME;
                default:
                    throw new RuntimeException("Unknown saving type: " + like + ". Allowed are: " + FQN + "," + DIR + "," + EXACT);
            }
        }

        public PrintStream openPrintStream() throws IOException {
            return new PrintStream(new FileOutputStream(this.as), true, "UTF-8");
        }
    }

    public Cli(String[] orig, Model model) {
        this.filteredArgs = prefilterArgs(orig);
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();
    }

    private static String cleanParameter(String param) {
        if (param.startsWith("-")) {
            return param.replaceAll("^--*", "-").toLowerCase();
        }

        return param; // do not make regexes and filenames lowercase
    }

    public boolean shouldBeVerbose() {
        return isVerbose;
    }

    public boolean isGui() {
        return filteredArgs.isEmpty();
    }

    @SuppressWarnings("ModifiedControlVariable") // shifting arguments when parsing
    private List<String> prefilterArgs(String[] originalArgs) {
        List<String> args = new ArrayList<>(originalArgs.length);
        String saveAs = null;
        String saveLike = null;

        for (int i = 0; i < originalArgs.length; i++) {
            String arg = originalArgs[i];
            String cleanedArg = cleanParameter(arg);

            if (cleanedArg.equals(VERBOSE)) {
                isVerbose = true;
            } else if (cleanedArg.equals(SAVE_AS)) {
                saveAs = originalArgs[i + 1];
                i++;
            } else if (cleanedArg.equals(SAVE_LIKE)) {
                saveLike = originalArgs[i + 1];
                i++;
            } else {
                args.add(arg);
            }
        }

        this.saving = new Saving(saveAs, saveLike);
        return args;
    }

    public void consumeCli() throws Exception {
        if (filteredArgs.isEmpty()) { // impossible in org.jrd.backend.Main#Main() control flow, but possible in tests
            return;
        }

        String operation = cleanParameter(filteredArgs.get(0));
        switch (operation) {
            case LIST_JVMS:
                listJvms();
                break;
            case LIST_PLUGINS:
                listPlugins();
                break;
            case LIST_CLASSES:
                listClasses(false);
                break;
            case LIST_CLASSESDETAILS:
                listClasses(true);
                break;
            case BYTES:
            case BASE64:
                boolean bytes = operation.equals(BYTES);
                printBytes(bytes);
                break;
            case DECOMPILE:
                decompile();
                break;
            case COMPILE:
                compile(new CompileArguments());
                break;
            case OVERWRITE:
                overwrite();
                break;
            case INIT:
                init();
                break;
            case ATTACH:
                attach();
                break;
            case DETACH:
                detach();
                break;
            case API:
                api();
                break;
            case HELP:
            case H:
                printHelp();
                break;
            case VERSION:
                printVersion();
                break;
            default:
                printHelp();
                throw new IllegalArgumentException("Unknown commandline argument '" + operation + "'.");
        }
    }

    private void overwrite() throws Exception {
        String newBytecodeFile;
        if (filteredArgs.size() == 3) {
            Logger.getLogger().log("Reading class for overwrite from stdin.");
            newBytecodeFile = null;
        } else {
            if (filteredArgs.size() != 4) {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.OVERWRITE_FORMAT + "'.");
            }
            newBytecodeFile = filteredArgs.get(3);
        }

        String className = filteredArgs.get(2);
        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        String clazz;

        if (newBytecodeFile == null) {
            clazz = DecompilationController.stdinToBase64();
        } else { // validate first
            FileToClassValidator.StringAndScore r = FileToClassValidator.validate(className, newBytecodeFile);

            if (r.getScore() > 0 && r.getScore() < 10) {
                Logger.getLogger().log(Logger.Level.ALL, "WARNING: " + r.getMessage());
            }
            if (r.getScore() >= 10) {
                Logger.getLogger().log(Logger.Level.ALL, "ERROR: " + r.getMessage());
            }

            clazz = DecompilationController.fileToBase64(newBytecodeFile);
        }

        AgentRequestAction request =
                DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.OVERWRITE, className, clazz);
        String response = DecompilationController.submitRequest(vmManager, request);

        if ("ok".equals(response)) {
            System.out.println("Overwrite of class '" + className + "' successful.");
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "The stream is clsoed as conditionally as is created")
    private void api() throws Exception {
        PrintStream out = System.out;
        try {
            if (saving != null && saving.as != null) {
                out = saving.openPrintStream();
            }

            if (filteredArgs.size() != 2) {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.API_FORMAT + "'.");
            }

            VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
            AgentApiGenerator.initItems(vmInfo, vmManager, pluginManager);
            out.println(AgentApiGenerator.getInterestingHelp());

            out.flush();
        } finally {
            if (saving != null && saving.as != null) {
                out.close();
            }
        }
    }

    private void init() throws Exception {
        if (filteredArgs.size() != 3) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.INIT_FORMAT + "'.");
        }
        String fqn = filteredArgs.get(2);
        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        initClass(vmInfo, vmManager, fqn, System.out);
    }

    public static void initClass(VmInfo vmInfo, VmManager vmManager, String fqn, PrintStream outputMessageStream) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.INIT_CLASS, fqn);
        String response = DecompilationController.submitRequest(vmManager, request);

        if ("ok".equals(response)) {
            outputMessageStream.println("Initialization of class '" + fqn + "' successful.");
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    private void attach() throws Exception {
        final int mandatoryParam = 2;
        if (filteredArgs.size() < mandatoryParam) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.ATTACH_FORMAT + "'.");
        }
        if (guessType(filteredArgs.get(1)) != VmInfo.Type.LOCAL) {
            throw new IllegalArgumentException("Sorry, first argument must be running jvm PID, nothing else.");
        }
        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        String[] futureType = new String[]{null, null, null};
        int[] futureTypeUnderstood = new int[]{0, 0, 0};
        for (int i = 0; i < 3; i++) {
            if (filteredArgs.size() > i + mandatoryParam) {
                futureType[i] = filteredArgs.get(i + mandatoryParam);
            }
        }
        KnownAgents.AgentLiveliness liveliness = null;
        for (int i = 0; i < 3; i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    liveliness = KnownAgents.AgentLiveliness.fromString(futureType[i]);
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        KnownAgents.AgentLoneliness loneliness = null;
        for (int i = 0; i < 3; i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    loneliness = KnownAgents.AgentLoneliness.fromString(futureType[i]);
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        Integer port = null;
        for (int i = 0; i < 3; i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    port = Integer.valueOf(futureType[i]);
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        boolean failed = false;
        for (int i = 0; i < 3; i++) {
            if (i + mandatoryParam >= filteredArgs.size()) {
                break;
            }
            if (futureTypeUnderstood[i] == 0) {
                failed = true;
                System.out.println("Cant parse " + (i + 1) + ".:" + futureType[i]);
            } else if (futureTypeUnderstood[i] > 1) {
                failed = true;
                System.out.println("applied more then once " + (i + 1) + ".:" + futureType[i]);
            }
        }
        if (failed) {
            System.out.println(Help.ATTACH_TEXT);
            System.out.println("exiting without action");
            return;
        }
        if (liveliness == null) {
            liveliness = KnownAgents.AgentLiveliness.SESSION;
        }
        if (loneliness == null) {
            loneliness = KnownAgents.AgentLoneliness.SINGLE_INSTANCE;
        }
        System.out.println(
                String.format("Attempt to attach %s, %s and port=%s", liveliness, loneliness, port == null ? "guessed" : port.toString())
        );
        VmDecompilerStatus status =
                new AgentAttachManager(vmManager).attachAgentToVm(vmInfo.getVmId(), vmInfo.getVmPid(), Optional.ofNullable(port));
        System.out.println("Attached. Listening on: " + status.getListenPort());
        if (liveliness == KnownAgents.AgentLiveliness.SESSION) {
            System.out.println(" kill this process (" + ProcessHandle.current().pid() + ") to detach agent");
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    detach(status.getListenPort());
                }
            });
            while (true) {
                Thread.sleep(1000);
            }
        } else if (liveliness == KnownAgents.AgentLiveliness.ONE_SHOT) {
            System.out.println("agent attached, and is detaching right away");
            detach(status.getListenPort());
        } else {
            System.out.println("agent agent is permanently attached to " + vmInfo.getVmPid() + " on " + status.getListenPort());
            System.out.println("exiting");
        }
    }

    private void detach() {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.DETACH_FORMAT + "'.");
        }
        if (filteredArgs.get(1).contains(":")) {
            String[] hostPort = filteredArgs.get(1).split(":");
            detach(hostPort[0], Integer.valueOf(hostPort[1]));
        } else {
            //is pid?
            detach(Integer.valueOf(filteredArgs.get(1)));
        }
    }

    private void detach(int port) {
        detach("localhost", port);
    }

    private void detach(String host, int port) {
        DecompilerRequestReceiver.getHaltAction(host, port, "none", 0, new AgentAttachManager(vmManager), vmManager, false);
        System.out.println("Should be detached successfully");
    }

    private final class CompileArguments {
        String wantedCustomCompiler;
        String puc;
        boolean isRecursive = false;
        List<File> filesToCompile = new ArrayList<>(1);

        @SuppressWarnings("ModifiedControlVariable")
        // shifting arguments when parsing
        CompileArguments() throws FileNotFoundException {
            for (int i = 1; i < filteredArgs.size(); i++) {
                String arg = filteredArgs.get(i);

                if (P.equals(arg)) {
                    wantedCustomCompiler = filteredArgs.get(i + 1);
                    i++; // shift
                } else if (CP.equals(arg)) {
                    puc = filteredArgs.get(i + 1);
                    i++; // shift
                } else if (R.equals(arg)) {
                    isRecursive = true;
                } else {
                    File fileToCompile = new File(arg);

                    if (!fileToCompile.exists()) {
                        throw new FileNotFoundException(fileToCompile.getAbsolutePath());
                    }

                    filesToCompile.add(fileToCompile);
                }
            }

            if (filesToCompile.isEmpty()) {
                throw new IllegalArgumentException("Expected at least one file for compile.");
            }
        }

        public ClassesProvider getClassesProvider() {
            if (puc == null) {
                return new ClassesProvider() {
                    @Override
                    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... names) {
                        return new ArrayList<>();
                    }

                    @Override
                    public List<String> getClassPathListing() {
                        return new ArrayList<>();
                    }
                };
            } else {
                return new RuntimeCompilerConnector.JrdClassesProvider(getVmInfo(puc), vmManager);
            }
        }

        public ClasspathlessCompiler getCompiler() {
            DecompilerWrapper decompiler = null;
            boolean hasCompiler = false;
            String compilerLogMessage = "Default runtime compiler will be used for overwrite.";

            if (wantedCustomCompiler != null) {
                decompiler = findDecompiler(wantedCustomCompiler);

                if (pluginManager.hasBundledCompiler(decompiler)) {
                    compilerLogMessage = wantedCustomCompiler + "'s bundled compiler will be used for overwrite.";
                    hasCompiler = true;
                }
            }
            Logger.getLogger().log(compilerLogMessage);
            return OverwriteClassDialog.getClasspathlessCompiler(decompiler, hasCompiler, isVerbose);
        }
    }

    private void compile(CompileArguments args) throws Exception {
        // handle -cp
        ClassesProvider provider = args.getClassesProvider();

        // handle -p
        ClasspathlessCompiler compiler = args.getCompiler();

        IdentifiedSource[] identifiedSources = CommonUtils.toIdentifiedSources(args.isRecursive, args.filesToCompile);
        Collection<IdentifiedBytecode> allBytecode =
                compiler.compileClass(provider, Optional.of((level, message) -> Logger.getLogger().log(message)), identifiedSources);

        boolean shouldUpload = false;

        // determine uploading
        if (saving.shouldSave()) {
            try {
                VmInfo.Type t = guessType(saving.as);
                if (t == VmInfo.Type.LOCAL || t == VmInfo.Type.REMOTE) {
                    shouldUpload = true;
                }
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
            }
        }

        if (shouldUpload) {
            VmInfo targetVm = getVmInfo(saving.as);
            int failCount = 0;

            for (IdentifiedBytecode bytecode : allBytecode) {
                String className = bytecode.getClassIdentifier().getFullName();
                Logger.getLogger().log("Uploading class '" + className + "'.");

                AgentRequestAction request = DecompilationController.createRequest(
                        targetVm, AgentRequestAction.RequestAction.OVERWRITE, className,
                        Base64.getEncoder().encodeToString(bytecode.getFile())
                );
                String response = DecompilationController.submitRequest(vmManager, request);

                if ("ok".equals(response)) {
                    Logger.getLogger().log("Successfully uploaded class '" + className + "'.");
                } else {
                    failCount++;
                    Logger.getLogger().log("Failed to upload class '" + className + "'.");
                }
            }

            if (failCount > 0) {
                throw new RuntimeException("Failed to upload " + failCount + " classes out of " + allBytecode.size() + " total.");
            } else {
                Logger.getLogger().log("Successfully uploaded all " + allBytecode.size() + " classes.");
            }
        } else {
            if (!saving.shouldSave() && allBytecode.size() > 1) {
                throw new IllegalArgumentException(
                        "Unable to print multiple classes to stdout. Either use saving modifiers or compile one class at a time."
                );
            }

            for (IdentifiedBytecode bytecode : allBytecode) {
                outOrSave(bytecode.getClassIdentifier().getFullName(), ".class", bytecode.getFile(), true);
            }
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

    private void decompile() throws Exception {
        if (filteredArgs.size() < 4) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.DECOMPILE_FORMAT + "'.");
        }

        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        String plugin = filteredArgs.get(2);
        int failCount = 0;
        int classCount = 0;

        for (int i = 3; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)), false).stream()
                    .map(a -> a.getName()).collect(Collectors.toList());

            for (String clazz : classes) {
                classCount++;
                VmDecompilerStatus result = obtainClass(vmInfo, clazz, vmManager);
                byte[] bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());

                if (new File(plugin).exists() && plugin.toLowerCase().endsWith(".json")) {
                    throw new RuntimeException("Plugin loading directly from file is not implemented.");
                }

                DecompilerWrapper decompiler;
                String[] options = null;
                if (plugin.startsWith(DecompilerWrapper.JAVAP_NAME)) {
                    options = Arrays.stream(plugin.split("-")).skip(1) // do not include "javap" in options
                            .map(s -> "-" + s).toArray(String[]::new);
                    decompiler = findDecompiler(DecompilerWrapper.JAVAP_NAME);
                } else {
                    decompiler = findDecompiler(plugin);
                }

                if (decompiler != null) {
                    String decompilationResult = pluginManager.decompile(decompiler, clazz, bytes, options, vmInfo, vmManager);

                    if (!outOrSave(clazz, ".java", decompilationResult)) {
                        failCount++;
                    }
                } else {
                    throw new RuntimeException("Plugin '" + plugin + "' not found.");
                }
            }
        }

        returnNonzero(failCount, classCount);
    }

    private void returnNonzero(int failures, int total) {
        if (total == 0) {
            throw new RuntimeException("No class found to save.");
        }
        if (failures > 0) {
            throw new RuntimeException("Failed to save " + failures + "classes.");
        }
    }

    private boolean outOrSave(String name, String extension, String s) throws IOException {
        return outOrSave(name, extension, s.getBytes(StandardCharsets.UTF_8), false);
    }

    private boolean outOrSave(String name, String extension, byte[] body, boolean forceBytes) throws IOException {
        if (saving.shouldSave()) {
            return CommonUtils.saveByGui(saving.as, saving.toInt(extension), extension, saving, name, body);
        } else {
            if (forceBytes) {
                System.out.write(body);
            } else {
                System.out.println(new String(body, StandardCharsets.UTF_8));
            }
            return true;
        }
    }

    private DecompilerWrapper findDecompiler(String decompilerName) {
        return findDecompiler(decompilerName, pluginManager);
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

    private void printBytes(boolean justBytes) throws Exception {
        if (filteredArgs.size() < 3) {
            throw new IllegalArgumentException(
                    "Incorrect argument count! Please use '" + (justBytes ? Help.BYTES_FORMAT : Help.BASE64_FORMAT) + "'."
            );
        }

        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        int failCount = 0;
        int classCount = 0;

        for (int i = 2; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)), false).stream()
                    .map(a -> a.getName()).collect(Collectors.toList());

            for (String clazz : classes) {
                classCount++;
                VmDecompilerStatus result = obtainClass(vmInfo, clazz, vmManager);

                byte[] bytes;
                if (justBytes) {
                    bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());
                } else {
                    bytes = result.getLoadedClassBytes().getBytes(StandardCharsets.UTF_8);
                }

                if (!outOrSave(clazz, ".class", bytes, justBytes)) {
                    failCount++;
                }
            }
        }

        returnNonzero(failCount, classCount);
    }

    private void listClasses(boolean details) throws IOException {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.LIST_CLASSES_FORMAT + "'.");
        }

        VmInfo vmInfo = getVmInfo(filteredArgs.get(1));
        List<Pattern> classRegexes = new ArrayList<>(filteredArgs.size() - 1);

        if (filteredArgs.size() == 2) {
            classRegexes.add(Pattern.compile(".*"));
        } else {
            for (int i = 2; i < filteredArgs.size(); i++) {
                classRegexes.add(Pattern.compile(filteredArgs.get(i)));
            }
        }

        listClassesFromVmInfo(vmInfo, classRegexes, details);
    }

    private static List<ClassInfo> obtainFilteredClasses(VmInfo vmInfo, VmManager vmManager, List<Pattern> filter, boolean details)
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

    private void listClassesFromVmInfo(VmInfo vmInfo, List<Pattern> filter, boolean details) throws IOException {
        List<ClassInfo> classes = obtainFilteredClasses(vmInfo, vmManager, filter, details);
        if (saving.shouldSave()) {
            if (saving.like.equals(Saving.DEFAULT) || saving.like.equals(Saving.EXACT)) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(saving.as), StandardCharsets.UTF_8))) {
                    for (ClassInfo clazz : classes) {
                        pw.println(clazz.toPrint(details));
                    }
                }
            } else {
                throw new RuntimeException(
                        "Only '" + Saving.DEFAULT + "' and '" + Saving.EXACT + "' are allowed for class listing saving."
                );
            }
        } else {
            for (ClassInfo clazz : classes) {
                System.out.println(clazz.toPrint(details));
            }
        }
    }

    private static boolean matchesAtLeastOne(ClassInfo clazz, List<Pattern> filter) {
        for (Pattern p : filter) {
            if (p.matcher(clazz.getName()).matches()) {
                return true;
            }
        }

        return false;
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "The stream is clsoed as conditionally as is created")
    private void listPlugins() throws IOException {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_PLUGINS + " does not expect arguments.");
        }

        PrintStream out = System.out;
        try {
            if (saving != null && saving.as != null) {
                out = saving.openPrintStream();
            }

            for (DecompilerWrapper dw : pluginManager.getWrappers()) {
                out.printf("%s %s/%s - %s%n", dw.getName(), dw.getScope(), invalidityToString(dw.isInvalidWrapper()), dw.getFileLocation());
            }

            out.flush();
        } finally {
            if (saving != null && saving.as != null) {
                out.close();
            }
        }
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "The stream is clsoed as conditionally as is created")
    private void listJvms() throws IOException {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_JVMS + " does not expect arguments.");
        }

        PrintStream out = System.out;
        try {
            if (saving != null && saving.as != null) {
                out = saving.openPrintStream();
            }

            for (VmInfo vmInfo : vmManager.getVmInfoSet()) {
                out.println(vmInfo.getVmPid() + " " + vmInfo.getVmName());
            }

            out.flush();
        } finally {
            if (saving != null && saving.as != null) {
                out.close();
            }
        }
    }

    private void printVersion() {
        System.out.println(MetadataProperties.getInstance());
    }

    private void printHelp() {
        Help.printHelpText();
    }

    private static String invalidityToString(boolean invalidWrapper) {
        if (invalidWrapper) {
            return "invalid";
        } else {
            return "valid";
        }
    }

    public static String[] obtainClasses(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, null);
        String response = DecompilationController.submitRequest(manager, request);
        if ("ok".equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClassNames();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static ClassInfo[] obtainClassesDetails(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction.RequestAction requestType = AgentRequestAction.RequestAction.CLASSES_WITH_INFO;
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, requestType, null);
        String response = DecompilationController.submitRequest(manager, request);
        if ("ok".equals(response)) {
            return vmInfo.getVmDecompilerStatus().getLoadedClasses();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    public static VmDecompilerStatus obtainClass(VmInfo vmInfo, String clazz, VmManager manager) {
        AgentRequestAction request = DecompilationController.createRequest(vmInfo, AgentRequestAction.RequestAction.BYTES, clazz);
        String response = DecompilationController.submitRequest(manager, request);

        if ("ok".equals(response)) {
            return vmInfo.getVmDecompilerStatus();
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
    }

    private VmInfo.Type guessType(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Unable to interpret PUC because it is empty.");
        }

        try {
            Integer.valueOf(input);
            Logger.getLogger().log("Interpreting '" + input + "' as PID. To use numbers as filenames, try './" + input + "'.");
            return VmInfo.Type.LOCAL;
        } catch (NumberFormatException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as PID failed because it is not a number.");
        }

        try {
            if (input.split(":").length == 2) {
                Integer.valueOf(input.split(":")[1]);
                Logger.getLogger().log("Interpreting '" + input + "' as hostname:port. To use colons as filenames, try './" + input + "'.");
                return VmInfo.Type.REMOTE;
            } else {
                Logger.getLogger()
                        .log("Interpretation of '" + input + "' as hostname:port failed because it cannot be correctly split on ':'.");
            }
        } catch (NumberFormatException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as hostname:port failed because port is not a number.");
        }

        try {
            NewFsVmController.cpToFiles(input);
            Logger.getLogger().log("Interpreting " + input + " as FS VM classpath.");
            return VmInfo.Type.FS;
        } catch (NewFsVmController.InvalidClasspathException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as FS VM classpath. failed. Cause: " + e.getMessage());
        }

        throw new RuntimeException("Unable to interpret '" + input + "' as any component of PUC.");
    }

    VmInfo getVmInfo(String param) {
        VmInfo.Type puc = guessType(param);
        switch (puc) {
            case LOCAL:
                return vmManager.findVmFromPid(param);
            case FS:
                return vmManager.createFsVM(NewFsVmController.cpToFilesCaught(param), null, false);
            case REMOTE:
                String[] hostnamePort = param.split(":");
                return vmManager.createRemoteVM(hostnamePort[0], Integer.parseInt(hostnamePort[1]));
            default:
                throw new RuntimeException("Unknown VmInfo.Type.");
        }
    }
}
