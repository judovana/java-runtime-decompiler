package org.jrd.agent;

import org.jrd.agent.api.UnsafeVariables;
import org.jrd.agent.api.Variables;

import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This class contains agent's premain and agentmain methods.
 *
 * @author pmikova
 */
public final class Main {

    public static final String JRD_AGENT_LOADED = "org.jrd.agent.loaded";

    private static final String ADDRESS_STRING = "address:";
    private static final String PORT_STRING = "port:";
    private static String hostname;
    private static Integer port;
    private static boolean firstTime = true;

    static String getHostname() {
        return hostname;
    }

    static Integer getPort() {
        return port;
    }

    private Main() {
    }

    /**
     * Premain method is executed when the agent is loaded. It sets the port and
     * host name from agentArgs and starts the listener thread.
     *
     * @param agentArgs arguments with parameters for listener
     * @param inst      instance of instrumentation of given VM
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        // guard against the agent being loaded twice
        synchronized (Main.class) {
            if (firstTime) {
                firstTime = false;
            } else {
                throw new RuntimeException("Main : attempting to load JRD agent more than once");
            }
        }
        System.setProperty(JRD_AGENT_LOADED, String.valueOf(Integer.parseInt(System.getProperty(JRD_AGENT_LOADED, "0")) + 1));
        Variables.init();
        UnsafeVariables.init();
        Transformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
        InstrumentationProvider p = AccessController.doPrivileged(new PrivilegedAction<InstrumentationProvider>() {
            @Override
            public InstrumentationProvider run() {
                InstrumentationProvider p = new InstrumentationProvider(inst, transformer);
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
        ConnectionDelegator.initialize(hostname, port, p);
    }

    /**
     * This method only calls the premain
     *
     * @param args arguments with parameters for listener
     * @param inst instance of instrumentation of given VM
     */
    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void setFirstTime(boolean b) {
        firstTime = b;
    }
}
