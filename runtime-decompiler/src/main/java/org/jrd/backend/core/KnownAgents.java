package org.jrd.backend.core;

import org.jrd.backend.communication.InstallDecompilerAgentImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KnownAgents {

    public enum AgentLiveliness {
        ONE_SHOT,
        SESSION,
        PERMANENT;

        @Override
        public String toString() {
            return super.toString();
        }

        public String toHelp() {
            switch (this) {
                case ONE_SHOT:
                    return "Agent will connect, do its job and disconnect.";
                case SESSION:
                    return "Agent will connect and will remain connected untill end of session.";
                case PERMANENT:
                    return "Agent will attach, and will disconnect only manually or on death of target process";
            }
            throw new RuntimeException("Unknown " + AgentLiveliness.class.getSimpleName() + " value " + this);
        }

        public static AgentLiveliness fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(AgentLiveliness.values()).filter(v -> v.toString().equals(s)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    public enum AgentLoneliness {
        SINGLE_INSTANCE,
        ANONYMOUS,
        FORCING;

        @Override
        public String toString() {
            return super.toString();
        }

        public String toHelp() {
            switch (this) {
                case SINGLE_INSTANCE:
                    return "Agent be allowed to attach to each process only once, unless " + FORCING + " is put to following attachment";
                case ANONYMOUS:
                    return "Agent will attach, but will not set the flag about its presence. Still, the property will be set.";
                case FORCING:
                    return "Agent will attach, but will skip the check for single instance";
            }
            throw new RuntimeException("Unknown " + AgentLoneliness.class.getSimpleName() + " value " + this);
        }

        public static AgentLoneliness fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(AgentLoneliness.values()).filter(v -> v.toString().equals(s)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    private static class KnownAgent {

        private final InstallDecompilerAgentImpl agent;
        private boolean live;
        private final AgentLiveliness ttl;

        public KnownAgent(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
            this.agent = install;
            this.ttl = ttl;
            this.live = true;
        }
    }

    private static List<KnownAgent> agents = Collections.synchronizedList(new ArrayList<>());

    public static void checkDead(String hostname, int listenPort, String vmId, int vmPid) {
        markDead(hostname, listenPort, vmId, vmPid, false,  findAgents(hostname, listenPort, vmId, vmPid), true);
    }

    public static void markDead(String hostname, int listenPort, String vmId, int vmPid) {
        markDead(hostname, listenPort, vmId, vmPid, true, findAgents(hostname, listenPort, vmId, vmPid), true);
    }

    private static void markDead(String hostname, int listenPort, String vmId, int vmPid, boolean action, List<KnownAgent> matchingAgents, boolean all) {
        if (matchingAgents.size() == 0) {
            System.err.println(String.format(
                    "not found agent for hostname=%s port=%d vmId=%s vmPid=%d in list of %d agents",
                    hostname, listenPort, vmId, vmPid, agents.size()));
            return;
        }
        if (matchingAgents.size() > 1) {
            System.err.println(String.format(
                    "found %d agents for hostname=%s port=%d vmId=%s vmPid=%d in list of %d agents",
                    matchingAgents.size(), hostname, listenPort, vmId, vmPid, agents.size()));
            if (!all) {
                return;
            }
        }
        for (KnownAgent agent : matchingAgents) {
            if (action) {
                agent.live = false;
                System.err.println("killing " + agent.agent.toString());
            } else {
                System.err.println("not killing " + agent.agent.toString());
            }
        }
    }

    public static List<KnownAgent> findAgents(String hostname, int listenPort, String vmId, int vmPid) {
        List<KnownAgent> result = new ArrayList<>();
        for (KnownAgent agent : agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live) {
                result.add(agent);
            }
        }
        return result;
    }

    public static void injected(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
        agents.add(new KnownAgent(install, ttl));
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

    public static boolean isPermanent(String hostname, int listenPort, String vmId, int vmPid) {
        for (KnownAgent agent : agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live && agent.ttl == AgentLiveliness.PERMANENT) {
                System.err.println("permanent agent " + agent.agent.toString());
                return true;
            }
        }
        return false;
    }

    public static boolean isOneShot(String hostname, int listenPort, String vmId, int vmPid) {
        for (KnownAgent agent : agents) {
            if (agent.agent.matches(hostname, listenPort, vmId, vmPid) && agent.live && agent.ttl == AgentLiveliness.ONE_SHOT) {
                System.err.println("single usage agent " + agent.agent.toString());
                return true;
            }
        }
        return false;
    }
}
