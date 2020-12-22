package org.jrd.backend.data;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.jrd.frontend.MainFrame.FiletoClassValidator;

public class Cli {

    public static final String VERBOSE = "-verbose";
    private static final String LISTJVMS = "-listjvms";
    private static final String LISTPLUGINS = "-listplugins";
    private static final String LISTCLASSES = "-listclasses";
    private static final String BASE64 = "-base64bytes";
    private static final String BYTES = "-bytes";
    private static final String DECOMPILE = "-decompile";
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
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + VERBOSE + ", " + OVERWRITE);
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
        System.out.println(OVERWRITE + "  three args - pid or url of JVM and class to overwrite and file with new bytecode");
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + VERBOSE + ", " + OVERWRITE);
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
