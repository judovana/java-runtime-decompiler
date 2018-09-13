/*
 * To change this license header, choose License Headers inputStream Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template inputStream the editor.
 */
package com.redhat.thermostat.decompiler.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class handles the socket accepting and request processing from the
 * decompiler
 *
 * @author pmikova
 */
public class AgentActionWorker extends Thread {

    private Socket socket;
    private InstrumentationProvider provider;
    private Boolean abort = false;

    public AgentActionWorker(Socket socket, InstrumentationProvider provider){
        this.socket = socket;
        this.provider = provider;

        try {
            executeRequest(socket);
        } catch (Exception e) {
            System.err.println("Error when trying to execute the request. Exception: " + e);
            try {
                socket.close();
            } catch (IOException e1) {
                System.err.println("Error when trying to close the socket: " + e1);
                //we can ignore this one too, since we are closing the socket anyway
            }
        }
    }

    private void executeRequest(Socket socket) {
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            System.err.println("Error when opening the input stream of the socket. Exception: " + e);

            try {
                socket.close();
            } catch (IOException e1) {
                System.err.println("Error when closing the socket. Exception: " + e1);
            }
            return;
        }

        OutputStream os = null;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            System.err.println("Error when opening the output stream of the socket. Exception: " + e);

            try {
                socket.close();
            } catch (IOException e1) {
                System.err.println("Error when closing the socket. Exception: " + e1);
            }
            return;
        }
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(is));
        BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(os));

        String line = null;
        try {
            line = inputStream.readLine();
        } catch (IOException e) {
            System.err.println("Exception occurred during reading of the line: " + e);
        }
        try {
            if (null == line) {
                outputStream.write("ERROR\n");
                outputStream.flush();
            } else {
                switch (line) {
                    case "HALT":
                        closeSocket(outputStream);
                        System.out.println("AGENT: Received HALT command, Closing socket and exiting.");
                        break;
                    case "CLASSES":
                        getAllLoadedClasses(inputStream, outputStream);
                        break;
                    case "BYTES":
                        sendByteCode(inputStream, outputStream);
                        break;
                    default:
                        outputStream.write("ERROR\n");
                        outputStream.flush();
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Exception occured while trying to process the request:" + e);

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Exception occured while trying to close the socket:" + e);
            }
        }
    }

    private void getAllLoadedClasses(BufferedReader in, BufferedWriter out) throws IOException {
        out.write("CLASSES");
        out.newLine();
        LinkedBlockingQueue<String> classNames = new LinkedBlockingQueue<String>(1024);
        new Thread(() -> {
            try {
                provider.getClassesNames(classNames, abort);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        while (true){
            String x = classNames.poll();
            if (x == null){
                continue;
            }
            if ("---END---".equals(x)){
                break;
            } else {
                out.write(x);
                out.newLine();
            }
        }
        out.flush();
    }

    private void sendByteCode(BufferedReader in, BufferedWriter out) throws IOException {
        String className = in.readLine();
        if (className == null) {
            out.write("ERROR\n");
            out.flush();
            return;
        }

        try {
            byte[] body = provider.findClassBody(className);
            String encoded = Base64.getEncoder().encodeToString(body);
            out.write("BYTES");
            out.newLine();
            out.write(encoded);
            out.newLine();
        } catch (Exception ex) {
            out.write("ERROR\n");
        }
        out.flush();
    }

    private void closeSocket(BufferedWriter out) throws IOException {
        out.write("GOODBYE");
        out.flush();
        socket.close();
        ConnectionDelegator.gracefulShutdown();
    }

}
