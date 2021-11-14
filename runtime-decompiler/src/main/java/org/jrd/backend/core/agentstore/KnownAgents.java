package org.jrd.backend.core.agentstore;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jrd.backend.communication.InstallDecompilerAgentImpl;
import org.jrd.backend.core.Logger;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KnownAgents {

    private static final class KnownAgentsHolder {
        private static final KnownAgents INSTANCE = new KnownAgents();
    }

    public static KnownAgents getInstance() {
        return KnownAgentsHolder.INSTANCE;
    }

    private static final Path JRD_TMP_FILE = new File(System.getProperty("java.io.tmpdir") + "/jrdAgents").toPath();
    private final List<KnownAgent> agents;

    public void markDead(String hostname, int listenPort, int vmPid) {
        markDead(hostname, listenPort, vmPid, true, findAgents(hostname, listenPort, vmPid), true);
    }

    private void markDead(String hostname, int listenPort, int vmPid, boolean action, List<KnownAgent> matchingAgents, boolean all) {
        if (matchingAgents.size() == 0) {
            System.err.println(
                    String.format(
                            "not found agent for hostname=%s port=%d vmPid=%d in list of %d agents", hostname, listenPort, vmPid,
                            agents.size()
                    )
            );
            return;
        }
        if (matchingAgents.size() > 1) {
            System.err.println(
                    String.format(
                            "found %d agents for hostname=%s port=%d vmPid=%d in list of %d agents", matchingAgents.size(), hostname,
                            listenPort, vmPid, agents.size()
                    )
            );
            if (!all) {
                return;
            }
        }
        for (KnownAgent agent : matchingAgents) {
            if (action) {
                agent.setLive(false);
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

    public void injected(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
        agents.add(new KnownAgent(install, ttl));
        System.err.println("storing " + install.toString());
        save();
    }

    private void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.writeString(JRD_TMP_FILE, gson.toJson(agents));
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
    }

    public void killAllSessionAgents() {
        //TODO
        System.err.println("TODO kill all session agents");
    }

    private KnownAgents() {
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
        agents = lagents;
    }
}
