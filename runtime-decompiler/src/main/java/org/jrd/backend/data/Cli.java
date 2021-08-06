package org.jrd.backend.data;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrame.FiletoClassValidator;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;
import org.jrd.frontend.NewFsVmFrame.NewFsVmController;
import org.jrd.frontend.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static org.jrd.backend.data.Help.printHelpText;

public class Cli {

    protected static final String VERBOSE = "-verbose";
    protected static final String SAVEAS = "-saveas";
    protected static final String SAVELIKE = "-savelike";
    protected static final String LISTJVMS = "-listjvms";
    protected static final String LISTPLUGINS = "-listplugins";
    protected static final String LISTCLASSES = "-listclasses";
    protected static final String BASE64 = "-base64bytes";
    protected static final String BYTES = "-bytes";
    protected static final String DECOMPILE = "-decompile";
    protected static final String COMPILE = "-compile";
    protected static final String OVERWRITE = "-overwrite";
    protected static final String VERSION = "-version";
    protected static final String HELP = "-help";
    protected static final String H = "-h";

    private final String[] allargs;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private Saving saving;

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

        public Saving(String as) {
            this.as = as;
            this.like = "default";
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
                    throw new RuntimeException("Unknown savng typr: " + like + ". Allowed are: " + FQN + "," + DIR + "," + EXACT);
            }
        }
    }

    public Cli(String[] orig, Model model) {
        this.allargs = orig;
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();
        for (String arg : allargs) {
            String aarg = cleanParameter(arg);
            if (aarg.equals(HELP) || aarg.equals(H)) {
                printHelp();
                System.exit(0);
            }
            if (aarg.equals(VERSION)) {
                try {
                    printVersion();
                    System.exit(0);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private static String cleanParameter(String param) {
        return param.replaceAll("^--*", "-").toLowerCase();
    }

    public boolean shouldBeVerbose() {
        for (String arg : allargs) {
            String aarg = cleanParameter(arg);
            if (aarg.equals(VERBOSE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGui() {
        return prefilterArgs().isEmpty();
    }

    private List<String> prefilterArgs() {
        List<String> args = new ArrayList(allargs.length);
        String saveas = null;
        String savelike = null;
        for (int i = 0; i < allargs.length; i++) {
            String arg = allargs[i];
            String aarg = cleanParameter(arg);
            if (aarg.equals(VERBOSE)) {
                //alread processed
            } else if (aarg.equals(SAVEAS)) {
                saveas = allargs[i + 1];
                i++;
            } else if (aarg.equals(SAVELIKE)) {
                savelike = allargs[i + 1];
                i++;
            } else {
                args.add(arg);
            }
        }
        this.saving = new Saving(saveas, savelike);
        return args;
    }

    public void consumeCli() throws Exception {
        List<String> args = prefilterArgs();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            arg = cleanParameter(arg);
            if (arg.equals(LISTJVMS)) {
                listJvms(args);
                break;
            }
            if (arg.equals(LISTPLUGINS)) {
                listPlugins(args);
                break;
            } else if (arg.equals(LISTCLASSES)) {
                listClasses(args, i);
                break;
            } else if (arg.equals(BYTES) || arg.equals(BASE64)) {
                boolean bytes = arg.equals(BYTES);
                printBytes(args, i, bytes);
                break;
            } else if (arg.equals(DECOMPILE)) {
                decompile(args, i);
                break;
            } else if (arg.equals(COMPILE)) {
                compile(args, i);
                break;
            } else if (arg.equals(OVERWRITE)) {
                overwrite(args, i);
                break;
            } else {
                printHelp();
                throw new RuntimeException("Unknown commandline switch " + arg);
            }
        }
    }

    private void overwrite(List<String> args, int i) throws Exception {
        String newBytecodeFile;
        if (args.size() == 3) {
            System.err.println("reading class from std.in");
            newBytecodeFile = null;
        } else {
            if (args.size() != 4) {
                throw new RuntimeException(OVERWRITE + "  three args - PUC of JVM and class to overwrite and file with new bytecode");
            }
            newBytecodeFile = args.get(i + 3);
        }
        String jvmStr = args.get(i + 1);
        String classStr = args.get(i + 2);
        if (newBytecodeFile != null) {
            FiletoClassValidator.StringAndScore r = FiletoClassValidator.validate(classStr, newBytecodeFile);
            if (r.score > 0 && r.score < 10) {
                System.err.println("WARNING:" + r.message);
            }
            if (r.score >= 10) {
                System.err.println("ERROR:" + r.message);
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

    private void compile(List<String> args, int i) throws Exception {
        if (args.size() < 2) {
            throw new RuntimeException(COMPILE + " expects at least one file to compile");
        }
        String cpPidUrl = null;
        String customCompiler = null;
        boolean haveCompiler = false;
        boolean recursive = false;
        List<File> toCompile = new ArrayList<>(1);
        for (int x = i + 1; x < args.size(); x++) {
            String arg = args.get(x);
            if (arg.equals("-p")) {
                customCompiler = args.get(x + 1);
                x++;
            } else if (arg.equals("-cp")) {
                cpPidUrl = args.get(x + 1);
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
        String s = "Default runtime compiler will be used";
        if (customCompiler == null) {
            haveCompiler = false;
        } else {
            decompiler = findDecompiler(customCompiler, pluginManager);
            haveCompiler = false;
            boolean pluginsDecompiler = this.pluginManager.haveCompiler(decompiler);
            if (pluginsDecompiler) {
                s = customCompiler + " plugin is delivered with its own compiler!!";
                haveCompiler = true;
            }
        }
        System.err.println(s);
        ClasspathlessCompiler rc;
        if (haveCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(pluginManager, decompiler);
        } else {
            rc = new io.github.mkoncek.classpathless.impl.CompilerJavac();
        }
        IdentifiedSource[] isis = Utils.sourcesToIdentifiedSources(recursive, toCompile);
        Collection<IdentifiedBytecode> result = rc.compileClass(cp, Optional.of(new MessagesListener() {
            @Override
            public void addMessage(Level level, String message) {
                System.err.println(message);
            }
        }), isis);
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
                System.out.println("Uploading " + ib.getClassIdentifier().getFullName());
                AgentRequestAction request = VmDecompilerInformationController.createRequest(target,
                        AgentRequestAction.RequestAction.OVERWRITE,
                        ib.getClassIdentifier().getFullName(),
                        Base64.getEncoder().encodeToString(ib.getFile()));
                String response = VmDecompilerInformationController.submitRequest(vmManager, request);
                if (response.equals("ok")) {
                    System.out.println("    Most likely done successfully.");
                } else {
                    f++;
                    System.out.println("    failed");
                }
            }
            if (f > 0) {
                throw new RuntimeException("Not uploaded " + f + " classes of total " + result.size());
            } else {
                System.out.println("Done all " + result.size() + " classes");
            }
        } else {
            if (!saving.shouldSave() && result.size() > 1) {
                throw new RuntimeException("more then one file is output of compilation, but yuo have stdout enabled. Use " + SAVEAS + " and friends to save the output of compilation");
            }
            for (IdentifiedBytecode ib : result) {
                outOrSave(ib.getClassIdentifier().getFullName(), ".class", ib.getFile(), true);
            }
        }
    }

    public static String guessName(byte[] bytes) throws IOException {
        String pkg = null;
        String clazz = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            while (true) {
                if (clazz != null && pkg != null) {
                    return pkg + "." + clazz; //this return should be most likely everywhere inline
                }
                String s = br.readLine();
                if (s == null) {
                    if (pkg == null && clazz == null) {
                        throw new RuntimeException("pkg.and.class.not.found");
                    } else if (pkg == null) {
                        throw new RuntimeException("pkg.not.found.for." + clazz);
                    } else if (clazz == null) {
                        throw new RuntimeException(pkg + ".lost.its.class");
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
                            return clazz.replace("/", "."); //jcod's disasm uses / instead of and have fully qulified class name as class name
                        }
                        if (key.equals("package")) {
                            pkg = keys[i + 1].replace("/", "."); //jasm's disasm uses / instead of .
                        }
                        if (key.equals("class") || key.equals("interface") || key.equals("enum")) {
                            clazz = keys[i + 1];
                        }
                    }
                }
            }
        }
    }

    private void decompile(List<String> args, int i) throws Exception {
        if (args.size() < 4) {
            throw new RuntimeException(
                    DECOMPILE
                            + " at least three arguments - PUC of JVM,  decompiler name (as set-up) or decompiler json file, or javap(see help) followed by fully classified class name(s)/regex(es)");
        }
        String jvmStr = args.get(i + 1);
        String decompilerName = args.get(i + 2);
        VmInfo vmInfo = getVmInfo(jvmStr);
        int failures = 0;
        int total = 0;
        for (int y = 3; y < args.size(); y++) {
            String clazzRegex = args.get(y);
            List<String> clazzes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)));
            for (String classStr : clazzes) {
                total++;
                VmDecompilerStatus result = obtainClass(vmInfo, classStr, vmManager);
                byte[] bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());
                if (new File(decompilerName).exists() && decompilerName.toLowerCase().endsWith(".json")) {
                    throw new RuntimeException("Plugins loading directly from file is not implemented yet.");
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
                        throw new RuntimeException("Decompiler " + decompilerName + " not found");
                    }
                }
            }
        }
        returnNonzero(failures, total);
    }

    private void returnNonzero(int failures, int total) throws Exception {
        if (total == 0) {
            throw new Exception("No class found!");
        }
        if (failures > 0) {
            throw new Exception(failures + " saving failed");
        }
    }

    private boolean outOrSave(String name, String suffix, String s) throws IOException {
        return outOrSave(name, suffix, s.getBytes(Charset.forName("utf-8")), false);
    }

    private boolean outOrSave(String name, String suffix, byte[] body, boolean forceBin) throws IOException {
        if (saving.shouldSave()) {
            return Utils.saveByGui(saving.as, saving.toInt(suffix), suffix, saving, name, body);
        } else {
            if (forceBin) {
                System.out.write(body);
            } else {
                System.out.println(new String(body, "utf-8"));
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

    private void printBytes(List<String> args, int i, boolean bytes) throws Exception {
        if (args.size() < 3) {
            throw new RuntimeException(BYTES + " and " + BASE64 + " expect at least two arguments - PUC of JVM and fully classified class name(s)/regex(es)");
        }
        String jvmStr = args.get(i + 1);
        VmInfo vmInfo = getVmInfo(jvmStr);
        int failures = 0;
        int total = 0;
        for (int y = 2; y < args.size(); y++) {
            String clazzRegex = args.get(y);
            List<String> clazzes = obtainFilteredClasses(vmInfo, vmManager, Arrays.asList(Pattern.compile(clazzRegex)));
            for (String classStr : clazzes) {
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

    private void listClasses(List<String> args, int i) throws IOException {
        if (args.size() < 2) {
            throw new RuntimeException(LISTCLASSES + " expect at least one argument - PUC. Second, optional is list of filtering regexes");
        }
        String param = args.get(i + 1);
        List<Pattern> filter = new ArrayList<>(args.size() - 1);
        for (int x = 2; x < args.size(); x++) {
            filter.add(Pattern.compile(args.get(x)));
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
                try (BufferedWriter bw = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(new File(saving.as)))))) {
                    for (String clazz : classes) {
                        bw.write(clazz);
                        bw.newLine();
                    }
                }
            } else {
                throw new RuntimeException("Only " + Saving.DEFAULT + " and " + Saving.EXACT + "allowed for saving list of classes");
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

    private void listPlugins(List<String> args) {
        if (args.size() != 1) {
            throw new RuntimeException(LISTPLUGINS + " does not expect argument");
        }
        PluginManager pm = new PluginManager();
        List<DecompilerWrapperInformation> wrappers = pm.getWrappers();
        for (DecompilerWrapperInformation dw : wrappers) {
            System.out.println(dw.getName() + " " + dw.getScope() + "/" + invalidityToString(dw.isInvalidWrapper()) + " - " + dw.getFileLocation());
        }
    }

    private void listJvms(List<String> args) {
        if (args.size() != 1) {
            throw new RuntimeException(LISTJVMS + " does not expect argument");
        }
        for (VmInfo vmInfo : vmManager.getVmInfoSet()) {
            System.out.println(vmInfo.getVmPid() + " " + vmInfo.getVmName());
        }
    }

    protected static Optional<Attributes> getJrdAttributes() throws IOException {
        Enumeration<URL> resources = Cli.class.getClassLoader().getResources("META-INF/MANIFEST.MF");

        while (resources.hasMoreElements()) {
            Manifest manifest = new Manifest(resources.nextElement().openStream());
            Attributes attributes = manifest.getAttributes("runtime-decompiler");

            if (attributes != null) {
                if ("java-runtime-decompiler".equals(attributes.getValue("groupId")) &&
                        "runtime-decompiler".equals(attributes.getValue("artifactId"))) {
                    return Optional.of(attributes);
                }
            }
        }

        return Optional.empty();
    }

    private void printVersion() throws IOException {
        Optional<Attributes> maybeAttributes = getJrdAttributes();

        if (maybeAttributes.isEmpty()) {
            System.out.println("JRD - version unknown");
            return;
        }

        Attributes attributes = maybeAttributes.get();
        System.out.println(
                attributes.getValue("groupId") + " - JRD - " +
                attributes.getValue("version") + " - " +
                attributes.getValue("timestamp")
        );
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
            throw new RuntimeException("No input to check P(pid) xor U(host:port) xor C(classpath) from.");
        }
        try {
            Integer.valueOf(input);
            if (OutputController.getLogger().isVerbose()) {
                System.err.println("Using " + input + " as pid. For files/folders of number try ./number format");
            }
            return VmInfo.Type.LOCAL;
        } catch (NumberFormatException eee) {
            if (OutputController.getLogger().isVerbose()) {
                eee.printStackTrace();
            }
            try {
                if (input.split(":").length == 2) {
                    Integer.valueOf(input.split(":")[1]);
                    if (OutputController.getLogger().isVerbose()) {
                        System.err.println("Using " + input + " as host:port. For files/folders of number try ./number format");
                    }
                    return VmInfo.Type.REMOTE;
                } else {
                    throw new NumberFormatException("it is not host:number format");
                }
            } catch (NumberFormatException ee) {
                if (OutputController.getLogger().isVerbose()) {
                    ee.printStackTrace();
                }
                try {
                    NewFsVmController.cpToFiles(input);
                    if (OutputController.getLogger().isVerbose()) {
                        System.err.println("Using " + input + " as host:port. For files/folders of number try ./number format");
                    }
                    return VmInfo.Type.FS;
                } catch (NewFsVmController.ProbablyNotClassPathElementException e) {
                    if (OutputController.getLogger().isVerbose()) {
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new RuntimeException("Unable to determine " + input + " as Pid xor host:port xor classpath");
    }

    private VmInfo getVmInfo(String param) {
        VmInfo.Type puc = guessType(param);
        VmInfo vmInfo;
        switch (puc) {
            case LOCAL:
                vmInfo = vmManager.findVmFromPID(param);
                break;
            case FS:
                vmInfo = vmManager.createFsVM(NewFsVmController.cpToFilesCatched(param), null);
                break;
            case REMOTE:
                vmInfo = vmManager.createRemoteVM(param.split(":")[0], Integer.valueOf(param.split(":")[1]));
                break;
            default:
                throw new RuntimeException("Unknown VmInfo.Type.");
        }
        return vmInfo;
    }

}
