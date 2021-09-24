package org.jrd.backend.communication;

public class ErrorCandidate {

    private static final String ERROR_ID = "ERROR";
    private final String line;

    public ErrorCandidate(String line) {
        this.line = line;
    }

    protected String getIdentifier() {
        return ERROR_ID;
    }

    public String getLine() {
        return line;
    }

    public boolean isError() {
        return isErrorImpl(getLine()) != null;
    }

    public String getErrorMessage() {
        if (isError()) {
            return isErrorImpl(getLine());
        } else {
            return "This was not error - " + getLine();
        }
    }

    public static String toError(String message) {
        return ERROR_ID + " " + message;
    }

    public static String toError(Exception ex) {
        return toError(ex.toString());
    }

    /**
     * @return null if line is not error, empty string or message otherwise
     */
    private String isErrorImpl(String linex) {
        if (linex.startsWith(getIdentifier())) {
            String message = linex.replaceAll("^" + getIdentifier(), "").trim();
            return message;
        } else {
            return null;
        }
    }
}
