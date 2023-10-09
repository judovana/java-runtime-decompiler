package org.jrd.backend.data;

import java.io.Serializable;

public class BytemanCompanion implements Serializable {
    private final int bytemanPort;
    private final String bytemanHost;
    private final int postBytemanAgentPort;
    private final String postBytemanAgentHost;

    public BytemanCompanion(String bytemanHost, int bytemanPort, String postBytemanAgentHost, int postBytemanAgentPort) {
        this.bytemanHost = bytemanHost;
        this.bytemanPort = bytemanPort;
        this.postBytemanAgentHost = postBytemanAgentHost;
        this.postBytemanAgentPort = postBytemanAgentPort;
    }

    public int getBytemanPort() {
        return bytemanPort;
    }

    public int getPostBytemanAgentPort() {
        return postBytemanAgentPort;
    }

    public String getBytemanHost() {
        return bytemanHost;
    }

    public String getPostBytemanAgentHost() {
        return postBytemanAgentHost;
    }
}
