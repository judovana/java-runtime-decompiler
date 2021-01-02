package org.jrd.backend.data;

import org.jc.api.ClassIdentifier;
import org.jc.api.ClassesProvider;
import org.jc.api.IdentifiedBytecode;
import org.jc.api.IdentifiedSource;
import org.jc.api.InMemoryCompiler;
import org.jc.api.MessagesListener;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrame.FiletoClassValidator;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class Cli {

    public static final String VERBOSE = "-verbose";
    private static final String LISTJVMS = "-listjvms";
    private static final String LISTPLUGINS = "-listplugins";
    private static final String LISTCLASSES = "-listclasses";
    private static final String BASE64 = "-base64bytes";
    private static final String BYTES = "-bytes";
    private static final String DECOMPILE = "-decompile";
    private static final String COMPILE = "-compile";
    private static final String OVERWRITE = "-overwrite";
    private static final String HELP = "-help";
    private static final String H = "-h";

    private final String[] allargs;
    private final VmManager vmManager;
    private final PluginManager pluginManager;

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
        for (String arg : allargs) {
            String aarg = cleanParameter(arg);
            if (!aarg.equals(VERBOSE)) {
                args.add(arg);
            }
        }
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
        if (args.size() != 4) {
            throw new RuntimeException(OVERWRITE + "  three args - pid or url of JVM and class to overwrite and file with new bytecode");
        }
        String jvmStr = args.get(i + 1);
        String classStr = args.get(i + 2);
        String newBytecodeFile = args.get(i + 3);
        try {
            FiletoClassValidator.StringAndScore r = FiletoClassValidator.validate(classStr, newBytecodeFile);
            if (r.score > 0 && r.score < 10) {
                System.err.println("WARNING:" + r.message);
            }
            if (r.score >= 10) {
                System.err.println("ERROR:" + r.message);
            }
            VmInfo vmInfo = vmManager.findVmFromPID(jvmStr);
            AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo,
                    AgentRequestAction.RequestAction.OVERWRITE,
                    classStr,
                    VmDecompilerInformationController.fileToBase64(newBytecodeFile));
            String response = VmDecompilerInformationController.submitRequest(vmManager, request);
            if (response.equals("ok")) {
                System.out.println("Most likely done successfully.");
            } else {
                throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

            }
        } catch (NumberFormatException e) {
            try {
                URL u = new URL(jvmStr);
                throw new RuntimeException("Remote VM not yet implemented");
            } catch (MalformedURLException ee) {
                throw new RuntimeException("Second param was supposed to be URL or PID", ee);
            }
        }
    }

    private void compile(List<String> args, int i) throws Exception {
        if (args.size() < 2) {
            throw new RuntimeException(COMPILE + " expects at least oen file to compile");
        }
        String cpPidUrl = null;
        String customCompiler = null;
        boolean haveCompiler = false;
        List<File> toCompile = new ArrayList<>(1);
        for (int x = i + 1; x < args.size(); x++) {
            String arg = args.get(x);
            if (arg.equals("-p")) {
                customCompiler = args.get(x + 1);
                x++;
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
            throw new RuntimeException("Not yet implemented: cp = new RuntimeCompilerConnector.JRDClassesProvider(vmInfo, vmManager);");
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
        InMemoryCompiler rc;
        if (haveCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(pluginManager, decompiler);
        } else {
            rc = new RuntimeCompilerConnector.DummyRuntimeCompiler();
        }
        IdentifiedSource[] isis = new IdentifiedSource[toCompile.size()];
        for (int x = 0; x < isis.length; x++) {
            byte[] bytes = Files.readAllBytes(toCompile.get(x).toPath());
            String name = guessName(bytes);
            isis[x] = new IdentifiedSource(new ClassIdentifier(name), bytes, Optional.empty());
        }
        Collection<IdentifiedBytecode> result = rc.compileClass(cp, Optional.of(new MessagesListener() {
            @Override
            public void addMessage(Level level, String message) {
                System.err.println(message);
            }
        }), isis);
        for (IdentifiedBytecode ib : result) {
            System.out.write(ib.getFile());
        }
    }

    private String guessName(byte[] bytes) throws IOException {
        String pkg = null;
        String clazz = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            while(true) {
                if (clazz != null && pkg != null) {
                    return pkg + "." + clazz; //this return should be most likely everywhere inline
                }
                String s = br.readLine();
                if (s == null) {
                    if (pkg == null && clazz == null) {
                        return "pkg.and.class.not.found";
                    } else if (pkg == null) {
                        return "pkg.not.found.for." + clazz;
                    } else if (clazz == null) {
                        return pkg + ".lost.its.class";
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
                        if (key.equals("package")) {
                            pkg = keys[i + 1].replace("/", "."); //jasm's disasm uses / instead of .
                        }
                        if (key.equals("class")) {
                            clazz = keys[i + 1];
                        }
                    }
                }
            }
        }
    }

    private void decompile(List<String> args, int i) throws Exception {
        if (args.size() != 4) {
            throw new RuntimeException(DECOMPILE + " expects exactly three arguments - pid or url of JVM, fully classified class name and decompiler name (as set-up) or decompiler json file, or javap(see help)");
        }
        String jvmStr = args.get(i + 1);
        String classStr = args.get(i + 2);
        String decompilerName = args.get(i + 3);
        try {
            VmInfo vmInfo = vmManager.findVmFromPID(jvmStr);
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
                String decompile_output = pluginManager.decompile(findDecompiler(DecompilerWrapperInformation.JAVAP_NAME, pluginManager), bytes, options);
                System.out.println(decompile_output);
            } else {
                DecompilerWrapperInformation decompiler = findDecompiler(decompilerName, pluginManager);
                if (decompiler != null) {
                    String decompiledClass = pluginManager.decompile(decompiler, bytes);
                    System.out.println(decompiledClass);
                } else {
                    throw new RuntimeException("Decompiler " + decompilerName + " not found");
                }
            }
        } catch (NumberFormatException e) {
            try {
                URL u = new URL(jvmStr);
                throw new RuntimeException("Remote VM not yet implemented");
            } catch (MalformedURLException ee) {
                throw new RuntimeException("Second param was supposed to be URL or PID", ee);
            }
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

    private void printBytes(List<String> args, int i, boolean bytes) throws IOException {
        if (args.size() != 3) {
            throw new RuntimeException(BYTES + " and " + BASE64 + " expect exactly two arguments - pid or url of JVM and fully classified class name");
        }
        String jvmStr = args.get(i + 1);
        String classStr = args.get(i + 2);
        try {
            VmInfo vmInfo = vmManager.findVmFromPID(jvmStr);
            VmDecompilerStatus result = obtainClass(vmInfo, classStr, vmManager);
            if (bytes) {
                byte[] ba = Base64.getDecoder().decode(result.getLoadedClassBytes());
                System.out.write(ba);
            } else {
                System.out.println(result.getLoadedClassBytes());
            }
        } catch (NumberFormatException e) {
            try {
                URL u = new URL(jvmStr);
                throw new RuntimeException("Remote VM not yet implemented");
            } catch (MalformedURLException ee) {
                throw new RuntimeException("Second param was supposed to be URL or PID", ee);
            }
        }
    }

    private void listClasses(List<String> args, int i) {
        if (args.size() != 2) {
            throw new RuntimeException(LISTCLASSES + " expect exactly one argument - pid or url");
        }
        String param = args.get(i + 1);
        try {
            VmInfo vmInfo = vmManager.findVmFromPID(param);
            String[] classes = obtainClasses(vmInfo, vmManager);
            for (String clazz : classes) {
                System.out.println(clazz);
            }
        } catch (NumberFormatException e) {
            try {
                URL u = new URL(param);
                throw new RuntimeException("Remote VM not yet implemented");
            } catch (MalformedURLException ee) {
                throw new RuntimeException("Second param was supposed to be URL or PID", ee);
            }
        }
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

    private void printHelp() {
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + COMPILE + ", " + VERBOSE + ", " + OVERWRITE);
        System.out.println(VERBOSE + " will set this app to print out all Exceptions and some debugging strings. Then continues ");
        System.out.println(HELP + "/" + H + " print this help end exit");
        System.out.println(LISTJVMS + " no arg expected, list available localhost JVMs ");
        System.out.println(LISTPLUGINS + "  no arg expected, currently configured plugins with theirs status");
        System.out.println(LISTCLASSES + "  one arg - pid or url of JVM. List its loaded classes.");
        System.out.println(BYTES + "  two args - pid or url of JVM and class to obtain - will stdout its binary form");
        System.out.println(BASE64 + "  two args - pid or url of JVM and class to obtain - will stdout its binary encoded as base64");
        System.out.println(DECOMPILE + "  three args - pid or url of JVM and class to obtain and name/file of decompiler config - will stdout decompiled class");
        System.out.println("              you can use special keyword javap, instead of decompiler name, to use javap disassembler.");
        System.out.println("              You can pass also parameters to it like any other javap, but without space. So e.g. javap-v is equal to call javap -v /tmp/class_you_entered.class");
        System.out.println(COMPILE + "  compile local file(s) against runtime classapth. Plugin can have its own compiler, eg jasm do nto require runtime classpath");
        System.out.println("              mandatory: file(s) to compile");
        System.out.println(" wip!         optional: pid or url of runtime classpath, plugin, recursive, directory to save to (if missing then stdout, failing if more then one file is result)");
        System.out.println(" wip!                 -cp                              -p <plugin> -r     -d <dir>");
        System.out.println(OVERWRITE + "  three args - pid or url of JVM and class to overwrite and file with new bytecode");
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + COMPILE + ", " + VERBOSE + ", " + OVERWRITE);
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
}
