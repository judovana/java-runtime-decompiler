package org.jrd.backend.communication;

import org.jrd.backend.core.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is handling opening of communication socket and request submitting.
 */
public class CallDecompilerAgent implements DelegatingJrdAgent {

    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT = 5395;

    private final int port;
    private final String address;
    private final List<JrdAgent> delegationCandidates = new ArrayList<>(1);

    /**
     * Constructor of the object
     * @param port port where to open socket
     * @param host socket host
     */
    public CallDecompilerAgent(int port, String host) {
        if (host == null) {
            host = DEFAULT_ADDRESS;
        }

        if (port <= 0) {
            port = DEFAULT_PORT;
        }

        this.address = host;
        this.port = port;
    }

    /**
     * Opens a socket and sends the request to the agent via socket.
     * @param request either "CLASSES" or "BYTES \n className", other formats
     * are refused
     * @return agents response or null
     */
    @Override
    public String submitRequest(final String request) {
        final Communicate comm = new Communicate(this.address, this.port);
        try {
            comm.println(request);
            return comm.readResponse();
        } catch (IOException ex) {
            Logger.getLogger().log(Logger.Level.DEBUG, ex);
            return null;
        } finally {
            comm.close();
        }
    }

    @Override
    public JrdAgent addDelegatingAgent(JrdAgent agent) {
        if (!delegationCandidates.contains(agent)) {
            delegationCandidates.add(agent);
            return agent;
        } else {
            return null;
        }
    }

    @Override
    public JrdAgent removeDelegatingAgent(JrdAgent agent) {
        if (delegationCandidates.contains(agent)) {
            delegationCandidates.remove(agent);
            return agent;
        } else {
            return null;
        }
    }

    @Override
    public int cleanDelegatingAgents() {
        int r = delegationCandidates.size();
        delegationCandidates.clear();
        return r;
    }
}
