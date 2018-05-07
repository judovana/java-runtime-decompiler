package com.redhat.thermostat.vm.decompiler.core;

import com.redhat.thermostat.vm.decompiler.communication.InstallDecompilerAgentImpl;

import com.sun.tools.attach.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains methods for attaching the agent.
 */
public class AgentLoader {

    //private static final Logger logger = LoggingUtils.getLogger(AgentLoader.class);
    private static final int PORT_MIN = 10101;
    private static final int MAX_PORT_SLOTS = 200;
    private static final int PORT_MAX = PORT_MIN + MAX_PORT_SLOTS;
    //private final ProcessChecker processChecker;
    static final String LOCALHOST = "localhost";
    static final int INVALID_PORT = -1;

    private static final String AGENT_LOADED_PROPERTY = "com.redhat.decompiler.thermostat.loaded";
    private static final String AGENT_PORT_PROPERTY = "com.redhat.decompiler.thermostat.port";
    private static final String HELPER_SOCKET_NAME_PROPERTY = "com.redhat.decompiler.thermostat.socketName";
    private static final String AGENT_HOME_SYSTEM_PROP = "com.redhat.decompiler.thermostat.home";
    private static final String DECOMPILER_HOME_ENV_VARIABLE = "DECOMPILER_HOME";
    private static final String DECOMPILER_PREFIX = "com.redhat.decompiler.thermostat";


    AgentLoader() {
       
    }

    /**
     * This method handles the attach of a decompiler agent to given VM.
     * @param vmId ID of VM to which we attach the agent
     * @param pid PID of the VM
     * @return AgentInfo object, if successful, else null
     */
    public int attach(String vmId, int pid) {
        int port = findPort();
        //logger.finest("Attempting to attach decompiler agent for VM '" + pid + "' on port '" + port + "'");
        try {
            String[] installProps = createProperties(port);
            boolean agentJarToBootClassPath = true;
            try{  
            InstallDecompilerAgentImpl.install(Integer.toString(pid), false, "localhost", port, installProps);
                } catch (IllegalArgumentException | IOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException ex) {
                    //logger.log(Level.SEVERE, "Attach failed!! Cause: " + ex.getMessage());
                    return INVALID_PORT;
                }

            if (port > 0) {
                return port;//new AgentInfo(pid, port, null, vmId, agentId, false);
            } else {
                return INVALID_PORT;
            }
        } catch (IllegalArgumentException | IOException e) {
            //logger.log(Level.WARNING, "Unable to attach decompiler agent to VM '" + pid + "' on port '" + port + "'");
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
                //logger.log(Level.INFO, "Could not open socket on port " + i + ". Trying again.");
            }
        }
        throw new IllegalStateException("No ports available in range [" + PORT_MIN + "," + PORT_MAX + "]");
    }



    private String[] createProperties(int port) throws IOException {
        List<String> properties = new ArrayList<>();
        String agentPortProperty = AGENT_PORT_PROPERTY + "=" + Integer.valueOf(port).toString();
        properties.add(agentPortProperty);
        return properties.toArray(new String[]{});
    }
}
