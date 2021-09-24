package org.jrd.backend.communication;

public class TopLevelErrorCandidate extends ErrorCandidate {
    private static final String ERROR_RESPONSE = "error";

    public TopLevelErrorCandidate(String line) {
        super(line);
    }

    public static String toError(String message) {
        return ERROR_RESPONSE + " " + message;
    }

    public static String toError(Exception ex) {
        return toError(ex.toString());
    }

    @Override
    protected String getIdentifier() {
        return ERROR_RESPONSE;
    }

}
