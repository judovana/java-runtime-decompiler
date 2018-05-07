package com.redhat.thermostat.vm.decompiler.communication;


import com.redhat.thermostat.vm.decompiler.core.AgentAttachManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is handling opening of communication socket and request submitting.
 *  
 */
public class CallDecompilerAgent {
    
    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT= 5395;
    
    private final int port;
    private final String address;
    //private static final Logger logger = LoggingUtils.getLogger(CallDecompilerAgent.class);
    
    /**
     * Constructor of the object
     * @param port port where to open socket
     * @param host socket host
     */
    public CallDecompilerAgent(int port, String host) {
        if (host == null) {
            host = DEFAULT_ADDRESS;
        }

        if (port <= 0) {
            port = DEFAULT_PORT;
        }

        this.address = host;
        this.port = port;
        //logger.log(Level.FINEST, "Port assigned to: " + port + ", host: " + host);
    }

    
    /**
     * Opens a socket and sends the request to the agent via socket. 
     * @param request either "CLASSES" or "BYTES \n className", other formats
     * are refused
     * @return agents response or null
     */
    public String submitRequest(final String request){
        final Communicate comm = new Communicate(this.address, this.port);
        try {
            comm.println(request);
            String results = comm.readResponse();
            return results;
        }catch(IOException ex){
            //logger.log(Level.SEVERE, "Communication with the agent failed,"
                    //+ " could not send a request.");
            return null;
        }finally {
            comm.close();
        }
    
    }
}
