package org.jrd.backend.data;

public class BytemanCompanion {
    private final int bytemanPort;
    private final int postBytemanAgentPort;

    public BytemanCompanion(int bytemanPort, int postBytemanAgentPort) {
        this.bytemanPort = bytemanPort;
        this.postBytemanAgentPort = postBytemanAgentPort;
    }

    public int getBytemanPort() {
        return bytemanPort;
    }

    public int getPostBytemanAgentPort() {
        return postBytemanAgentPort;
    }
}
