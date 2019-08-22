package org.jrd.backend.communication;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Config;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;
import org.jboss.byteman.agent.install.VMInfo;

/**
 * This is byteman's install library copied, with small modifications. This is
 * done with permission of Andrew Dinn, author of Byteman. For the original
 * source of this code, please follow links below:
 * http://byteman.jboss.org/  -- official page
 * https://github.com/bytemanproject/byteman -- git repository
 *
 * This is a provisional solution for the attach, while I am trying to create
 * an abstract library to share some functionality.
 */
public class InstallDecompilerAgentImpl {

    public static void install(String pid, boolean addToBoot, String host, int port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException {
        install(pid, addToBoot, false, host, port, properties);
    }

    public static void install(String pid, boolean addToBoot, boolean setPolicy, String host, int port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException {
        install(pid, addToBoot, setPolicy, false, host, port, properties);
    }

 
    public static void install(String pid, boolean addToBoot, boolean setPolicy, boolean useModuleLoader, String host, int port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException {

        if (port < 0) {
            throw new IllegalArgumentException("Install : port cannot be negative");
        }

        for (int i = 0; i < properties.length; i++) {
            String prop = properties[i];
            if (prop == null || prop.length() == 0) {
                throw new IllegalArgumentException("Install : properties  cannot be null or \"\"");
            }
            if (prop.indexOf(',') >= 0) {
                throw new IllegalArgumentException("Install : properties may not contain ','");
            }
        }

        InstallDecompilerAgentImpl install = new InstallDecompilerAgentImpl(pid, addToBoot, setPolicy, useModuleLoader, host, port, properties);
        install.locateAgent();
        install.attach();
        install.injectAgent();
    }


    public static VMInfo[] availableVMs() {
        List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
        VMInfo[] vmInfo = new VMInfo[vmds.size()];
        int i = 0;
        for (VirtualMachineDescriptor vmd : vmds) {
            vmInfo[i++] = new VMInfo(vmd.id(), vmd.displayName());
        }

        return vmInfo;
    }

    private static final String AGENT_PORT_PROPERTY = "com.redhat.decompiler.thermostat.port";
    private static final String AGENT_HOME_SYSTEM_PROP = "com.redhat.decompiler.thermostat.home";
    private static final String DECOMPILER_HOME_ENV_VARIABLE = "THERMOSTAT_DECOMPILER_AGENT_HOME";
    private static final String DECOMPILER_JAR_ENV_VARIABLE = "THERMOSTAT_DECOMPILER_AGENT_JAR";
    private static final String DECOMPILER_PREFIX = "com.redhat.decompiler.thermostat";

    private String agentJar;
    private String modulePluginJar;
    private String id;
    private int port;
    private String host;
    private boolean addToBoot;
    private boolean setPolicy;
    private boolean useModuleLoader;
    private String props;
    private VirtualMachine vm;
    private Config config = Config.getConfig();

    public static String getSystemProperty(String id, String property) {
        return getProperty(id, property);
    }

    private static String getProperty(String id, String property) {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(id);
            return (String) vm.getSystemProperties().get(property);
        } catch (AttachNotSupportedException | IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
            return null;
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException e) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
                }
            }
        }
    }
    private String DECOMPILER_HOME_SYSTEM_PROP = DECOMPILER_PREFIX + ".home";
    private String DECOMPILER_AGENT_NAME = "decompiler-agent-1.0.0";
    private String DECOMPILER_AGENT_BASE_DIR = "target";

    private InstallDecompilerAgentImpl(String pid, boolean addToBoot, boolean setPolicy,
            boolean useModuleLoader, String host, int port, String[] properties) {

        agentJar = null;
        modulePluginJar = null;
        this.id = pid;
        this.port = port;
        this.addToBoot = addToBoot;
        this.setPolicy = setPolicy;
        this.useModuleLoader = useModuleLoader;
        this.host = host;
        if (properties != null) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < properties.length; i++) {
                builder.append(",prop:");
                builder.append(properties[i]);
            }
            props = builder.toString();
        } else {
            props = "";
        }
        vm = null;
    }

    private InstallDecompilerAgentImpl(String pid, boolean addToBoot, String host, int port, String[] properties) {
        this(pid, addToBoot, false, false, host, port, properties);
    }

    /**
     * attach to the Java process identified by the process id supplied on the
     * command line
     */
    private void attach() throws AttachNotSupportedException, IOException, IllegalArgumentException {

        if (id.matches("[0-9]+")) {
            // integer process id
            int pid = Integer.valueOf(id);
            if (pid <= 0) {
                throw new IllegalArgumentException("Install : invalid pid " + id);
            }
            vm = VirtualMachine.attach(Integer.toString(pid));
        } else {
            // try to search for this VM with an exact match
            List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd : vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }
                if (displayName.equals(id)) {
                    String pid = vmd.id();
                    vm = VirtualMachine.attach(vmd);
                    return;
                }
            }
            // hmm, ok, lets see if we can find a trailing match e.g. if the displayName
            // is org.jboss.Main we will accept jboss.Main or Main
            for (VirtualMachineDescriptor vmd : vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }

                if (displayName.indexOf('.') >= 0 && displayName.endsWith(id)) {
                    // looking hopeful ensure the preceding char is a '.'
                    int idx = displayName.length() - (id.length() + 1);
                    if (displayName.charAt(idx) == '.') {
                        // yes it's a match
                        String pid = vmd.id();
                        vm = VirtualMachine.attach(vmd);
                        return;
                    }
                }
            }

            // no match so throw an exception
            throw new IllegalArgumentException("Install : invalid pid " + id);
        }

    }

    /**
     * get the attached process to upload and install the agent jar using
     * whatever agent options were configured on the command line
     */
    private void injectAgent() throws AgentLoadException, AgentInitializationException, IOException {
        try {
            // we need at the very least to enable the listener so that scripts can be uploaded
            String agentOptions = "listener:true";
            if (host != null && host.length() != 0) {
                agentOptions += ",address:" + host;
            }
            if (port != 0) {
                agentOptions += ",port:" + port;
            }
            if (addToBoot) {
                agentOptions += ",boot:" + agentJar;
            }
            if (setPolicy) {
                agentOptions += ",policy:true";
            }
            /* if (useModuleLoader) {
                agentOptions += ",modules:org.jboss.byteman.modules.jbossmodules.JBossModulesSystem,sys:" + modulePluginJar;
            }*/
            if (props != null) {
                agentOptions += props;
            }
            vm.loadAgent(agentJar,
                     agentOptions);
            
        } finally {
            vm.detach();
        }
    }

     /**
     * check the supplied arguments and stash away the relevant data
     * @param args the value supplied to main
     */
    private void parseArgs(String[] args) {
        int argCount = args.length;
        int idx = 0;
        if (idx == argCount) {
            //usage(0);
        }

        String nextArg = args[idx];

        while (nextArg.length() != 0
                && nextArg.charAt(0) == '-') {
            if (nextArg.equals("-p")) {
                idx++;
                if (idx == argCount) {
                    //usage(1);
                }
                nextArg = args[idx];
                idx++;
                try {
                    port = Integer.decode(nextArg);
                } catch (NumberFormatException e) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Install : invalid value for port " + nextArg, e));
                }
            } else if (nextArg.equals("-h")) {
                idx++;
                if (idx == argCount) {
                    //usage(1);
                }
                nextArg = args[idx];
                idx++;
                host = nextArg;
            } else if (nextArg.equals("-b")) {
                idx++;
                addToBoot = true;
            } else if (nextArg.equals("-s")) {
                idx++;
                setPolicy = true;
            } else if (nextArg.equals("-m")) {
                idx++;
                useModuleLoader = true;
            } else if (nextArg.startsWith("-D")) {
                idx++;
                String prop = nextArg.substring(2);
                if (!prop.startsWith(DECOMPILER_PREFIX) || prop.contains(",")) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Install : invalid property setting " + prop));
                    //usage(1);
                }
                props = props + ",prop:" + prop;
            } else if (nextArg.equals("--help")) {
                //usage(0);
            } else {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Install : invalid option " + args[idx]));
                //usage(1);
            }
            if (idx == argCount) {
                //usage(1);
            }
            nextArg = args[idx];
        }

        if (idx != argCount - 1) {
            //usage(1);
        }

        // we actually allow any string for the process id as we can look up by name also
        id = nextArg;
    }

    
    
    private void locateAgent() throws IOException
    {
        agentJar = config.getAgentExpandedPath();
    }

    /**
     *
     * @param bmHome
     * @param baseDir
     * @param libName
     * @return
     * @throws IOException
     */
    public String locateJarFromHomeDir(String bmHome, String baseDir, String libName) throws IOException
    {
        if (bmHome.endsWith("/")) {
            bmHome = bmHome.substring(0, bmHome.length() - 1);
        }

        File bmHomeFile = new File(bmHome);
        if (!bmHomeFile.isDirectory()) {
            throw new FileNotFoundException("Install : " + bmHome + " does not identify a directory");
        }

        File bmLibFile = new File(bmHome + "/" + baseDir);
        if (!bmLibFile.isDirectory()) {
            throw new FileNotFoundException("Install : " + bmHome + "/" + baseDir + " does not identify a directory");
        }

        try {
            JarFile jarFile = new JarFile(bmHome + "/" + baseDir + "/" + libName + ".jar");
        } catch (IOException e) {
            throw new IOException("Install : " + bmHome + "/" + baseDir + "/" + libName + ".jar is not a valid jar file", e);
        }

        return bmHome + "/" + baseDir + "/" + libName + ".jar";
    }

    /**
     *
     * @param libName
     * @return
     * @throws IOException
     */
    public String locateJarFromClasspath(String libName) throws IOException
    {
        String javaClassPath = System.getProperty("java.class.path");
        String pathSepr = System.getProperty("path.separator");
        String fileSepr = System.getProperty("file.separator");
        final String EXTENSION = ".jar";
        final int EXTENSION_LEN = EXTENSION.length();
        final int NAME_LEN = libName.length();
        final String VERSION_PATTERN = "-[0-9]+\\.[0-9]+\\.[0-9]+.*";

        String[] elements = javaClassPath.split(pathSepr);
        String jarname = null;
        for (String element : elements) {
            if (element.endsWith(EXTENSION)) {
                String name = element.substring(0, element.length() - EXTENSION_LEN);
                int lastFileSepr = name.lastIndexOf(fileSepr);
                if (lastFileSepr >= 0) {
                    name= name.substring(lastFileSepr+1);
                }
                if (name.startsWith(libName)) {
                    if (name.length() == NAME_LEN) {
                        jarname = element;
                        break;
                    }
                    //  could be a contender --  check it only has a standard version suffix
                    // i.e. "-NN.NN.NN-ANANAN"
                    String version = name.substring(NAME_LEN);
                    if (version.matches(VERSION_PATTERN)) {
                        jarname =  element;
                        break;
                    }
                }
            }
        }

        if (jarname != null) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, libName + " jar is " + jarname);
            return jarname;
        } else {
            throw new  FileNotFoundException("Install : cannot find " + libName + " jar please set environment variable " 
                    + DECOMPILER_HOME_ENV_VARIABLE + " or System property " + DECOMPILER_HOME_SYSTEM_PROP);
        }
    }

}
