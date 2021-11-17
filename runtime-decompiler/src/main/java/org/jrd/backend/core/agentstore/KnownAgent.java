package org.jrd.backend.core.agentstore;

import com.google.gson.Gson;
import org.jrd.backend.communication.InstallDecompilerAgentImpl;
import org.jrd.backend.core.Logger;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

public class KnownAgent {

    private final int port;
    private final int pid;
    private final String host;
    private final long owner; //to close only my connections on exit

    private final AgentLiveliness ttl;

    @SuppressWarnings("ExplicitInitialization") //the null have its meaning here
    private Long deadSince = null;

    KnownAgent(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
        this.pid = Integer.parseInt(install.getPid());
        this.port = install.getPort();
        this.host = install.getHost();
        this.ttl = ttl;
        this.owner = ProcessHandle.current().pid();
    }

    public boolean isLive() {
        return deadSince == null;
    }

    public int getPid() {
        return pid;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public AgentLiveliness getLiveliness() {
        return ttl;
    }

    public long getOwner() {
        return owner;
    }

    public boolean matches(String hostname, int listenPort, int vmPid) {
        return this.host.equals(hostname) && this.port == listenPort && this.pid == vmPid;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public boolean verify() {
        Socket socket = null;
        try {
            socket = SocketFactory.getDefault().createSocket(host, port);
            socket.setSoTimeout(5000); //if buffered reader do not get newline
            if (!socket.isConnected()) {
                throw new RuntimeException("connection to " + host + ":" + port + " failed");
            }
            Logger.getLogger().log(" restoring agent verified on : " + host + ":" + port);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            //TODO, repalce with handshake
            out.write("BLAH\n");
            out.flush();
            String reply = in.readLine();
            if ("ERROR Agent received unknown command: 'BLAH'.".equals(reply)) {
                Logger.getLogger().log(" restored agent verified on : " + host + ":" + port);
                deadSince = null;
                return true;
            } else {
                throw new RuntimeException(host + ":" + port + " is not our agent. Returned unexpected: " + reply);
            }
        } catch (Exception ex) {
            Logger.getLogger().log(ex);
            Logger.getLogger().log(" removing unresponsive agent: " + host + ":" + port);
            deadSince = new Date().getTime();
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception eex) {
                    Logger.getLogger().log(eex);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnownAgent that = (KnownAgent) o;
        return port == that.port && pid == that.pid && owner == that.owner && Objects.equals(host, that.host) && ttl == that.ttl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, pid, host, owner, ttl);
    }

    public void markKilled() {
        deadSince = new Date().getTime();
    }
}
