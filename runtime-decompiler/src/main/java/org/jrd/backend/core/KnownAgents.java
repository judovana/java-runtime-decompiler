package org.jrd.backend.core;

import org.jrd.backend.communication.InstallDecompilerAgentImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KnownAgents {

    private enum AgentLiveliness {
        ONE_SHOT,
        SESSION,
        PERNAMENT
    }

    private static class KnownAgent {

        private final InstallDecompilerAgentImpl agent;
        private boolean live;
        private final AgentLiveliness ttl = AgentLiveliness.SESSION; //all agents are now sessioned

        public KnownAgent(InstallDecompilerAgentImpl install) {
            this.agent = install;
            this.live = true;
        }
    }

    private static List<KnownAgent> agents = Collections.synchronizedList(new ArrayList<>());

    public static void markDead(String hostname, int listenPort, String vmId, int vmPid) {
        for (KnownAgent agent : agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live) {
                agent.live = false;
                System.err.println("killing " + agent.agent.toString());
            }
        }
    }

    public static void injected(InstallDecompilerAgentImpl install) {
        agents.add(new KnownAgent(install));
        System.err.println("storing " + install.toString());
    }

    public static boolean isSessionPermanent(String hostname, int listenPort, String vmId, int vmPid) {
        for (KnownAgent agent : agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live && agent.ttl == AgentLiveliness.SESSION) {
                System.err.println("session agent " + agent.agent.toString());
                return true;
            }
        }
        return false;
    }
}
