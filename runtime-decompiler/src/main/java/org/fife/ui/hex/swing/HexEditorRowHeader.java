//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.swing;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;

public class HexEditorRowHeader extends JList implements TableModelListener {
    private static final long serialVersionUID = 1L;
    private HexTable table;
    private RowHeaderListModel model;
    private static final Border CELL_BORDER;

    public HexEditorRowHeader(final HexTable table) {
        this.table = table;
        this.model = new RowHeaderListModel();

        this.setModel(model);
        this.setFocusable(false);
        this.setFont(table.getFont());
        this.setFixedCellHeight(table.getRowHeight());
        this.setCellRenderer(new CellRenderer());
        this.setBorder(new RowHeaderBorder());
        this.setSelectionMode(1);
        this.syncRowCount();
        table.getModel().addTableModelListener(this);
    }

    @Override
    public void addSelectionInterval(final int anchor, final int lead) {
        super.addSelectionInterval(anchor, lead);
        final int min = Math.min(anchor, lead);
        final int max = Math.max(anchor, lead);
        this.table.setSelectedRows(min, max);
    }

    @Override
    public void removeSelectionInterval(final int index0, final int index1) {
        super.removeSelectionInterval(index0, index1);
        final int anchor = this.getAnchorSelectionIndex();
        final int lead = this.getLeadSelectionIndex();
        this.table.setSelectedRows(Math.min(anchor, lead), Math.max(anchor, lead));
    }

    @Override
    public void setSelectionInterval(final int anchor, final int lead) {
        super.setSelectionInterval(anchor, lead);
        final int min = Math.min(anchor, lead);
        final int max = Math.max(anchor, lead);
        this.table.setSelectedRows(min, max);
    }

    private void syncRowCount() {
        if (this.table.getRowCount() != this.model.getSize()) {
            this.model.setSize(this.table.getRowCount());
        }
    }

    public void tableChanged(final TableModelEvent e) {
        this.syncRowCount();
    }

    static {
        CELL_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 5);
    }

    private static class CellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        CellRenderer() {
            this.setHorizontalAlignment(4);
        }

        @Override
        public Component getListCellRendererComponent(
                final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, false, hasFocus);
            this.setBorder(HexEditorRowHeader.CELL_BORDER);
            return this;
        }
    }

    private static class RowHeaderListModel extends AbstractListModel {
        private static final long serialVersionUID = 1L;
        private int size;

        public Object getElementAt(final int index) {
            return "0x" + Integer.toHexString(index * 16);
        }

        public int getSize() {
            return this.size;
        }

        public void setSize(final int size) {
            final int old = this.size;
            this.size = size;
            final int diff = size - old;
            if (diff > 0) {
                this.fireIntervalAdded(this, old, size - 1);
            } else if (diff < 0) {
                this.fireIntervalRemoved(this, size + 1, old - 1);
            }
        }
    }

    private class RowHeaderBorder extends EmptyBorder {
        private static final long serialVersionUID = 1L;

        RowHeaderBorder() {
            super(0, 0, 0, 2);
        }

        @Override
        public void paintBorder(
                final Component c, final Graphics g, int x, final int y, final int width, final int height
        ) {
            x = x + width - this.right;
            g.setColor(HexEditorRowHeader.this.table.getGridColor());
            g.drawLine(x, y, x, y + height);
        }
    }
}
