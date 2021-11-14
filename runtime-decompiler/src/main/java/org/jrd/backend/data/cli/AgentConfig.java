package org.jrd.backend.data.cli;

import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.agentstore.AgentLoneliness;
import org.jrd.backend.core.Logger;

import java.util.List;
import java.util.Optional;

public final class AgentConfig {

    final AgentLoneliness loneliness;
    final Optional<Integer> port;
    final AgentLiveliness liveliness;

    private AgentConfig(AgentLoneliness loneliness, AgentLiveliness liveliness, Optional<Integer> port) {
        this.loneliness = loneliness;
        this.port = port;
        this.liveliness = liveliness;
    }

    @SuppressWarnings({"CyclomaticComplexity"}) // un-refactorable
    public static AgentConfig create(List<String> agentArgs, boolean session) {
        int[] futureTypeUnderstood = new int[]{0, 0, 0};
        AgentLiveliness liveliness = null;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    liveliness = AgentLiveliness.fromString(agentArgs.get(i));
                    futureTypeUnderstood[i]++;
                    break;
                } catch (Exception e) {
                    //no interest, will just remain null
                }
            }
        }
        AgentLoneliness loneliness = null;
        for (int i = 0; i < agentArgs.size(); i++) {
            if (futureTypeUnderstood[i] == 0) {
                try {
                    loneliness = AgentLoneliness.fromString(agentArgs.get(i));
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
            if (session) {
                liveliness = AgentLiveliness.SESSION;
            } else {
                liveliness = AgentLiveliness.ONE_SHOT;
            }
        }
        if (loneliness == null) {
            loneliness = AgentLoneliness.SINGLE_INSTANCE;
        }
        Logger.getLogger().log(
                Logger.Level.DEBUG,
                String.format("Agent set to attach %s, %s and port=%s", liveliness, loneliness, port == null ? "guessed" : port.toString())
        );
        return new AgentConfig(loneliness, liveliness, Optional.ofNullable(port));
    }
}
