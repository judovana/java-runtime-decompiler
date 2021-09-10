//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import org.fife.ui.hex.event.HexEditorEvent;
import org.fife.ui.hex.event.HexEditorListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;

public class HexEditor extends JScrollPane {
    private static final long serialVersionUID = 1L;
    public static final String PROPERTY_ALTERNATE_COLUMN_BG = "alternateColBG";
    public static final String PROPERTY_ALTERNATE_ROW_BG = "alternateRowBG";
    public static final String PROPERTY_ASCII_DUMP_HIGHLIGHT_COLOR = "asciiDumpHighlightColor";
    public static final String PROPERTY_HIGHLIGHT_ASCII_DUMP = "highlightAsciiDump";
    public static final String PROPERTY_SHOW_GRID = "showGrid";
    private HexTable table;
    private boolean alternateRowBackground;
    private boolean alternateColumnBackground;
    private boolean highlightSelectionInAsciiDump;
    private Color highlightSelectionInAsciiDumpColor;
    private static final TransferHandler DEFAULT_TRANSFER_HANDLER;
    static final int DUMP_COLUMN_WIDTH = 200;

    public HexEditor() {
        final HexTableModel model = new HexTableModel(this);
        this.table = new HexTable(this, model);

        this.setViewportView(table);
        this.setShowRowHeader(true);
        this.setAlternateRowBackground(false);
        this.setAlternateColumnBackground(false);
        this.setHighlightSelectionInAsciiDump(true);
        this.setHighlightSelectionInAsciiDumpColor(new Color(255, 255, 192));
        this.setTransferHandler(HexEditor.DEFAULT_TRANSFER_HANDLER);
        this.setVerticalScrollBarPolicy(20);
        this.setHorizontalScrollBarPolicy(30);
    }

    public void addHexEditorListener(final HexEditorListener l) {
        this.listenerList.add(HexEditorListener.class, l);
    }

    public int cellToOffset(final int row, final int col) {
        return this.table.cellToOffset(row, col);
    }

    public void copy() {
        this.invokeAction(TransferHandler.getCopyAction());
    }

    public void cut() {
        this.invokeAction(TransferHandler.getCutAction());
    }

    public void delete() {
        if (this.table.leadSelectionIndex == -1 || this.table.anchorSelectionIndex == -1) {
            UIManager.getLookAndFeel().provideErrorFeedback(this.table);
            return;
        }
        final int start = this.table.getSmallestSelectionIndex();
        final int end = this.table.getLargestSelectionIndex();
        final int len = end - start + 1;
        this.removeBytes(start, len);
    }

    protected void fireHexEditorEvent(final int offset, final int added, final int removed) {
        HexEditorEvent e = null;
        final Object[] listeners = this.listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == HexEditorListener.class) {
                if (e == null) {
                    e = new HexEditorEvent(this, offset, added, removed);
                }
                ((HexEditorListener) listeners[i + 1]).hexBytesChanged(e);
            }
        }
    }

    public boolean getAlternateColumnBackground() {
        return this.alternateColumnBackground;
    }

    public boolean getAlternateRowBackground() {
        return this.alternateRowBackground;
    }

    public byte getByte(final int offset) {
        return this.table.getByte(offset);
    }

    public int getByteCount() {
        return this.table.getByteCount();
    }

    public boolean getHighlightSelectionInAsciiDump() {
        return this.highlightSelectionInAsciiDump;
    }

    public Color getHighlightSelectionInAsciiDumpColor() {
        return this.highlightSelectionInAsciiDumpColor;
    }

    public int getLargestSelectionIndex() {
        return this.table.getLargestSelectionIndex();
    }

    public int getSmallestSelectionIndex() {
        return this.table.getSmallestSelectionIndex();
    }

    HexTable getTable() {
        return this.table;
    }

    private void invokeAction(final Action a) {
        a.actionPerformed(new ActionEvent(
                this, 1001, (String) a.getValue("Name"), EventQueue.getMostRecentEventTime(), 0
        ));
    }

    public Point offsetToCell(final int offset) {
        return this.table.offsetToCell(offset);
    }

    public byte[] get() {
        return table.getDocBuffer();
    }

    public void open(final InputStream in) throws IOException {
        this.table.open(in);
    }

    public void paste() {
        this.invokeAction(TransferHandler.getPasteAction());
    }

    public boolean redo() {
        return this.table.redo();
    }

    public void removeBytes(final int offs, final int len) {
        this.table.removeBytes(offs, len);
        this.table.changeSelectionByOffset(offs, false);
    }

    public void removeHexEditorListener(final HexEditorListener l) {
        this.listenerList.remove(HexEditorListener.class, l);
    }

    public void replaceBytes(final int offset, int len, final byte[] bytes) {
        if (len == 1) {
            len = 0;
        }
        this.table.replaceBytes(offset, len, bytes);
        this.table.changeSelectionByOffset(this.table.anchorSelectionIndex, false);
        final int count = (bytes == null) ? 0 : bytes.length;
        this.table.setSelectionByOffsets(offset, offset + count - 1);
    }

    public void replaceSelection(final byte[] bytes) {
        final int offset = this.table.getSmallestSelectionIndex();
        final int len = this.table.getLargestSelectionIndex() - offset + 1;
        this.replaceBytes(offset, len, bytes);
    }

    public void setAlternateColumnBackground(final boolean alternate) {
        if (alternate != this.alternateColumnBackground) {
            this.alternateColumnBackground = alternate;
            this.table.repaint();
            this.firePropertyChange(PROPERTY_ALTERNATE_COLUMN_BG, !alternate, alternate);
        }
    }

    public void setAlternateRowBackground(final boolean alternate) {
        if (alternate != this.alternateRowBackground) {
            this.alternateRowBackground = alternate;
            this.table.repaint();
            this.firePropertyChange(PROPERTY_ALTERNATE_ROW_BG, !alternate, alternate);
        }
    }

    public void setHighlightSelectionInAsciiDump(final boolean highlight) {
        if (highlight != this.highlightSelectionInAsciiDump) {
            this.highlightSelectionInAsciiDump = highlight;
            this.table.repaint();
            this.firePropertyChange(PROPERTY_HIGHLIGHT_ASCII_DUMP, !highlight, highlight);
        }
    }

    public void setHighlightSelectionInAsciiDumpColor(final Color c) {
        if (c != null && !c.equals(this.highlightSelectionInAsciiDumpColor)) {
            final Color old = this.highlightSelectionInAsciiDumpColor;
            this.highlightSelectionInAsciiDumpColor = c;
            this.table.repaint();
            this.firePropertyChange(PROPERTY_ASCII_DUMP_HIGHLIGHT_COLOR, old, c);
        }
    }

    public void setSelectedRange(final int startOffs, final int endOffs) {
        this.table.setSelectionByOffsets(startOffs, endOffs);
    }

    public void setShowColumnHeader(final boolean show) {
        this.setColumnHeaderView(show ? this.table.getTableHeader() : null);
    }

    public void setShowGrid(final boolean show) {
        if (show != this.table.getShowHorizontalLines()) {
            this.table.setShowGrid(show);
            this.firePropertyChange(PROPERTY_SHOW_GRID, !show, show);
        }
    }

    public void setShowRowHeader(final boolean show) {
        this.setRowHeaderView(show ? new HexEditorRowHeader(this.table) : null);
    }

    public boolean undo() {
        return this.table.undo();
    }

    static {
        DEFAULT_TRANSFER_HANDLER = new HexEditorTransferHandler();
    }
}
