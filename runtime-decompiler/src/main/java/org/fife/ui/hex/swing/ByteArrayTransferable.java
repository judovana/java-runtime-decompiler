//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class ByteArrayTransferable implements Transferable {
    private static final DataFlavor[] FLAVORS;

    static {
        FLAVORS = new DataFlavor[]{DataFlavor.stringFlavor, DataFlavor.plainTextFlavor};
    }

    private int offset;
    private byte[] bytes;

    public ByteArrayTransferable(final int offset, final byte[] bytes) {
        this.offset = offset;
        if (bytes != null) {
            this.bytes = bytes.clone();
        } else {
            this.bytes = new byte[0];
        }
    }

    public int getLength() {
        return this.bytes.length;
    }

    public int getOffset() {
        return this.offset;
    }

    public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(ByteArrayTransferable.FLAVORS[0])) {
            return new String(this.bytes, StandardCharsets.UTF_8);
        }
        if (flavor.equals(ByteArrayTransferable.FLAVORS[1])) {
            return new StringReader(new String(this.bytes, StandardCharsets.UTF_8));
        }
        throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return ByteArrayTransferable.FLAVORS.clone();
    }

    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        for (int i = 0; i < ByteArrayTransferable.FLAVORS.length; ++i) {
            if (flavor.equals(ByteArrayTransferable.FLAVORS[i])) {
                return true;
            }
        }
        return false;
    }
}
