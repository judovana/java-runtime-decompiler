package com.redhat.thermostat.vm.decompiler.communication;


import com.redhat.thermostat.vm.decompiler.core.AgentAttachManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class opens a socket and contain methods for read and write to socket
 * IS/OS.
 */
public class Communicate {

    private Socket commSocket;
    private BufferedReader commInput;
    private BufferedWriter commOutput;

    public static final String DEFAULT_ADDRESS = "localhost";
    //private static final Logger logger = LoggingUtils.getLogger(Communicate.class);

    /**
     * Constructor creates a socket on given port and saves the streams into
     * class variables.
     *
     * @param host host name
     * @param port port where we open the socket
     */
    public Communicate(String host, int port) {

        try {
            this.commSocket = new Socket(host, port);
        } catch (IOException ex) {
            System.out.println("Error creating socket");
            ex.printStackTrace();
            //Logger.getLogger(Communicate.class.getName()).log(Level.SEVERE, null, ex);
        }
        InputStream is;
        try {
            is = this.commSocket.getInputStream();
        } catch (IOException e) {
            System.out.println("Error getting stream");
            //logger.log(Level.SEVERE, "Opening of input stream of a socket "
                   // + "failed: " + e.getMessage());
            try {
                this.commSocket.close();
            } catch (IOException e1) {
                System.out.println("Error when closing the stream");
                //logger.log(Level.WARNING, "Error while closing the socket: "
                        //+ e1.getMessage());

            }
            return;
        }

        OutputStream os;
        try {
            os = this.commSocket.getOutputStream();
        } catch (IOException e) {
            //logger.log(Level.SEVERE, "Opening of output stream of a socket "
                    //+ "failed: " + e.getMessage());
            try {
                this.commSocket.close();
            } catch (IOException e1) {
                //logger.log(Level.WARNING, "Error while closing the socket: "
                        //+ "" + e1.getMessage());
            }
            return;
        }

        this.commInput = new BufferedReader(new InputStreamReader(is));
        this.commOutput = new BufferedWriter(new OutputStreamWriter(os));

        return;
    }

    /**
     * Closes a socket.
     */
    public void close() {
        try {
            this.commSocket.close(); // also closes the in/out streams
        } catch (IOException e) {
            //logger.log(Level.WARNING, "Error while closing the socket: "
                    //+ "" + e.getMessage());

        } finally {
            this.commSocket = null;
            this.commInput = null;
            this.commOutput = null;
        }
    }

    /**
     * Method that reads agent's response.
     * @return "ERROR" in case of fail or corresponding bytes or class names
     */
    public String readResponse(){
        String initLine;
        try {
            initLine = this.commInput.readLine().trim();
        } catch (IOException ex) {
            //logger.log(Level.SEVERE, "Agent did not return anything.");
            return "ERROR";
        }
        
        if (initLine.equals("ERROR")) {
            //logger.log(Level.SEVERE, "Agent returned error.");
            return "ERROR";
        } 
        
        else if (initLine.equals("BYTES")) {
            try {
                String s = this.commInput.readLine();
                s = s.trim();
                //logger.log(Level.FINE, "Agent returned bytes.");
                return s;
            } catch (IOException ex) {
                //logger.log(Level.WARNING, "Can not read line, check "
                        //+ "agent communication output.");
            }

        } 
        
        else if (initLine.equals("CLASSES")) {
            StringBuilder str = new StringBuilder();
            while (true) {
                try {
                    String s = this.commInput.readLine();
                    if (s == null) {
                        break;
                    }
                    s = s.trim();
                    if (!s.isEmpty()) {
                        str.append(s).append(";");
                    }
                } catch (IOException ex) {
                    //logger.log(Level.WARNING, "Can not read line, check "
                        //+ "agent communication output.");
                }
            }
            //logger.log(Level.FINE, "Agent returned class names.");
            return str.toString();

            // Agent shutdown response
        } else if (initLine.equals("GOODBYE")) {
            return "OK";
        }
        
        //logger.log(Level.SEVERE, "Unknow header of " + initLine);
        return "ERROR";
    }

    /**
     * Sends a line with request to agent.
     * @param line "CLASSES" or "BYTES className"
     * @throws IOException if the write operation fails
     */
    public void println(String line) throws IOException {
        this.commOutput.write(line);
        this.commOutput.newLine();
        this.commOutput.flush();
    }

}
