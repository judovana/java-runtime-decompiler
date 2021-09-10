package org.jrd.backend.core;

public class Logger {

    private static final String NULL_OBJECT = "Trying to log null object";

    public enum Level {
        ALL, // log in all cases
        DEBUG // log in verbose/debug mode
    }

    private boolean verbose = false;

    public void setVerbose() {
        verbose = true;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private static class LoggerHolder {
        // https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getLogger() {
        return LoggerHolder.INSTANCE;
    }

    public void log(Level level, String s) {
        log(level, (Object) s);
    }

    public void log(Level level, Throwable s) {
        log(level, (Object) s);
    }

    public void log(String s) {
        log(Level.DEBUG, (Object) s);
    }

    public void log(Throwable s) {
        log(Level.DEBUG, (Object) s);
    }

    private void log(Level level, Object o) {
        String s = "";
        if (o == null) {
            s = NULL_OBJECT;
        } else if (o instanceof Throwable) {
            if (verbose || level == Level.ALL) {
                ((Throwable) o).printStackTrace();
            }
            // show gui error dialog? To disturbing? Add shownexttime checkbox? just notification(sounds best considering the nature of exceptions)?
            return;
        } else {
            s = o.toString();
        }
        if (verbose || level == Level.ALL) {
            System.err.println(s);
            // print to some gui console?
        }
    }
}
