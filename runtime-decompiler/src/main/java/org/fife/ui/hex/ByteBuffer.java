//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ByteBuffer {
    private byte[] buffer;

    public ByteBuffer(final int size) {
        this.buffer = new byte[size];
    }

    public ByteBuffer(final String file) throws IOException {
        this(new File(file));
    }

    public ByteBuffer(final File file) throws IOException {
        final int size = (int) file.length();
        if (size < 0) {
            throw new IOException("Negative file length: " + size);
        }
        this.buffer = new byte[size];
        if (size > 0) {
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            int pos = 0;
            int count = 0;
            try {
                while (pos < this.buffer.length && (count = in.read(this.buffer, pos, this.buffer.length - pos)) > -1) {
                    pos += count;
                }
            } finally {
                in.close();
            }
        }
    }

    public ByteBuffer(final InputStream in) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.buffer = new byte[4096];
        int count = 0;
        while ((count = in.read(this.buffer, 0, this.buffer.length)) > -1) {
            baos.write(this.buffer, 0, count);
        }
        this.buffer = baos.toByteArray();
    }

    public byte getByte(final int offset) {
        return this.buffer[offset];
    }

    public int getSize() {
        return this.buffer.length;
    }

    public void insertByte(final int offset, final byte b) {
        final byte[] buf2 = new byte[this.buffer.length + 1];
        System.arraycopy(this.buffer, 0, buf2, 0, offset);
        buf2[offset] = b;
        System.arraycopy(this.buffer, offset, buf2, offset + 1, this.buffer.length - offset);
        this.buffer = buf2;
    }

    public void insertBytes(final int offs, final byte[] b) {
        if (b == null || b.length == 0) {
            return;
        }
        final byte[] buf2 = new byte[this.buffer.length + b.length];
        System.arraycopy(this.buffer, 0, buf2, 0, offs);
        System.arraycopy(b, 0, buf2, offs, b.length);
        System.arraycopy(this.buffer, offs, buf2, offs + b.length, this.buffer.length - offs);
        this.buffer = buf2;
    }

    public int read(final int offset, final byte[] buf) {
        if (buf == null) {
            return -1;
        }
        final int count = Math.min(buf.length, this.getSize() - offset);
        System.arraycopy(this.buffer, offset, buf, 0, count);
        return count;
    }

    public void remove(final int offset, final int len) {
        this.remove(offset, len, null);
    }

    public void remove(final int offset, final int len, final byte[] removed) {
        if (removed != null) {
            System.arraycopy(this.buffer, offset, removed, 0, len);
        }
        final byte[] buf = new byte[this.buffer.length - len];
        System.arraycopy(this.buffer, 0, buf, 0, offset);
        System.arraycopy(this.buffer, offset + len, buf, offset, buf.length - offset);
        this.buffer = buf;
    }

    public void setByte(final int offset, final byte b) {
        this.buffer[offset] = b;
    }

    public byte[] getBuffer() {
        return Arrays.copyOf(buffer, buffer.length);
    }
}
