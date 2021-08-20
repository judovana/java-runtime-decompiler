package org.jrd.backend.data;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.FileToClassValidator;
import org.jrd.frontend.frame.main.VmDecompilerInformationController;
import org.jrd.frontend.frame.filesystem.NewFsVmController;
import org.jrd.frontend.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.jrd.backend.data.Help.LIST_CLASSES_FORMAT;
import static org.jrd.backend.data.Help.BYTES_FORMAT;
import static org.jrd.backend.data.Help.BASE64_FORMAT;
import static org.jrd.backend.data.Help.DECOMPILE_FORMAT;
import static org.jrd.backend.data.Help.OVERWRITE_FORMAT;
import static org.jrd.backend.data.Help.printHelpText;

public class Cli {

    protected static final String VERBOSE = "-verbose";
    protected static final String SAVE_AS = "-saveas";
    protected static final String SAVE_LIKE = "-savelike";
    protected static final String LIST_JVMS = "-listjvms";
    protected static final String LIST_PLUGINS = "-listplugins";
    protected static final String LIST_CLASSES = "-listclasses";
    protected static final String BASE64 = "-base64bytes";
    protected static final String BYTES = "-bytes";
    protected static final String DECOMPILE = "-decompile";
    protected static final String COMPILE = "-compile";
    protected static final String OVERWRITE = "-overwrite";
    protected static final String VERSION = "-version";
    protected static final String HELP = "-help";
    protected static final String H = "-h";

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private Saving saving;
    private boolean isVerbose;

    protected static class Saving implements Utils.StatusKeeper {
        protected static final String DEFAULT = "default";
        protected static final String EXACT = "exact";
        protected static final String FQN = "fqn";
        protected static final String DIR = "dir";
        private final String as;
        private final String like;

        public Saving(String as, String like) {
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
            OutputController.getLogger().log(ex);
        }

