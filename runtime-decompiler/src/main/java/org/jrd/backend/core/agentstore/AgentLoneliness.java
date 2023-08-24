package org.jrd.backend.core.agentstore;

import java.util.Arrays;

public enum AgentLoneliness {
    SINGLE_INSTANCE,
    ANONYMOUS,
    FORCING,
    AF;

    @Override
    public String toString() {
        return super.toString();
    }

    public String toHelp() {
        switch (this) {
            case SINGLE_INSTANCE:
                return "Agent be allowed to attach to each process only once, unless " + FORCING + " is put to " +
                        "following attachment (default)";
            case ANONYMOUS:
                return "Agent will attach, but will not set the flag about its presence. Still, the property will be " +
                        "set. (weird)";
            case FORCING:
                return "Agent will attach, but will skip the check for single instance (sometimes useful)";
            case AF:
                return "Agent will attach, but will skip the check for single instance" +
                        "and will not set the flag about its presence. Still, the property will be set. (super weird)";
            default:
                throw new RuntimeException("Unknown " + AgentLoneliness.class.getSimpleName() + " value " + this);
        }
    }

    public String toButton() {
        switch (this) {
            case SINGLE_INSTANCE:
                return "SI";
            case ANONYMOUS:
                return "A";
            case FORCING:
                return "F";
            case AF:
                return "AF";
            default:
                throw new RuntimeException("Unknown " + AgentLoneliness.class.getSimpleName() + " value " + this);
        }
    }

    public static AgentLoneliness fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(AgentLoneliness.values()).filter(v -> v.toString().equals(s)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }
}
