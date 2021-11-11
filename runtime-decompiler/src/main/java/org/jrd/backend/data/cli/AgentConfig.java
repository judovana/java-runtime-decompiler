package org.jrd.backend.data.cli;

import org.jrd.backend.core.KnownAgents;
import org.jrd.backend.core.Logger;

import java.util.List;
import java.util.Optional;

class AgentConfig {

    final KnownAgents.AgentLoneliness loneliness;
    final Optional<Integer> port;
    final KnownAgents.AgentLiveliness liveliness;

    private AgentConfig(KnownAgents.AgentLoneliness loneliness, KnownAgents.AgentLiveliness liveliness, Optional<Integer> port) {
        this.loneliness = loneliness;
        this.port = port;
        this.liveliness = liveliness;
    }

    public static AgentConfig create(List<String> agentArgs) {
        int[] futureTypeUnderstood = new int[]{0, 0, 0};
        KnownAgents.AgentLiveliness liveliness = null;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    liveliness = KnownAgents.AgentLiveliness.fromString(agentArgs.get(i));
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        KnownAgents.AgentLoneliness loneliness = null;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    loneliness = KnownAgents.AgentLoneliness.fromString(agentArgs.get(i));
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        Integer port = null;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    port = Integer.valueOf(agentArgs.get(i));
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        boolean failed = false;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                failed = true;
                Logger.getLogger().log(Logger.Level.ALL, "Cant parse " + (i + 1) + ".:" + agentArgs.get(i));
            } else if (futureTypeUnderstood[i] > 1) {
                failed = true;
                Logger.getLogger().log(Logger.Level.ALL, "applied more then once " + (i + 1) + ".:" + agentArgs.get(i));
            }
        }
        if (failed) {
            Logger.getLogger().log(Logger.Level.ALL, Help.AGENT_TEXT);
            throw new RuntimeException("parsing of agent params failed, exiting without action");
        }
        if (liveliness == null) {
            liveliness = KnownAgents.AgentLiveliness.SESSION;
        }
        if (loneliness == null) {
            loneliness = KnownAgents.AgentLoneliness.SINGLE_INSTANCE;
        }
        Logger.getLogger().log(
                Logger.Level.DEBUG,
                String.format("Agent set to attach %s, %s and port=%s", liveliness, loneliness, port == null ? "guessed" : port.toString())
        );
        return new AgentConfig(loneliness, liveliness, Optional.ofNullable(port));
    }
}
