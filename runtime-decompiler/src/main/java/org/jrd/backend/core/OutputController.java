package org.jrd.backend.core;

public class OutputController {

    private static final String NULL_OBJECT = "Trying to log null object";

    public static enum Level {

        MESSAGE_ALL, // - stdout/log in all cases
        MESSAGE_DEBUG, // - stdout/log in verbose/debug mode
    }

    private boolean verbose = false;

    public void setVerbose() {
        verbose = true;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private static class OutputControllerHolder {

        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        private static final OutputController INSTANCE = new OutputController();
    }


    public static OutputController getLogger() {
        return OutputControllerHolder.INSTANCE;
    }

    public void log(Level level, String s) {
        log(level, (Object) s);
    }

    public void log(Level level, Throwable s) {
        log(level, (Object) s);
    }

    public void log(String s) {
        log(Level.MESSAGE_DEBUG, (Object) s);
    }

    public void log(Throwable s) {
        log(Level.MESSAGE_DEBUG, (Object) s);
    }

    private void log(Level level, Object o) {
        String s = "";
        if (o == null) {
            s = NULL_OBJECT;
        } else if (o instanceof Throwable) {
            if (verbose || level == Level.MESSAGE_ALL) {
                ((Throwable) o).printStackTrace();
            }
            //in headfull show errordialog? To disturbing? Add shownexttime checkbox? just notification(soudns best considering the nature of exceptions)?
            return;
        } else {
            s = o.toString();
        }
        if (verbose || level == Level.MESSAGE_ALL) {
            System.out.println(s);
            // in headfull print to some gui console?
        }
    }

}
