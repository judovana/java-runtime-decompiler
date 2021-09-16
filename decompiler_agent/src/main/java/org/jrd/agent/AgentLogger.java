package org.jrd.agent;

public class AgentLogger {

    private static final String NULL_OBJECT = "Trying to log null object";



    private static class AgentLoggerHolder {
        // https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        private static final AgentLogger INSTANCE = new AgentLogger();
    }


    public static AgentLogger getLogger() {
        return AgentLoggerHolder.INSTANCE;
    }


    public void log(Object o) {
        String s = "";
        if (o == null) {
            s = NULL_OBJECT;
        } else if (o instanceof Throwable) {
            ((Throwable) o).printStackTrace();
            return;
        } else {
            s = o.toString();
        }
        System.err.println(s);
    }

}
