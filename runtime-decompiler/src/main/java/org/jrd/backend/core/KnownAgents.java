package org.jrd.backend.core;

import org.jrd.backend.communication.InstallDecompilerAgentImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KnownAgents {

        private static class KnownAgent {

        private final InstallDecompilerAgentImpl agent;
        private boolean live;

        public KnownAgent(InstallDecompilerAgentImpl install) {
            this.agent = install;
            this.live = true;
        }
    }

    private static List<KnownAgent> agents  = Collections.synchronizedList(new ArrayList<>());

    public static void markDead(String hostname, int listenPort, String vmId, int vmPid) {
        for(KnownAgent agent: agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live){
                agent.live = false;
                System.err.println("killing " + agent.agent.toString());
            }
        }
    }

    public static void injected(InstallDecompilerAgentImpl install) {
        agents.add(new KnownAgent(install));
        System.err.println("storing " + install.toString());
    }
}
