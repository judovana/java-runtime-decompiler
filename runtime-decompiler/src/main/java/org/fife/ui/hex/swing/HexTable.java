//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

public class HexTable extends JTable {
    private static final long serialVersionUID = 1L;
    private final HexEditor hexEditor;
    private HexTableModel model;
    int leadSelectionIndex;
    int anchorSelectionIndex;
    private static final Color ALTERNATING_CELL_COLOR;

    public HexTable(final HexEditor hexEditor, final HexTableModel model) {
        super(model);
        this.hexEditor = hexEditor;
        this.model = model;
        this.enableEvents(8L);
        this.setAutoResizeMode(0);
        this.setFont(new Font("Monospaced", 0, 14));
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(1);
        this.setSurrendersFocusOnKeystroke(true);
        this.setDefaultEditor(Object.class, new CellEditor());
        this.setDefaultRenderer(Object.class, new CellRenderer());
        this.getTableHeader().setReorderingAllowed(false);
        this.setShowGrid(false);
        final FontMetrics fm = this.getFontMetrics(this.getFont());
        final Font headerFont = UIManager.getFont("TableHeader.font");
        final FontMetrics headerFM = hexEditor.getFontMetrics(headerFont);
        int w = fm.stringWidth("wwww");
        w = Math.max(w, headerFM.stringWidth("+999"));
        for (int i = 0; i < this.getColumnCount(); ++i) {
            final TableColumn column = this.getColumnModel().getColumn(i);
            if (i < 16) {
                column.setPreferredWidth(w);
            } else {
                column.setPreferredWidth(200);
            }
        }
        this.setPreferredScrollableViewportSize(new Dimension(w * 16 + 200, 25 * this.getRowHeight()));
        final int n = 0;
        this.leadSelectionIndex = n;
        this.anchorSelectionIndex = n;
    }

    private int adjustColumn(final int row, final int col) {
        if (col < 0) {
            return 0;
        }
        if (row == this.getRowCount() - 1) {
            int lastRowCount = this.model.getByteCount() % 16;
            if (lastRowCount == 0) {
                lastRowCount = 16;
            }
            if (lastRowCount < 16) {
                return Math.min(col, this.model.getByteCount() % 16 - 1);
            }
        }
        return Math.min(col, this.getColumnCount() - 1 - 1);
    }

    public int cellToOffset(final int row, final int col) {
        if (row < 0 || row >= this.getRowCount() || col < 0 || col > 15) {
            return -1;
        }
        final int offs = row * 16 + col;
        return (offs >= 0 && offs < this.model.getByteCount()) ? offs : -1;
    }

    @Override
    public void changeSelection(int row, int col, final boolean toggle, final boolean extend) {
        col = this.adjustColumn(row, col);
        if (row < 0) {
            row = 0;
        }
        this.repaintSelection();
        if (extend) {
            this.leadSelectionIndex = this.cellToOffset(row, col);
        } else {
            final int cellToOffset = this.cellToOffset(row, col);
            this.leadSelectionIndex = cellToOffset;
            this.anchorSelectionIndex = cellToOffset;
        }
        if (this.getAutoscrolls()) {
            this.ensureCellIsVisible(row, col);
        }
        this.repaintSelection();
    }

    public void changeSelectionByOffset(int offset, final boolean extend) {
        offset = Math.max(0, offset);
        offset = Math.min(offset, this.model.getByteCount() - 1);
        final int row = offset / 16;
        final int col = offset % 16;
        this.changeSelection(row, col, false, extend);
    }

    @Override
    public void clearSelection() {
        if (this.anchorSelectionIndex > -1) {
            this.leadSelectionIndex = this.anchorSelectionIndex;
        } else {
            final int n = 0;
            this.leadSelectionIndex = n;
            this.anchorSelectionIndex = n;
        }
        this.repaintSelection();
    }

    private void ensureCellIsVisible(final int row, final int col) {
        final Rectangle cellRect = this.getCellRect(row, col, false);
        if (cellRect != null) {
            this.scrollRectToVisible(cellRect);
        }
    }

    public byte getByte(final int offset) {
        return this.model.getByte(offset);
    }

    public int getByteCount() {
        return this.model.getByteCount();
    }

    public int getLargestSelectionIndex() {
        return Math.max(this.leadSelectionIndex, this.anchorSelectionIndex);
    }

    public int getSmallestSelectionIndex() {
        return Math.min(this.leadSelectionIndex, this.anchorSelectionIndex);
    }

    @Override
    public boolean isCellEditable(final int row, final int col) {
        return row >= 0 && col >= 0 && col < 16; //headers are not part of this condition
    }

