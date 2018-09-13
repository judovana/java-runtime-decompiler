package com.redhat.thermostat.decompiler.agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionDelegator extends Thread{

    private static ConnectionDelegator connectionDelegator;
    public static final int DEFAULT_PORT = 5395;
    public static final String DEFAULT_ADRESS = "localhost";

    private ServerSocket theServerSocket;
    private InstrumentationProvider provider;
    private static boolean running;
    private static String addressGiven;
    private static Integer portGiven;

    private ConnectionDelegator(InstrumentationProvider provider, ServerSocket serverSocket) {
        this.provider = provider;
        this.theServerSocket = serverSocket;
        setDaemon(true);
    }

    /**
     * This method is used to create an ConnectionDelegator object and start
     * listener thread
     *
     * @param hostname host name to open communication with
     * @param port on which open socket
     * @param provider this is where instrumentation and transformer objects are
     * stored
     *
     * @return boolean true if ran correctly, else false
     */
    public static synchronized boolean initialize(String hostname, Integer port,
                                                  InstrumentationProvider provider) {
        ConnectionDelegator.addressGiven = hostname;
        portGiven = port;
        ServerSocket initServerSocket = null;
        try {
            if (port == null) {
                port = DEFAULT_PORT;
            }
            if (hostname == null) {
                hostname = DEFAULT_ADRESS;
            }
            initServerSocket = new ServerSocket();
            initServerSocket.bind(new InetSocketAddress(hostname, port));
        } catch (IOException e) {
            System.err.println("Exception occurred when opening the socket: "
                    + e);
            return false;
        }

        connectionDelegator = new ConnectionDelegator(provider, initServerSocket);
        connectionDelegator.start();
        return true;
    }

    /**
     * Waits for new connection.
     * When client connects starts new worker thread and delegates connection to it
     */
    @Override
    public void run() {
        running = true;
        while (running) {
            if (theServerSocket.isClosed()) {
                return;
            }
            Socket clientSocket = null;
            try {
                clientSocket = theServerSocket.accept();
            } catch (IOException e) {
                if (!theServerSocket.isClosed()) {
                    System.err.println("The server socket is closed, killing the thread.");
                }
                return;
            }
            new Thread(
                    new AgentActionWorker(clientSocket, provider))
                    .start();
        }

        if (!theServerSocket.isClosed()){
            try {
                theServerSocket.close();
            } catch (IOException e) {
                System.err.print("Error when closing the server socket" + e);
            }
        }
    }

    /**
     * Closes server socket
     * Already connected clients can finish their work but no new clients can connect.
     */
    public static void gracefulShutdown(){
        if (/*Agent was created by client*/true){
            running = false;
        }
    }

}
