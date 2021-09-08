//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class HexEditorTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean canImport(final JComponent comp, final DataFlavor[] flavors) {
        final HexEditor editor = (HexEditor) comp;
        return editor.isEnabled() && this.getImportFlavor(flavors, editor) != null;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        final HexEditor e = (HexEditor) c;
        final int start = e.getSmallestSelectionIndex();
        final int end = e.getLargestSelectionIndex();
        final byte[] array = new byte[end - start + 1];
        for (int i = end; i >= start; --i) {
            array[i - start] = e.getByte(i);
        }
        final ByteArrayTransferable bat = new ByteArrayTransferable(start, array);
        return bat;
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        if (action == 2) {
            final ByteArrayTransferable bat = (ByteArrayTransferable) data;
            final int offs = bat.getOffset();
            final HexEditor e = (HexEditor) source;
            e.removeBytes(offs, bat.getLength());
        }
    }

    private DataFlavor getImportFlavor(final DataFlavor[] flavors, final HexEditor e) {
        for (int i = 0; i < flavors.length; ++i) {
            if (flavors[i].equals(DataFlavor.stringFlavor)) {
                return flavors[i];
            }
        }
        return null;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        final HexEditor e = (HexEditor) c;
        return e.isEnabled() ? 3 : 1;
    }

    @Override
    public boolean importData(final JComponent c, final Transferable t) {
        final HexEditor e = (HexEditor) c;
        final boolean imported = false;
        final DataFlavor flavor = this.getImportFlavor(t.getTransferDataFlavors(), e);
        if (flavor != null) {
            try {
                final Object data = t.getTransferData(flavor);
                if (flavor.equals(DataFlavor.stringFlavor)) {
                    final String text = (String) data;
                    final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    e.replaceSelection(bytes);
                }
            } catch (UnsupportedFlavorException ufe) {
                ufe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return imported;
    }
}
