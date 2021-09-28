package org.jrd.frontend.utility;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;


/**
 * Behaves like the 'tee' command, sends output to both actual std stream and a
 * log
 */
public final class TeeOutputStream extends PrintStream {

    // Everything written to TeeOutputStream is written to this buffer too
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    public TeeOutputStream(PrintStream stdStream) {
        super(stdStream, false, StandardCharsets.UTF_8);
    }

    /*
     * The big ones: these do the actual writing
     */

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        if (len == 0) {
            return;
        }
        byteArrayOutputStream.write(b, off, len);
        super.write(b, off, len);
    }

    @Override
    public synchronized void write(int b) {
        byteArrayOutputStream.write(b);
        super.write(b);
    }

    public byte[] getByteArray() {
        return byteArrayOutputStream.toByteArray();
    }
}
