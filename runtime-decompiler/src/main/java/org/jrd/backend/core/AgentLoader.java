package org.jrd.backend.core;

import org.jrd.backend.communication.InstallDecompilerAgentImpl;

import com.sun.tools.attach.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains methods for attaching the agent.
 */
public class AgentLoader {

    //private static final Logger logger = LoggingUtils.getLogger(AgentLoader.class);
    private static final int PORT_MIN = 10900;
    private static final int MAX_PORT_SLOTS = 200;
    private static final int PORT_MAX = PORT_MIN + MAX_PORT_SLOTS;
    //private final ProcessChecker processChecker;
    static final int INVALID_PORT = -1;

    private static final String AGENT_PORT_PROPERTY = "com.redhat.decompiler.thermostat.port";


    AgentLoader() {
       
    }

    /**
     * This method handles the attachment of a decompiler agent to given VM.
     * @param pid PID of the VM
     * @return AgentInfo object, if successful, else null
     */
    public int attach(int pid) {
        int port = findPort();
        //logger.finest("Attempting to attach decompiler agent for VM '" + pid + "' on port '" + port + "'");
        try {
            String[] installProps = createProperties(port);
            try {
                InstallDecompilerAgentImpl.install(Integer.toString(pid), false, false, "localhost", port, installProps);
            } catch (IllegalArgumentException | IOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException ex) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Attach failed!! Cause: " + ex.getMessage(), ex));
                return INVALID_PORT;
            }

            if (port > 0) {
                return port; // new AgentInfo(pid, port, null, vmId, agentId, false);
            } else {
                return INVALID_PORT;
            }
        } catch (IllegalArgumentException | IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Unable to attach decompiler agent to VM '" + pid + "' on port '" + port + "'", e));
        return INVALID_PORT;
        }
    }

    private int findPort() {
        for (int i = PORT_MIN; i <= PORT_MAX; i++) {
            try {
                try (ServerSocket s = new ServerSocket(i)) {
                    s.close();
                    return i;
                }
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, new RuntimeException("Could not open socket on port " + i + ". Trying again.", e));

            }
        }
        throw new IllegalStateException("No ports available in range [" + PORT_MIN + "," + PORT_MAX + "]");
    }



    private String[] createProperties(int port) throws IOException {
        List<String> properties = new ArrayList<>();
        String agentPortProperty = AGENT_PORT_PROPERTY + "=" + port;
        properties.add(agentPortProperty);
        return properties.toArray(new String[]{});
    }
}