        public int toInt(String suffix) {
            switch (like) {
                case FQN:
                    return Utils.FULLY_QUALIFIED_NAME;
                case EXACT:
                    return Utils.CUSTOM_NAME;
                case DIR:
                    return Utils.SRC_SUBDIRS_NAME;
                case DEFAULT:
                    if (".java".equals(suffix)) {
                        return Utils.FULLY_QUALIFIED_NAME;
                    }
                    if (".class".equals(suffix)) {
                        return Utils.SRC_SUBDIRS_NAME;
                    }
                    return Utils.CUSTOM_NAME;
                default:
                    throw new RuntimeException("Unknown saving type: " + like + ". Allowed are: " + FQN + "," + DIR + "," + EXACT);
            }
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
                listJVMs();
                break;
            case LIST_PLUGINS:
                listPlugins();
                break;
            case LIST_CLASSES:
                listClasses();
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
                compile();
                break;
            case OVERWRITE:
                overwrite();
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
            OutputController.getLogger().log("Reading class for overwrite from stdin.");
            newBytecodeFile = null;
        } else {
            if (filteredArgs.size() != 4) {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + OVERWRITE_FORMAT + "'.");
            }
            newBytecodeFile = filteredArgs.get(3);
        }
        String jvmStr = filteredArgs.get(1);
        String classStr = filteredArgs.get(2);
        if (newBytecodeFile != null) {
            FileToClassValidator.StringAndScore r = FileToClassValidator.validate(classStr, newBytecodeFile);
            if (r.score > 0 && r.score < 10) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "WARNING: " + r.message);
            }
            if (r.score >= 10) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "ERROR: " + r.message);
            }
        }
        VmInfo vmInfo = getVmInfo(jvmStr);
        String clazz;
        if (newBytecodeFile == null) {
            clazz = VmDecompilerInformationController.stdinToBase64();
        } else {
            clazz = VmDecompilerInformationController.fileToBase64(newBytecodeFile);
        }
        AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo,
                AgentRequestAction.RequestAction.OVERWRITE,
                classStr,
                clazz);
        String response = VmDecompilerInformationController.submitRequest(vmManager, request);
        if (response.equals("ok")) {
            System.out.println("Most likely done successfully.");
        } else {
            throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

        }
    }

    private void compile() throws Exception {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Expected at least one file for compile.");
        }
        String cpPidUrl = null;
        String customCompiler = null;
        boolean recursive = false;
        List<File> toCompile = new ArrayList<>(1);
        for (int x = 1; x < filteredArgs.size(); x++) {
            String arg = filteredArgs.get(x);
            if (arg.equals("-p")) {
                customCompiler = filteredArgs.get(x + 1);
                x++;
            } else if (arg.equals("-cp")) {
                cpPidUrl = filteredArgs.get(x + 1);
                x++;
            } else if (arg.equals("-r")) {
                recursive = true;
            } else {
                toCompile.add(new File(arg));
                if (!toCompile.get(toCompile.size() - 1).exists()) {
                    throw new RuntimeException(toCompile.get(toCompile.size() - 1).getAbsolutePath() + " does not exists");
                }
            }
        }
        ClassesProvider cp;
        if (cpPidUrl == null) {
            cp = new ClassesProvider() {
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
            VmInfo vmInfo = getVmInfo(cpPidUrl);
            cp = new RuntimeCompilerConnector.JRDClassesProvider(vmInfo, vmManager);
        }
        DecompilerWrapperInformation decompiler = null;
        boolean haveCompiler = false;
        String compilerLogMessage = "Default runtime compiler will be used for overwrite.";

        if (customCompiler != null) {
            decompiler = findDecompiler(customCompiler, pluginManager);

            if (pluginManager.haveCompiler(decompiler)) {
                compilerLogMessage = customCompiler + "'s bundled compiler will be used for overwrite.";
                haveCompiler = true;
            }
        }
        OutputController.getLogger().log(compilerLogMessage);

        ClasspathlessCompiler rc;
        if (haveCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(decompiler);
        } else {
            rc = new io.github.mkoncek.classpathless.impl.CompilerJavac();
        }
        IdentifiedSource[] isis = Utils.sourcesToIdentifiedSources(recursive, toCompile);
        Collection<IdentifiedBytecode> result = rc.compileClass(
                cp,
                Optional.of((level, message) -> OutputController.getLogger().log(message)),
                isis
        );

        boolean upload = false;
        if (saving.shouldSave()) {
            try {
                VmInfo.Type t = guessType(saving.as);
                if (t == VmInfo.Type.LOCAL || t == VmInfo.Type.REMOTE) {
                    upload = true;
                }
            } catch (Exception ex) {
                OutputController.getLogger().log(ex);
            }
        }
        if (upload) {
            VmInfo target = getVmInfo(saving.as);
            int f = 0;
            for (IdentifiedBytecode ib : result) {
                String className = ib.getClassIdentifier().getFullName();
                OutputController.getLogger().log("Uploading class '" + className + "'.");

                AgentRequestAction request = VmDecompilerInformationController.createRequest(target,
                        AgentRequestAction.RequestAction.OVERWRITE,
                        ib.getClassIdentifier().getFullName(),
                        Base64.getEncoder().encodeToString(ib.getFile()));
                String response = VmDecompilerInformationController.submitRequest(vmManager, request);
                if (response.equals("ok")) {
                    OutputController.getLogger().log("Successfully uploaded class '" + className + "'.");
                } else {
                    f++;
                    OutputController.getLogger().log("Failed to upload class '" + className + "'.");
                }
            }
            if (f > 0) {
                throw new RuntimeException("Failed to upload " + f + " classes out of " + result.size() + " total.");
            } else {
                OutputController.getLogger().log("Successfully uploaded all " + result.size() + " classes.");
            }
        } else {
            if (!saving.shouldSave() && result.size() > 1) {
                throw new IllegalArgumentException("Unable to print multiple classes to stdout. Either use saving modifiers or compile one class at a time.");
            }
            for (IdentifiedBytecode ib : result) {
                outOrSave(ib.getClassIdentifier().getFullName(), ".class", ib.getFile(), true);
            }
        }
    }

    public static String guessName(byte[] bytes) throws IOException {
        String pkg = null;
        String clazz = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            while (true) {
                if (clazz != null && pkg != null) {
                    return pkg + "." + clazz; //this return should be most likely everywhere inline
                }
                String s = br.readLine();
                if (s == null) {
                    if (pkg == null && clazz == null) {
                        throw new RuntimeException("Neither package nor class was found.");
                    } else if (pkg == null) {
                        throw new RuntimeException("Package not found for class '" + clazz + "'.");
                    } else if (clazz == null) {
                        throw new RuntimeException("Class not found for package '" + pkg + "'.");
                    } else {
                        return pkg + "." + clazz;
                    }
                }
                s = s.trim();
                String[] ss = s.split(";");
                for (String sss : ss) {
                    String[] keys = sss.split("\\s+");
                    for (int i = 0; i < keys.length; i++) {
                        String key = keys[i];
                        if (key.equals("0xCAFEBABE")) {
                            return clazz.replace("/", "."); //jcoder's disassembler uses / instead of and has fully qualified class name as class name
                        }
                        if (key.equals("package")) {
                            pkg = keys[i + 1].replace("/", "."); //jasm's disassembler uses / instead of .
                        }
                        if (key.equals("class") || key.equals("interface") || key.equals("enum")) {
                            clazz = keys[i + 1];
                        }
                    }
                }
            }
        }
    }

    private void decompile() throws Exception {
        if (filteredArgs.size() < 4) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + DECOMPILE_FORMAT + "'.");
        }
        String jvmStr = filteredArgs.get(1);
        String decompilerName = filteredArgs.get(2);
        VmInfo vmInfo = getVmInfo(jvmStr);
        int failures = 0;
        int total = 0;
        for (int i = 3; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)));
            for (String classStr : classes) {
                total++;
                VmDecompilerStatus result = obtainClass(vmInfo, classStr, vmManager);
                byte[] bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());
                if (new File(decompilerName).exists() && decompilerName.toLowerCase().endsWith(".json")) {
                    throw new RuntimeException("Plugin loading directly from file is not implemented.");
                }
                if (decompilerName.startsWith(DecompilerWrapperInformation.JAVAP_NAME)) {
                    String[] split_name = decompilerName.split("-");
                    String[] options = new String[split_name.length - 1];
                    for (int x = 1; x < split_name.length; x++) {
                        options[x - 1] = "-" + split_name[x];
                    }
                    String decompile_output = pluginManager.decompile(findDecompiler(DecompilerWrapperInformation.JAVAP_NAME, pluginManager), classStr, bytes, options, vmInfo, vmManager);
                    if (!outOrSave(classStr, ".java", decompile_output)) {
                        failures++;
                    }
                } else {
                    DecompilerWrapperInformation decompiler = findDecompiler(decompilerName, pluginManager);
                    if (decompiler != null) {
                        String decompiledClass = pluginManager.decompile(decompiler, classStr, bytes, null, vmInfo, vmManager);
                        if (!outOrSave(classStr, ".java", decompiledClass)) {
                            failures++;
                        }
                    } else {
                        throw new RuntimeException("Decompiler '" + decompilerName + "' not found");
                    }
                }
            }
        }
        returnNonzero(failures, total);
    }

    private void returnNonzero(int failures, int total) {
        if (total == 0) {
            throw new RuntimeException("No class found to save.");
        }
        if (failures > 0) {
            throw new RuntimeException("Failed to save " + failures + "classes.");
        }
    }

    private boolean outOrSave(String name, String suffix, String s) throws IOException {
        return outOrSave(name, suffix, s.getBytes(StandardCharsets.UTF_8), false);
    }

    private boolean outOrSave(String name, String suffix, byte[] body, boolean forceBin) throws IOException {
        if (saving.shouldSave()) {
            return Utils.saveByGui(saving.as, saving.toInt(suffix), suffix, saving, name, body);
        } else {
            if (forceBin) {
                System.out.write(body);
            } else {
                System.out.println(new String(body, StandardCharsets.UTF_8));
            }
            return true;
        }
    }

    private DecompilerWrapperInformation findDecompiler(String decompilerName, PluginManager pluginManager) {
        List<DecompilerWrapperInformation> wrappers = pluginManager.getWrappers();
        DecompilerWrapperInformation decompiler = null;
        for (DecompilerWrapperInformation dw : wrappers) {
            if (!dw.getScope().equals(DecompilerWrapperInformation.LOCAL_SCOPE) && dw.getName().equals(decompilerName)) {
                decompiler = dw;
            }
        }
        //LOCAL is preferred one
        for (DecompilerWrapperInformation dw : wrappers) {
            if (dw.getScope().equals(DecompilerWrapperInformation.LOCAL_SCOPE) && dw.getName().equals(decompilerName)) {
                decompiler = dw;
            }
        }
        return decompiler;
    }

    private void printBytes(boolean bytes) throws Exception {
        if (filteredArgs.size() < 3) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + (bytes ? BYTES_FORMAT : BASE64_FORMAT) + "'.");
        }
        String jvmStr = filteredArgs.get(1);
        VmInfo vmInfo = getVmInfo(jvmStr);
        int failures = 0;
        int total = 0;
        for (int i = 2; i < filteredArgs.size(); i++) {
            String clazzRegex = filteredArgs.get(i);
            List<String> classes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)));
            for (String classStr : classes) {
                total++;
                VmDecompilerStatus result = obtainClass(vmInfo, classStr, vmManager);
                if (bytes) {
                    byte[] ba = Base64.getDecoder().decode(result.getLoadedClassBytes());
                    if (!outOrSave(classStr, ".class", ba, true)) {
                        failures++;
                    }
                } else {
                    if (!outOrSave(classStr, ".class", result.getLoadedClassBytes())) {
                        failures++;
                    }
                }
            }
        }
        returnNonzero(failures, total);
    }

    private void listClasses() throws IOException {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + LIST_CLASSES_FORMAT + "'.");
        }
        String param = filteredArgs.get(1);
        List<Pattern> filter = new ArrayList<>(filteredArgs.size() - 1);
        for (int i = 2; i < filteredArgs.size(); i++) {
            filter.add(Pattern.compile(filteredArgs.get(i)));
        }
        if (filter.isEmpty()) {
            filter.add(Pattern.compile(".*"));
        }
        VmInfo vmInfo = getVmInfo(param);
        listClassesFromVmInfo(vmInfo, filter);
    }

    private static List<String> obtainFilteredClasses(VmInfo vmInfo, VmManager vmManager, List<Pattern> filter) throws IOException {
        String[] allClasses = obtainClasses(vmInfo, vmManager);
        List<String> filteredClasses = new ArrayList<>(allClasses.length);
        for (String clazz : allClasses) {
            if (matchesAtLeastOne(clazz, filter)) {
                filteredClasses.add(clazz);
            }
        }
        return filteredClasses;
    }

    private void listClassesFromVmInfo(VmInfo vmInfo, List<Pattern> filter) throws IOException {
        List<String> classes = obtainFilteredClasses(vmInfo, vmManager, filter);
        if (saving.shouldSave()) {
            if (saving.like.equals(Saving.DEFAULT) || saving.like.equals(Saving.EXACT)) {
                try (BufferedWriter bw = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(saving.as), StandardCharsets.UTF_8)))) {
                    for (String clazz : classes) {
                        bw.write(clazz);
                        bw.newLine();
                    }
                }
            } else {
                throw new RuntimeException("Only '" + Saving.DEFAULT + "' and '" + Saving.EXACT + "' are allowed for class listing saving.");
            }
        } else {
            for (String clazz : classes) {
                System.out.println(clazz);
            }
        }
    }

    private static boolean matchesAtLeastOne(String clazz, List<Pattern> filter) {
        for (Pattern p : filter) {
            if (p.matcher(clazz).matches()) {
                return true;
            }
        }
        return false;
    }

    private void listPlugins() {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_PLUGINS + " does not expect arguments.");
        }
        PluginManager pm = new PluginManager();
        List<DecompilerWrapperInformation> wrappers = pm.getWrappers();
        for (DecompilerWrapperInformation dw : wrappers) {
            System.out.println(dw.getName() + " " + dw.getScope() + "/" + invalidityToString(dw.isInvalidWrapper()) + " - " + dw.getFileLocation());
        }
    }

    private void listJVMs() {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_JVMS + " does not expect arguments.");
        }
        for (VmInfo vmInfo : vmManager.getVmInfoSet()) {
            System.out.println(vmInfo.getVmPid() + " " + vmInfo.getVmName());
        }
    }

    private void printVersion() {
        System.out.println(MetadataProperties.getInstance());
    }

    private void printHelp() {
        printHelpText();
    }

    private static String invalidityToString(boolean invalidWrapper) {
        if (invalidWrapper) {
            return "invalid";
        } else {
            return "valid";
        }
    }

    public static String[] obtainClasses(VmInfo vmInfo, VmManager manager) {
        AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo, AgentRequestAction.RequestAction.CLASSES, null);
        String response = VmDecompilerInformationController.submitRequest(manager, request);
        if (response.equals("ok")) {
            String[] classes = vmInfo.getVmDecompilerStatus().getLoadedClassNames();
            return classes;
        } else {
            throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);
        }
    }

    public static VmDecompilerStatus obtainClass(VmInfo vmInfo, String clazz, VmManager manager) {
        AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo, AgentRequestAction.RequestAction.BYTES, clazz);
        String response = VmDecompilerInformationController.submitRequest(manager, request);
        if (response.equals("ok")) {
            return vmInfo.getVmDecompilerStatus();
        } else {
            throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

        }
    }

    private VmInfo.Type guessType(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Unable to interpret PUC because it is empty.");
        }
        try {
            Integer.valueOf(input);
            OutputController.getLogger().log("Interpreting '" + input + "' as PID. To use numbers as filenames, try './" + input + "'.");

            return VmInfo.Type.LOCAL;
        } catch (NumberFormatException e) {
            OutputController.getLogger().log("Interpretation of '" + input + "' as PID failed because it is not a number.");
        }

        try {
            if (input.split(":").length == 2) {
                Integer.valueOf(input.split(":")[1]);
                OutputController.getLogger().log("Interpreting '" + input + "' as hostname:port. To use colons as filenames, try './" + input + "'.");

                return VmInfo.Type.REMOTE;
            } else {
                OutputController.getLogger().log("Interpretation of '" + input + "' as hostname:port failed because it cannot be correctly split on ':'.");
            }
        } catch (NumberFormatException e) {
            OutputController.getLogger().log("Interpretation of '" + input + "' as hostname:port failed because port is not a number.");
        }

        try {
            NewFsVmController.cpToFiles(input);
            OutputController.getLogger().log("Interpreting " + input + " as FS VM classpath.");

            return VmInfo.Type.FS;
        } catch (NewFsVmController.ProbablyNotClassPathElementException e) {
            OutputController.getLogger().log("Interpretation of '" + input + "' as FS VM classpath. failed. Cause: " + e.getMessage());
        }

        throw new RuntimeException("Unable to interpret '" + input + "' as any component of PUC.");
    }

    VmInfo getVmInfo(String param) {
        VmInfo.Type puc = guessType(param);
        VmInfo vmInfo;
        switch (puc) {
            case LOCAL:
                vmInfo = vmManager.findVmFromPID(param);
                break;
            case FS:
                vmInfo = vmManager.createFsVM(NewFsVmController.cpToFilesCaught(param), null);
                break;
            case REMOTE:
                vmInfo = vmManager.createRemoteVM(param.split(":")[0], Integer.parseInt(param.split(":")[1]));
                break;
            default:
                throw new RuntimeException("Unknown VmInfo.Type.");
        }
        return vmInfo;
    }
}
