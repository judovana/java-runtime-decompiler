package org.jrd.backend.core;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import org.jrd.backend.communication.InstallDecompilerAgentImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains methods for attaching the agent.
 */
public class AgentLoader {

    static final int INVALID_PORT = -1;
    private static final int PORT_MIN = 10900;
    private static final int MAX_PORT_SLOTS = 200;
    private static final int PORT_MAX = PORT_MIN + MAX_PORT_SLOTS;

    private static final String AGENT_PORT_PROPERTY = "com.redhat.decompiler.thermostat.port";


    AgentLoader() {
    }

    /**
     * This method handles the attachment of a decompiler agent to given VM.
     * @param pid PID of the VM
     * @return port number if successful, else {@link #INVALID_PORT}
     */
    public int attach(int pid) {
        int port = findPort();
        String[] installProps = createProperties(port);

        Logger.getLogger().log(
                Logger.Level.DEBUG,
                "Attempting to attach decompiler agent for VM '" + pid + "' on port '" + port + "'"
        );

        try {
            InstallDecompilerAgentImpl.install(
                    Integer.toString(pid), false, false, "localhost", port, installProps
            );
        } catch (IllegalArgumentException | IOException | AttachNotSupportedException |
                AgentLoadException | AgentInitializationException ex) {
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Attach failed!! Cause: ", ex));
            return INVALID_PORT;
        }

        if (port > 0) {
            return port;
        } else {
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
                Logger.getLogger().log(
                        Logger.Level.DEBUG,
                        new RuntimeException("Could not open socket on port " + i + ". Trying again.", e)
                );
            }
        }

        throw new IllegalStateException("No ports available in range [" + PORT_MIN + "," + PORT_MAX + "]");
    }

    private String[] createProperties(int port) {
        List<String> properties = new ArrayList<>();

        properties.add(AGENT_PORT_PROPERTY + "=" + port);

        return properties.toArray(new String[]{});
    }
}
