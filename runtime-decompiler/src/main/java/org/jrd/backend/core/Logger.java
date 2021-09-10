package org.jrd.backend.core;

/**
 * Class for logging Strings and Exceptions
 */
public class Logger {

    private static final String NULL_OBJECT_MESSAGE = "Trying to log null object";

    public enum Level {
        ALL, // log in all cases
        DEBUG // log in verbose/debug mode
    }

    private boolean isVerbose = false;

    public void setVerbose() {
        isVerbose = true;
    }

    private static class LoggerHolder {
        // https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getLogger() {
        return LoggerHolder.INSTANCE;
    }

    public void log(Level level, String message) {
        log(level, (Object) message);
    }

    public void log(Level level, Throwable throwable) {
        log(level, (Object) throwable);
    }

    /**
     * Shorthand for {@code log(Logger.Level.DEBUG, message)}.
     * @param message the string to be logged
     */
    public void log(String message) {
        log(Level.DEBUG, (Object) message);
    }

    /**
     * Shorthand for {@code log(Logger.Level.DEBUG, throwable)}.
     * @param throwable the exception/error to be logged
     */
    public void log(Throwable throwable) {
        log(Level.DEBUG, (Object) throwable);
    }

    private void log(Level level, Object o) {
        String s = "";

        if (o == null) {
            s = NULL_OBJECT_MESSAGE;
        } else if (o instanceof Throwable) {
            if (isVerbose || level == Level.ALL) {
                ((Throwable) o).printStackTrace();
            }
            // show gui error dialog? Add shownexttime checkbox?
            return;
        } else {
            s = o.toString();
        }

        if (isVerbose || level == Level.ALL) {
            System.err.println(s);
            // print to some gui console?
        }
    }
}
