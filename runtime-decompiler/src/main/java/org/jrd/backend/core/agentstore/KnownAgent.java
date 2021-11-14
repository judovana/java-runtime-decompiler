package org.jrd.backend.core.agentstore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jrd.backend.communication.InstallDecompilerAgentImpl;

class KnownAgent {

    private final int port;
    private final int pid;
    private final String host;
    private final long owner;

    private boolean live;
    private final AgentLiveliness ttl;
    private boolean verified;

    KnownAgent(InstallDecompilerAgentImpl install, AgentLiveliness ttl) {
        this.pid = Integer.parseInt(install.getPid());
        this.port = install.getPort();
        this.host = install.getHost();
        this.ttl = ttl;
        this.live = true;
        this.verified = true;
        this.owner = ProcessHandle.current().pid();
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public boolean matches(String hostname, int listenPort, int vmPid) {
        return this.host.equals(hostname) && this.port == listenPort && this.pid == vmPid;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        return json;
    }
}
