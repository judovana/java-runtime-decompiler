//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import org.fife.ui.hex.ByteBuffer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class HexTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private HexEditor editor;
    private ByteBuffer doc;
    private int bytesPerRow;
    private UndoManager undoManager;
    private String[] columnNames;
    private byte[] bitBuf;
    private char[] dumpColBuf;
    private String[] byteStringValues;

    public HexTableModel(final HexEditor editor) {
        this.bitBuf = new byte[16];
        this.editor = editor;
        this.doc = new ByteBuffer(16);
        this.bytesPerRow = 16;
        this.undoManager = new UndoManager();

        this.columnNames = new String[17];
        for (int i = 0; i < 16; ++i) {
            this.columnNames[i] = "+" + Integer.toHexString(i).toUpperCase();
        }
        this.columnNames[16] = "ASCII Dump";

        this.dumpColBuf = new char[16];
        Arrays.fill(dumpColBuf, ' ');

        this.byteStringValues = new String[256];
        for (int i = 0; i < this.byteStringValues.length; ++i) {
            this.byteStringValues[i] = Integer.toHexString(i);
        }
    }

    public byte getByte(final int offset) {
        return this.doc.getByte(offset);
    }

    public int getByteCount() {
        return this.doc.getSize();
    }

    public int getBytesPerRow() {
        return this.bytesPerRow;
    }

    public int getColumnCount() {
        return this.getBytesPerRow() + 1;
    }

    @Override
    public String getColumnName(final int col) {
        return this.columnNames[col];
    }

    public int getRowCount() {
        return this.doc.getSize() / this.bytesPerRow + ((this.doc.getSize() % this.bytesPerRow > 0) ? 1 : 0);
    }

    public Object getValueAt(final int row, final int col) {
        if (col != this.bytesPerRow) {
            final int pos = this.editor.cellToOffset(row, col);
            return (pos == -1) ? "" : this.byteStringValues[this.doc.getByte(pos) & 0xFF];
        }
        final int pos = this.editor.cellToOffset(row, 0);
        if (pos == -1) {
            return "";
        }
        final int count = this.doc.read(pos, this.bitBuf);
        for (int i = 0; i < count; ++i) {
            char ch = (char) this.bitBuf[i];
            if (ch < ' ' || ch > '~') {
                ch = '.';
            }
            this.dumpColBuf[i] = ch;
        }
        return new String(this.dumpColBuf, 0, count);
    }

    public boolean redo() {
        boolean canRedo = this.undoManager.canRedo();
        if (canRedo) {
            this.undoManager.redo();
            canRedo = this.undoManager.canRedo();
        } else {
            UIManager.getLookAndFeel().provideErrorFeedback(this.editor);
        }
        return canRedo;
    }

    public void removeBytes(final int offset, final int len) {
        this.replaceBytes(offset, len, null);
    }

    public void replaceBytes(final int offset, final int len, final byte[] bytes) {
        byte[] removed = null;
        if (len > 0) {
            removed = new byte[len];
            this.doc.remove(offset, len, removed);
        }
        byte[] added = null;
        if (bytes != null && bytes.length > 0) {
            this.doc.insertBytes(offset, bytes);
            added = bytes.clone();
        }
        if (removed != null || added != null) {
            this.undoManager.addEdit(new BytesReplacedUndoableEdit(offset, removed, added));
            this.fireTableDataChanged();
            final int addCount = (added == null) ? 0 : added.length;
            final int remCount = (removed == null) ? 0 : removed.length;
            this.editor.fireHexEditorEvent(offset, addCount, remCount);
        }
    }

    public void setBytes(final String fileName) throws IOException {
        this.doc = new ByteBuffer(fileName);
        this.undoManager.discardAllEdits();
        this.fireTableDataChanged();
        this.editor.fireHexEditorEvent(0, this.doc.getSize(), 0);
    }

    public void setBytes(final InputStream in) throws IOException {
        this.doc = new ByteBuffer(in);
        this.undoManager.discardAllEdits();
        this.fireTableDataChanged();
        this.editor.fireHexEditorEvent(0, this.doc.getSize(), 0);
    }

    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        final byte b = (byte) Integer.parseInt((String) value, 16);
        final int offset = this.editor.cellToOffset(row, col);
        if (offset > -1) {
            final byte old = this.doc.getByte(offset);
            if (old == b) {
                return;
            }
            this.doc.setByte(offset, b);
            this.undoManager.addEdit(new ByteChangedUndoableEdit(offset, old, b));
            this.fireTableCellUpdated(row, col);
            this.fireTableCellUpdated(row, this.bytesPerRow);
            this.editor.fireHexEditorEvent(offset, 1, 1);
        }
    }

    public boolean undo() {
        boolean canUndo = this.undoManager.canUndo();
        if (canUndo) {
            this.undoManager.undo();
            canUndo = this.undoManager.canUndo();
        } else {
            UIManager.getLookAndFeel().provideErrorFeedback(this.editor);
        }
        return canUndo;
    }

    private class ByteChangedUndoableEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        private int offs;
        private byte oldVal;
        private byte newVal;

        ByteChangedUndoableEdit(final int offs, final byte oldVal, final byte newVal) {
            this.offs = offs;
            this.oldVal = oldVal;
            this.newVal = newVal;
        }

        @Override
        public void undo() {
            super.undo();
            if (HexTableModel.this.getByteCount() < this.offs) {
                throw new CannotUndoException();
            }
            this.setValueImpl(this.offs, this.oldVal);
        }

        @Override
        public void redo() {
            super.redo();
            if (HexTableModel.this.getByteCount() < this.offs) {
                throw new CannotRedoException();
            }
            this.setValueImpl(this.offs, this.newVal);
        }

        private void setValueImpl(final int offset, final byte val) {
            HexTableModel.this.editor.setSelectedRange(offset, offset);
            HexTableModel.this.doc.setByte(offset, val);
            final Point p = HexTableModel.this.editor.offsetToCell(offset);
            HexTableModel.this.fireTableCellUpdated(p.x, p.y);
            HexTableModel.this.fireTableCellUpdated(p.x, HexTableModel.this.bytesPerRow);
            HexTableModel.this.editor.fireHexEditorEvent(offset, 1, 1);
        }
    }

    private class BytesReplacedUndoableEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        private int offs;
        private byte[] removed;
        private byte[] added;

        BytesReplacedUndoableEdit(final int offs, final byte[] removed, final byte[] added) {
            this.offs = offs;
            this.removed = removed;
            this.added = added;
        }

        @Override
        public void undo() {
            super.undo();
            if (HexTableModel.this.getByteCount() < this.offs) {
                throw new CannotUndoException();
            }
            this.removeAndAdd(this.added, this.removed);
        }

        @Override
        public void redo() {
            super.redo();
            if (HexTableModel.this.getByteCount() < this.offs) {
                throw new CannotRedoException();
            }
            this.removeAndAdd(this.removed, this.added);
        }

        private void removeAndAdd(final byte[] toRemove, final byte[] toAdd) {
            final int remCount = (toRemove == null) ? 0 : toRemove.length;
            final int addCount = (toAdd == null) ? 0 : toAdd.length;
            HexTableModel.this.doc.remove(this.offs, remCount);
            HexTableModel.this.doc.insertBytes(this.offs, toAdd);
            HexTableModel.this.fireTableDataChanged();
            int endOffs = this.offs;
            if (toAdd != null && toAdd.length > 0) {
                endOffs += toAdd.length - 1;
            }
            HexTableModel.this.editor.setSelectedRange(this.offs, endOffs);
            HexTableModel.this.editor.fireHexEditorEvent(this.offs, addCount, remCount);
        }
    }

    public byte[] getDoc() {
        return doc.getBuffer();
    }
}
