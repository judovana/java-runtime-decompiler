package org.jrd.backend.core.agentstore;

import java.util.Arrays;

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
            default:
                throw new RuntimeException("Unknown " + AgentLiveliness.class.getSimpleName() + " value " + this);
        }
    }

    public static AgentLiveliness fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(AgentLiveliness.values()).filter(v -> v.toString().equals(s)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }
}
