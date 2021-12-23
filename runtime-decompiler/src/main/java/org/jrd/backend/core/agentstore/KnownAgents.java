package org.jrd.backend.core.agentstore;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jrd.backend.communication.InstallDecompilerAgentImpl;
import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmManager;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class KnownAgents {

    private static final class KnownAgentsHolder {
        private static final KnownAgents INSTANCE = new KnownAgents();
    }

    public static KnownAgents getInstance() {
        return KnownAgentsHolder.INSTANCE;
    }

    public static final Path JRD_TMP_FILE = new File(System.getProperty("java.io.tmpdir") + "/jrdAgents").toPath();
    private final List<KnownAgent> agents;

    public void markDead(String hostname, int listenPort, int vmPid) {
        List<KnownAgent> found = findAgents(hostname, listenPort, vmPid);
        if (vmPid <= 0) {
            found = findAgents(hostname, listenPort);
        } else if (hostname == null || hostname.trim().isEmpty() || listenPort <= 0) {
            found = findAgents(vmPid);
        }
        markDead(hostname, listenPort, vmPid, true, found, true);
    }

    private void markDead(String hostname, int listenPort, int vmPid, boolean action, List<KnownAgent> matchingAgents, boolean all) {
        if (matchingAgents.size() == 0) {
            System.err.println(
                    String.format(
                            "not found agent for hostname=%s port=%d vmPid=%d in list of %d live agents (total agents %d)", hostname, listenPort, vmPid,
                            agents.stream().filter(a->a.isLive()).collect(Collectors.toList()).size(), agents.size()
                    )
            );
            return;
        }
        if (matchingAgents.size() > 1) {
            System.err.println(
                    String.format(
                            "found %d agents for hostname=%s port=%d vmPid=%d in list of %d live agents (total agents %d)", matchingAgents.size(), hostname,
                            listenPort, vmPid,agents.stream().filter(a->a.isLive()).collect(Collectors.toList()).size(), agents.size()
                    )
            );
            if (!all) {
                return;
            }
        }
        for (KnownAgent agent : matchingAgents) {
            if (action) {
                agent.markKilled();
                System.err.println("killing " + agent.toString());
            } else {
                System.err.println("not killing " + agent.toString());
            }
        }
        save();
    }

    public List<KnownAgent> findAgents(String hostname, int listenPort, int vmPid) {
        List<KnownAgent> result = new ArrayList<>();
        for (KnownAgent agent : agents) {
            if (agent.matches(hostname, listenPort, vmPid) && agent.isLive()) {
                result.add(agent);
            }
        }
        return result;
    }

    public List<KnownAgent> findAgents(int vmPid) {
        List<KnownAgent> result = new ArrayList<>();
        for (KnownAgent agent : agents) {
            if (agent.getPid() == vmPid && agent.isLive()) {
                result.add(agent);
            }
        }
        return result;
    }

    public List<KnownAgent> findAgents(String hostname, int listenPort) {
        List<KnownAgent> result = new ArrayList<>();
        for (KnownAgent agent : agents) {
            if (agent.getHost().equals(hostname) && agent.getPort() == listenPort && agent.isLive()) {
                result.add(agent);
            }
        }
        return result;
    }

    public void injected(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
        agents.add(new KnownAgent(install, ttl));
        System.err.println("storing " + install.toString());
        save();
    }

    private void save() {
        //TODO lock
        //TODO before save, load an merge
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.writeString(JRD_TMP_FILE, gson.toJson(agents));
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
    }

    @SuppressWarnings("ModifiedControlVariable")
    public void killAllSessionAgents(VmManager vmManager) {
        long currentPid = ProcessHandle.current().pid();
        for (int i = 0; i < agents.size(); i++) {
            KnownAgent agent = agents.get(i);
            if (agent.getLiveliness() == AgentLiveliness.SESSION && agent.isLive() && agent.getOwner() == currentPid) {
                agents.remove(i);
                i--;
                Logger.getLogger().log("detaching session agent " + i + "/" + agents.size());
                DecompilerRequestReceiver
                        .getHaltAction(agent.getHost(), agent.getPort(), "none", 0, new AgentAttachManager(vmManager), vmManager, false);
                Logger.getLogger().log(agent.getHost() + ":" + agent.getPort() + " should be detached successfully");
            }
        }
        save();
    }

    private KnownAgents() {
        agents = load();
        verifyAgents(); //in each load?
    }

    private List<KnownAgent> load() {
        //TODO lock
        List<KnownAgent> lagents = null;
        if (JRD_TMP_FILE.toFile().exists()) {
            try {
                try (Reader reader = Files.newBufferedReader(JRD_TMP_FILE)) {
                    lagents = Collections.synchronizedList(new Gson().fromJson(reader, new TypeToken<List<KnownAgent>>() {
                    }.getType()));

                }
            } catch (Exception ex) {
                Logger.getLogger().log(Logger.Level.ALL, ex);
            }
        } else {
            lagents = Collections.synchronizedList(new ArrayList<>());
        }
        return lagents;
    }

    @SuppressWarnings("ModifiedControlVariable")
    private void verifyAgents() {
        for (int i = 0; i < agents.size(); i++) {
            KnownAgent agent = agents.get(i);
            boolean isVerified = agent.verify();
            if (!isVerified) {
                agents.remove(i);
                Logger.getLogger().log("removed " + i + "/" + agents.size());
                i--;
            }
        }
        save();
    }
}
