package org.jrd.backend.communication;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.jrd.backend.data.Config;

import java.io.IOException;
import java.util.List;

/**
 * This is Byteman's install library copied, with small modifications. This is
 * done with permission of Andrew Dinn, author of Byteman. For the original
 * source of this code, please follow links below:
 * http://byteman.jboss.org/  -- official page
 * https://github.com/bytemanproject/byteman -- git repository
 *
 * This is a provisional solution for the attachment, while I am trying to create
 * an abstract library to share some functionality.
 */
public final class InstallDecompilerAgentImpl {

    public static void install(
            String pid, boolean addToBoot, boolean setPolicy, String host, int port, String[] properties
    ) throws IllegalArgumentException, IOException, AttachNotSupportedException,
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

        InstallDecompilerAgentImpl install = new InstallDecompilerAgentImpl(
                pid, addToBoot, setPolicy, host, port, properties
        );
        install.locateAgent();
        install.attach();
        install.injectAgent();
    }

    private String agentJar;
    private String id;
    private int port;
    private String host;
    private boolean addToBoot;
    private boolean setPolicy;
    private String props;
    private VirtualMachine vm;
    private Config config = Config.getConfig();

    private InstallDecompilerAgentImpl(String pid, boolean addToBoot, boolean setPolicy,
            String host, int port, String[] properties) {

        agentJar = null;
        this.id = pid;
        this.port = port;
        this.addToBoot = addToBoot;
        this.setPolicy = setPolicy;
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

    /**
     * attach to the Java process identified by the process id supplied on the
     * command line
     */
    private void attach() throws AttachNotSupportedException, IOException, IllegalArgumentException {

        if (id.matches("[0-9]+")) {
            // integer process id
            int pid = Integer.parseInt(id);
            if (pid <= 0) {
                throw new IllegalArgumentException("Install : invalid pid " + id);
            }
            vm = VirtualMachine.attach(Integer.toString(pid));
        } else {
            // try to search for this VM with an exact match
            List<VirtualMachineDescriptor> descriptors = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd : descriptors) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }
                if (displayName.equals(id)) {
                    vm = VirtualMachine.attach(vmd);
                    return;
                }
            }
            // hmm, ok, lets see if we can find a trailing match e.g. if the displayName
            // is org.jboss.Main we will accept jboss.Main or Main
            for (VirtualMachineDescriptor descriptor : descriptors) {
                String displayName = descriptor.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }

                if (displayName.indexOf('.') >= 0 && displayName.endsWith(id)) {
                    // looking hopeful ensure the preceding char is a '.'
                    int idx = displayName.length() - (id.length() + 1);
                    if (displayName.charAt(idx) == '.') {
                        // yes it's a match
                        vm = VirtualMachine.attach(descriptor);
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
            if (props != null) {
                agentOptions += props;
            }
            vm.loadAgent(agentJar, agentOptions);
        } finally {
            vm.detach();
        }
    }

    private void locateAgent() throws IOException {
        agentJar = config.getAgentExpandedPath();
    }
}
