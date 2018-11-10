package org.jrd.backend.data;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.VmDecompilerInformationController;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Cli {


    public static final String VERBOSE = "-verbose";
    private static final String LISTJVMS = "-listjvms";
    private static final String LISTPLUGINS = "-listplugins";
    private static final String LISTCLASSES = "-listclasses";
    private static final String BASE64 = "-base64bytes";
    private static final String BYTES = "-bytes";
    private static final String DECOMPILE = "-decompile";
    private static final String HELP = "-help";
    private static final String H = "-h";

    private final String[] allargs;
    private final VmManager manager;

    public Cli(String[] orig, VmManager manager) {
        this.allargs = orig;
        this.manager = manager;
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
                if (args.size() != 1) {
                    throw new RuntimeException(LISTJVMS + " do not expect argument");
                }
                for (VmInfo vm : manager.vmList) {
                    System.out.println(vm.getVmPid() + " " + vm.getVmName());
                }
                break;
            }
            if (arg.equals(LISTPLUGINS)) {
                if (args.size() != 1) {
                    throw new RuntimeException(LISTPLUGINS + " do not expect argument");
                }
                PluginManager pm = new PluginManager();
                List<DecompilerWrapperInformation> wrappers = pm.getWrappers();
                for (DecompilerWrapperInformation dw : wrappers) {
                    System.out.println(dw.getName() + " " + dw.getScope() + "/" + invalidityToString(dw.isInvalidWrapper()) + " - " + dw.getFileLocation());
                }
                break;
            } else if (arg.equals(LISTCLASSES)) {
                if (args.size() != 2) {
                    throw new RuntimeException(LISTCLASSES + " expect exactly one argument - pid or url");
                }
                String param = args.get(i + 1);
                try {
                    VmInfo vmInfo = manager.findVmFromPID(param);
                    AgentRequestAction request = VmDecompilerInformationController.createRequest(manager, vmInfo, null, AgentRequestAction.RequestAction.CLASSES);
                    String response = VmDecompilerInformationController.submitRequest(manager, request);
                    if (response.equals("ok")) {
                        VmDecompilerStatus vmStatus = manager.getVmDecompilerStatus(vmInfo);
                        String[] classes = vmStatus.getLoadedClassNames();
                        for (String clazz : classes) {
                            System.out.println(clazz);
                        }
                    }
                    if (response.equals("error")) {
                        throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

                    }
                } catch (NumberFormatException e) {
                    try {
                        URL u = new URL(param);
                        throw new RuntimeException("Remote VM not yet implemented");
                    } catch (MalformedURLException ee) {
                        throw new RuntimeException("Second param was supposed to be URL or PID", ee);
                    }
                }
                break;
            } else if (arg.equals(BYTES) || arg.equals(BASE64)) {
                if (args.size() != 3) {
                    throw new RuntimeException(BYTES + " and " + BASE64 + " expect exactly two argument - pid or url of JVM and fully classified class name");
                }
                String jvmStr = args.get(i + 1);
                String classStr = args.get(i + 2);
                try {
                    VmInfo vmInfo = manager.findVmFromPID(jvmStr);
                    VmDecompilerStatus result = obtainClass(vmInfo, classStr, manager);
                    if (arg.equals(BYTES)) {
                        byte[] ba = Base64.getDecoder().decode(result.getLoadedClassBytes());
                        System.out.write(ba);
                    } else if (arg.equals(BASE64)) {
                        System.out.println(result.getLoadedClassBytes());
                    } else {
                        throw new RuntimeException("Moon had fallen to Earth and Sun burned the rest...");
                    }
                } catch (NumberFormatException e) {
                    try {
                        URL u = new URL(jvmStr);
                        throw new RuntimeException("Remote VM not yet implemented");
                    } catch (MalformedURLException ee) {
                        throw new RuntimeException("Second param was supposed to be URL or PID", ee);
                    }
                }
                break;
            } else if (arg.equals(DECOMPILE)) {
                if (args.size() != 4) {
                    throw new RuntimeException(DECOMPILE + " expect exactly three argument - pid or url of JVM, fully classified class name and decompiler name (as set-up) or decompiler json file");
                }
                String jvmStr = args.get(i + 1);
                String classStr = args.get(i + 2);
                String decompilerName = args.get(i + 3);
                try {
                    VmInfo vmInfo = manager.findVmFromPID(jvmStr);
                    VmDecompilerStatus result = obtainClass(vmInfo, classStr, manager);
                    byte[] bytes = Base64.getDecoder().decode(result.getLoadedClassBytes());
                    PluginManager pluginManager = new PluginManager();
                    if (new File(decompilerName).exists() && decompilerName.toLowerCase().endsWith(".json")) {
                        throw new RuntimeException("Plugins laoded directly form file are noty impelemnetd");
                    }
                    List<DecompilerWrapperInformation> wrappers = pluginManager.getWrappers();
                    DecompilerWrapperInformation decompiler = null;
                    for (DecompilerWrapperInformation dw : wrappers) {
                        if (!dw.getScope().equals(DecompilerWrapperInformation.LOCAL_SCOPE) && dw.getName().equals(decompilerName)) {
                            decompiler = dw;
                        }
                    }
                    //LOCAL is preffered one
                    for (DecompilerWrapperInformation dw : wrappers) {
                        if (dw.getScope().equals(DecompilerWrapperInformation.LOCAL_SCOPE) && dw.getName().equals(decompilerName)) {
                            decompiler = dw;
                        }
                    }
                    if (decompiler != null) {
                        String decompiledClass = pluginManager.decompile(decompiler, bytes);
                        System.out.println(decompiledClass);
                    } else {
                        throw new RuntimeException("Decompiler " + decompilerName + " not found");
                    }
                } catch (NumberFormatException e) {
                    try {
                        URL u = new URL(jvmStr);
                        throw new RuntimeException("Remote VM not yet implemented");
                    } catch (MalformedURLException ee) {
                        throw new RuntimeException("Second param was supposed to be URL or PID", ee);
                    }
                }
                break;
            } else {
                printHelp();
                throw new RuntimeException("Unknown commandline switch " + arg);
            }
        }
    }

    private void printHelp() {
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + VERBOSE);
        System.out.println(VERBOSE + " will set this app to print out all Exceptions and some debugging strings. Then continues " );
        System.out.println(HELP + "/" + H + " print this help end exit" );
        System.out.println(LISTJVMS + " no arg expected, list available localhost JVMs " );
        System.out.println(LISTPLUGINS + "  no arg expected, currnently configured plugins with theirs status");
        System.out.println(LISTCLASSES + "  one arg - pid or url of JVM. List its laoded classes." );
        System.out.println(BYTES + "  two args - pid or url of JVM and  class to obtain - will stdou out its binary form" );
        System.out.println(BASE64 + "  two args - pid or url of JVM and  class to obtain - will stdou out its binary encoded as base64" );
        System.out.println(DECOMPILE+ "  three args - pid or url of JVM and  class to obtain and name/file of decompiler config - will stdou out decompiled class" );
        System.out.println("Allowed are: " + LISTJVMS + " , " + LISTPLUGINS + " , " + LISTCLASSES + ", " + BASE64 + " , " + BYTES + ", " + DECOMPILE + ", " + VERBOSE);
    }


    private static String invalidityToString(boolean invalidWrapper) {
        if (invalidWrapper) {
            return "invalid";
        } else {
            return "valid";
        }
    }

    private static VmDecompilerStatus obtainClass(VmInfo vmInfo, String clazz, VmManager manager) {
        AgentRequestAction request = VmDecompilerInformationController.createRequest(manager, vmInfo, clazz, AgentRequestAction.RequestAction.BYTES);
        String response = VmDecompilerInformationController.submitRequest(manager, request);
        if (response.equals("ok")) {
            VmDecompilerStatus vmStatus = manager.getVmDecompilerStatus(vmInfo);
            return vmStatus;
        } else {
            throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

        }
    }
}
