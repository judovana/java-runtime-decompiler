package org.jrd.agent;

import org.jrd.agent.api.UnsafeVariables;
import org.jrd.agent.api.Variables;

import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class contains agent's premain and agentmain methods.
 *
 * @author pmikova
 */
public final class Main {

    public static final String JRD_AGENT_LOADED = "org.jrd.agent.loaded";

    private static final String ADDRESS_STRING = "address:";
    private static final String PORT_STRING = "port:";

    private static final String LONELINESS_STRING = "loneliness:";
    private static final String LONELINESS_VAL_S = "SINGLE_INSTANCE";
    private static final String LONELINESS_VAL_A = "ANONYMOUS";
    private static final String LONELINESS_VAL_F = "FORCING";
    private static final String LONELINESS_VAL_AF = "AF";
    private static int confirmedAttaches = 0;

    private static final Map<String, Pattern> COMPILED_LOADERS_PATTERNS = new HashMap();

    private Main() {
    }

    /**
     * Premain method is executed when the agent is loaded. It sets the port and
     * host name from agentArgs and starts the listener thread.
     *
     * @param agentArgs arguments with parameters for listener
     * @param inst      instance of instrumentation of given VM
     */
    public static void premain(final String agentArgs, Instrumentation inst) throws Exception {
        String hostname = null;
        Integer port = null;
        final String loneliness;
        // guard against the agent being loaded twice
        synchronized (Main.class) {
            loneliness = checkLonelienss(agentArgs);
        }
        System.setProperty(JRD_AGENT_LOADED, String.valueOf(Integer.parseInt(System.getProperty(JRD_AGENT_LOADED, "0")) + 1));
        Variables.init();
        UnsafeVariables.init();
        final String lonelinessCopy = loneliness;
        Transformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
        InstrumentationProvider p = AccessController.doPrivileged(new PrivilegedAction<InstrumentationProvider>() {
            @Override
            public InstrumentationProvider run() {
                InstrumentationProvider p = new InstrumentationProvider(inst, transformer, lonelinessCopy, agentArgs);
                return p;
            }
        });
        if (agentArgs != null) {
            String[] argsArray = agentArgs.split(",");
            for (String arg : argsArray) {
                if (arg.startsWith(ADDRESS_STRING)) {
                    hostname = arg.substring(ADDRESS_STRING.length());

                } else if (arg.startsWith(PORT_STRING)) {
                    try {
                        port = Integer.valueOf(arg.substring(PORT_STRING.length()));
                        if (port <= 0) {
                            AgentLogger.getLogger().log(new RuntimeException("The port value is negative:" + port));
                            port = null;

                        }
                    } catch (Exception e) {
                        AgentLogger.getLogger().log(new RuntimeException("The port value is invalid: " + arg + " . Exception: ", e));
                    }
                }
            }
        }
        ConnectionDelegator.initialize(hostname, port, p, loneliness);
    }

    /**
     * This method only calls the premain
     *
     * @param args arguments with parameters for listener
     * @param inst instance of instrumentation of given VM
     */
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        premain(args, inst);
    }

    public static void deregister(String loneliness, String port) {
        if (loneliness.equals(LONELINESS_VAL_S)) {
            confirmedAttaches--;
            System.err.println("Removed JRD agent: " + loneliness + "/" + port);
        } else if (loneliness.equals(LONELINESS_VAL_A)) {
            System.err.println("Removed JRD agent: " + loneliness + "/" + port);
        } else if (loneliness.equals(LONELINESS_VAL_F)) {
            confirmedAttaches--;
            System.err.println("Removed JRD agent: " + loneliness + "/" + port);
        } else if (loneliness.equals(LONELINESS_VAL_AF)) {
            System.err.println("Removed JRD agent: " + loneliness + "/" + port);
        }
    }

    private static String checkLonelienss(String agentArgs) {
        String loneliness = "SINGLE_INSTANCE";
        if (agentArgs != null) {
            String[] argsArray = agentArgs.split(",");
            for (String arg : argsArray) {
                if (arg.startsWith(LONELINESS_STRING)) {
                    loneliness = arg.substring(LONELINESS_STRING.length());
                }
            }
        }
        if (loneliness.equals(LONELINESS_VAL_S)) {
            if (confirmedAttaches != 0) {
                throw new RuntimeException("Main : attempting to load JRD agent more than once in mode: " + loneliness);
            }
            System.err.println("Added JRD agent: " + loneliness + "/" + agentArgs);
            confirmedAttaches++;
        } else if (loneliness.equals(LONELINESS_VAL_A)) {
            if (confirmedAttaches != 0) {
                throw new RuntimeException("Main : attempting to load JRD agent more than once in mode: " + loneliness);
            }
            System.err.println("Added JRD agent: " + loneliness + "/" + agentArgs);
        } else if (loneliness.equals(LONELINESS_VAL_F)) {
            confirmedAttaches++;
            System.err.println("Added JRD agent: " + loneliness + "/" + agentArgs);
        } else if (loneliness.equals(LONELINESS_VAL_AF)) {
            System.err.println("Added JRD agent: " + loneliness + "/" + agentArgs);
        }
        return loneliness;
    }

    public static boolean equalsOrMatching(ClassLoader candidate, String requested) {
        return equalsOrMatching(AgentLogger.classLoaderId(candidate), requested);
    }

    public static boolean equalsOrMatching(String candidate, String requested) {
        //nullchecks
        if (requested == null || requested.trim().isEmpty()) {
            return true;
        }
        //equals
        if (candidate.equals(requested)) {
            return true;
        }
        //mapchecks
        Pattern compiledClassloader = null;
        if (COMPILED_LOADERS_PATTERNS.containsKey(requested)) {
            //null means, it attempted to compile but failed
            compiledClassloader = COMPILED_LOADERS_PATTERNS.get(requested);
        } else {
            //compile and save to map
            try {
                compiledClassloader = Pattern.compile(requested);
            } catch (Exception ex) {
                //can not compile, that is possible and acceptable
            }
            COMPILED_LOADERS_PATTERNS.put(requested, compiledClassloader);
        }
        //match
        if (compiledClassloader == null) {
            return false;
        } else {
            return compiledClassloader.matcher(candidate).matches();
        }
    }

}
