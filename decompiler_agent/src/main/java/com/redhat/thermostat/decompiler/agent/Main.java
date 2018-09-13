package com.redhat.thermostat.decompiler.agent;

import java.lang.instrument.Instrumentation;

/**
 * This class contains agent's premain and agentmain methods.
 *
 * @author pmikova
 */
public class Main {

    private static final String ADRESS_STRING = "address:";
    private static final String PORT_STRING = "port:";
    private static String hostname;
    private static Integer port;

    /**
     * Premain method is executed when the agent is loaded. It sets the port and
     * host name from agentArgs and starts the listener thread.
     *
     * @param agentArgs arguments with parameters for listener
     * @param inst instance of instrumentation of given VM
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        Transformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
        InstrumentationProvider p = new InstrumentationProvider(inst, transformer);

        if (agentArgs != null) {
            String[] argsArray = agentArgs.split(",");
            for (String arg : argsArray) {
                if (arg.startsWith(ADRESS_STRING)) {
                    hostname = arg.substring(ADRESS_STRING.length(), arg.length());

                } else if (arg.startsWith(PORT_STRING)) {
                    try {
                        port = Integer.valueOf(arg.substring(PORT_STRING.length(), arg.length()));
                        if (port <= 0) {
                            System.err.println("The port value is negative:" + port);
                            port = null;

                        }
                    } catch (Exception e) {
                        System.err.println("The port value is invalid: " + arg + " . Exception: " + e);
                    }
                }
            }
        }

        boolean start = ConnectionDelegator.initialize(hostname, port, p);

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

}