    @Override
    public boolean isCellSelected(final int row, final int col) {
        final int offset = this.cellToOffset(row, col);
        if (offset == -1) {
            return false;
        }
        final int start = this.getSmallestSelectionIndex();
        final int end = this.getLargestSelectionIndex();
        return offset >= start && offset <= end;
    }

    public Point offsetToCell(final int offset) {
        if (offset < 0 || offset >= this.model.getByteCount()) {
            return new Point(-1, -1);
        }
        final int row = offset / 16;
        final int col = offset % 16;
        return new Point(row, col);
    }

    public void open(final String fileName) throws IOException {
        this.model.setBytes(fileName);
    }

    public void open(final InputStream in) throws IOException {
        this.model.setBytes(in);
    }

    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
        final Object value = this.getValueAt(row, column);
        final boolean isSelected = this.isCellSelected(row, column);
        final boolean hasFocus = this.cellToOffset(row, column) == this.leadSelectionIndex;
        return renderer.getTableCellRendererComponent(this, value, isSelected, hasFocus, row, column);
    }

    private void processKeyPressedEvent(KeyEvent e, boolean isExtended, int offset) {
        this.changeSelectionByOffset(offset, isExtended);
        e.consume();
    }

    @Override
    protected void processKeyEvent(final KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            final boolean extend = e.isShiftDown();

            switch (e.getKeyCode()) {
                case 37: {
                    processKeyPressedEvent(e, extend, Math.max(this.leadSelectionIndex - 1, 0));
                    break;
                }
                case 39: {
                    processKeyPressedEvent(
                            e, extend, Math.min(this.leadSelectionIndex + 1, this.model.getByteCount() - 1)
                    );
                    break;
                }
                case 38: {
                    processKeyPressedEvent(e, extend, Math.max(this.leadSelectionIndex - 16, 0));
                    break;
                }
                case 40: {
                    processKeyPressedEvent(
                            e, extend, Math.min(this.leadSelectionIndex + 16, this.model.getByteCount() - 1)
                    );
                    break;
                }
                case 34: {
                    final int visibleRowCount = this.getVisibleRect().height / this.getRowHeight();
                    final int offs = Math.min(
                            this.leadSelectionIndex + visibleRowCount * 16, this.model.getByteCount() - 1
                    );

                    processKeyPressedEvent(e, extend, offs);
                    break;
                }
                case 33: {
                    final int visibleRowCount = this.getVisibleRect().height / this.getRowHeight();
                    final int offs = Math.max(this.leadSelectionIndex - visibleRowCount * 16, 0);

                    processKeyPressedEvent(e, extend, offs);
                    break;
                }
                case 36: {
                    processKeyPressedEvent(e, extend, this.leadSelectionIndex / 16 * 16);
                    break;
                }
                case 35: {
                    int offs = this.leadSelectionIndex / 16 * 16 + 15;
                    offs = Math.min(offs, this.model.getByteCount() - 1);

                    processKeyPressedEvent(e, extend, offs);
                    break;
                }
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_ENTER:
                    final int row = anchorSelectionIndex / 16;
                    final int col = anchorSelectionIndex % 16;

                    this.editCellAt(row, col);
                    e.consume();
                    break;
                default:
                    break;
            }
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
            switch (e.getKeyChar()) {
                case ' ':
                case '\n':
                    e.consume(); // disregard KEY_TYPED after KEY_PRESSED
                    break;
                default:
                    break;
            }
        }

        super.processKeyEvent(e);
    }

    public boolean redo() {
        return this.model.redo();
    }

    void removeBytes(final int offs, final int len) {
        this.model.removeBytes(offs, len);
    }

    private void repaintSelection() {
        this.repaint();
    }

    public void replaceBytes(final int offset, final int len, final byte[] bytes) {
        this.model.replaceBytes(offset, len, bytes);
    }

    public void setSelectedRows(final int min, final int max) {
        if (min < 0 || min >= this.getRowCount() || max < 0 || max >= this.getRowCount()) {
            throw new IllegalArgumentException();
        }
        final int startOffs = min * 16;
        final int endOffs = max * 16 + 15;
        this.changeSelectionByOffset(startOffs, false);
        this.changeSelectionByOffset(endOffs, true);
    }

    public void setSelectionByOffsets(int startOffs, final int endOffs) {
        startOffs = Math.max(0, startOffs);
        startOffs = Math.min(startOffs, this.model.getByteCount() - 1);
        this.repaintSelection();
        this.anchorSelectionIndex = startOffs;
        this.leadSelectionIndex = endOffs;
        if (this.getAutoscrolls()) {
            int endRow = endOffs / 16;
            int endCol = endOffs % 16;
            endCol = this.adjustColumn(endRow, endCol);
            if (endRow < 0) {
                endRow = 0;
            }
            this.ensureCellIsVisible(endRow, endCol);
        }
        this.repaintSelection();
    }

    public boolean undo() {
        return this.model.undo();
    }

    public byte[] getDocBuffer() {
        return model.getDoc();
    }

    static {
        ALTERNATING_CELL_COLOR = new Color(240, 240, 240);
    }

    private static class CellEditor extends DefaultCellEditor implements FocusListener {
        private static final long serialVersionUID = 1L;

        CellEditor() {
            super(createTextField());
            final AbstractDocument doc = (AbstractDocument) ((JTextComponent) this.editorComponent).getDocument();
            doc.setDocumentFilter(new EditorDocumentFilter());
            this.getComponent().addFocusListener(this);
        }

        private static JTextField createTextField() {
            JTextField r = new JTextField();
            r.setBorder(BorderFactory.createEmptyBorder());
            return r;
        }

        public void focusGained(final FocusEvent e) {
            final JTextField textField = (JTextField) this.getComponent();
            textField.selectAll();
        }

        public void focusLost(final FocusEvent e) {
        }

        @Override
        public boolean stopCellEditing() {
            final String value = (String) this.getCellEditorValue();
            if (value.length() == 0) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);
                return false;
            }
            return super.stopCellEditing();
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private Point highlight;

        CellRenderer() {
            this.highlight = new Point();
        }

        @Override
        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                final boolean selected,
                final boolean focus,
                final int row,
                final int column
        ) {
            super.getTableCellRendererComponent(table, sanitize(value, row, column), selected, focus, row, column);
            this.highlight.setLocation(-1, -1);
            if (column == table.getColumnCount() - 1 && HexTable.this.hexEditor.getHighlightSelectionInAsciiDump()) {
                final int selStart = HexTable.this.getSmallestSelectionIndex();
                final int selEnd = HexTable.this.getLargestSelectionIndex();
                final int b1 = row * 16;
                final int b2 = b1 + 15;
                if (selStart <= b2 && selEnd >= b1) {
                    final int start = Math.max(selStart, b1) - b1;
                    final int end = Math.min(selEnd, b2) - b1;
                    this.highlight.setLocation(start, end);
                }
                final boolean colorBG = HexTable.this.hexEditor.getAlternateRowBackground() && (row & 0x1) > 0;
                this.setBackground(colorBG ? HexTable.ALTERNATING_CELL_COLOR : table.getBackground());
            } else if (!selected) {
                if ((HexTable.this.hexEditor.getAlternateRowBackground() && (row & 0x1) > 0) ^ // xor!
                        (HexTable.this.hexEditor.getAlternateColumnBackground() && (column & 0x1) > 0)) {
                    this.setBackground(HexTable.ALTERNATING_CELL_COLOR);
                } else {
                    this.setBackground(table.getBackground());
                }
            }
            return this;
        }

        private Object sanitize(Object value, int row, int column) {
            if (column < 0 || column > 15 || !(value instanceof String)) {
                return value;
            }

            String s = (String) value;
            if (s.length() < 2) {
                return "0" + s.toUpperCase();
            } else {
                return s.toUpperCase();
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            if (this.highlight.x > -1) {
                final int w = this.getFontMetrics(HexTable.this.getFont()).charWidth('w');
                g.setColor(HexTable.this.hexEditor.getHighlightSelectionInAsciiDumpColor());
                final int x = this.getInsets().left + this.highlight.x * w;
                g.fillRect(x, 0, (this.highlight.y - this.highlight.x + 1) * w, HexTable.this.getRowHeight());
            }
            g.setColor(this.getForeground());
            int x2 = 2;
            final String text = this.getText();
            if (text.length() == 1) {
                x2 += g.getFontMetrics().charWidth('w');
            }
            g.drawString(text, x2, 11);
        }
    }

    private static class EditorDocumentFilter extends DocumentFilter {
        private boolean ensureByteRepresented(final String str) {
            try {
                final int i = Integer.parseInt(str, 16);
                if (i < 0 || i > 255) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);
                return false;
            }
            return true;
        }

        @Override
        public void insertString(
                final FilterBypass fb, final int offs, final String string, final AttributeSet attr
        ) throws BadLocationException {
            final Document doc = fb.getDocument();
            final String temp = doc.getText(0, offs) + string + doc.getText(offs, doc.getLength() - offs);
            if (this.ensureByteRepresented(temp)) {
                fb.insertString(offs, temp, attr);
            }
        }

        @Override
        public void replace(
                final FilterBypass fb, final int offs, final int len, final String text, final AttributeSet attrs
        ) throws BadLocationException {
            final Document doc = fb.getDocument();
            final String temp = doc.getText(0, offs) + text + doc.getText(offs + len, doc.getLength() - (offs + len));
            if (this.ensureByteRepresented(temp)) {
                fb.replace(offs, len, text, attrs);
            }
        }
    }
}
