package org.jrd.backend.communication;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * This class opens a socket and contain methods for read and write to socket
 * IS/OS.
 */
public class Communicate {

    public static final String NO_VALUE_OK_RESULT = "OK";
    public static final String NO_VALLUE_DONE_RESULT = "DONE";

    private Socket commSocket;
    private BufferedReader commInput;
    private BufferedWriter commOutput;

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
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
        InputStream is;
        try {
            is = this.commSocket.getInputStream();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
            try {
                this.commSocket.close();
            } catch (IOException e1) {
                Logger.getLogger().log(Logger.Level.ALL, e1);
            }
            return;
        }

        OutputStream os;
        try {
            os = this.commSocket.getOutputStream();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.DEBUG, e);
            try {
                this.commSocket.close();
            } catch (IOException e1) {
                Logger.getLogger().log(Logger.Level.DEBUG, e1);
            }
            return;
        }

        this.commInput = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        this.commOutput = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }

    /**
     * Closes a socket.
     */
    public void close() {
        try {
            this.commSocket.close(); // also closes the in/out streams
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.DEBUG, e);
        } finally {
            this.commSocket = null;
            this.commInput = null;
            this.commOutput = null;
        }
    }

    private String trimReadLine() throws IOException {
        String line = this.commInput.readLine();

        if (line == null) {
            String message = "Agent returned null response.";

            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException(message));
            return ErrorCandidate.toError(message);
        }

        return line.trim();
    }

    /**
     * Method that reads agent's response.
     * @return "ERROR" in case of fail or corresponding bytes or class names
     */
    @SuppressWarnings("ReturnCount") // returns in switch cases
    public String readResponse() {
        String initLine;

        // read header
        try {
            initLine = trimReadLine();
        } catch (IOException ex) {
            Logger.getLogger().log(Logger.Level.DEBUG, ex);
            return ErrorCandidate.toError(ex);
        }

        // parse body based on header
        ErrorCandidate errorCandidate = new ErrorCandidate(initLine);
        if (errorCandidate.isError()) {
            Logger.getLogger().log(
                    Logger.Level.ALL, new RuntimeException("Agent returned error in response header: " + errorCandidate.getErrorMessage())
            );
            return initLine;
        }
        //non executive commands. Move to enum?
        switch (initLine) {
            case "GOODBYE":
                Logger.getLogger().log(Logger.Level.DEBUG, "Agent closed socket when halting.");
                return NO_VALUE_OK_RESULT;
            case NO_VALLUE_DONE_RESULT:
                Logger.getLogger().log(Logger.Level.DEBUG, "Agent successfully overwrote class.");
                return NO_VALUE_OK_RESULT;
            default: //this is ok, it jsut feeding of idiotic codestyle
        }
        switch (AgentRequestAction.RequestAction.fromString(initLine)) {
            case VERSION:
            case BYTES:
                try {
                    String bytes = trimReadLine();
                    Logger.getLogger().log(Logger.Level.DEBUG, "Agent returned bytes: " + bytes);
                    return bytes;
                } catch (IOException ex) {
                    Logger.getLogger().log(Logger.Level.ALL, ex);
                    return ErrorCandidate.toError(ex);
                }
            case SEARCH_CLASSES:
            case OVERRIDES:
            case CLASSES:
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
                        Logger.getLogger().log(Logger.Level.ALL, ex);
                    }
                }
                Logger.getLogger().log(Logger.Level.DEBUG, "Agent successfully returned class names or overrides");
                return str.toString();
            default:
                String message = "Unknown agent response header: '" + initLine + "'.";
                Logger.getLogger().log(Logger.Level.ALL, message);
                return ErrorCandidate.toError(message);
        }
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
