//
// Decompiled by Procyon v0.5.36
//

package org.fife.ui.hex.event;

import org.fife.ui.hex.swing.HexEditor;

import java.util.EventObject;

public class HexEditorEvent extends EventObject {
    private static final long serialVersionUID = 1L;
    private int offset;
    private int added;
    private int removed;

    public HexEditorEvent(final HexEditor editor, final int offs, final int added, final int removed) {
        super(editor);
        this.offset = offs;
        this.added = added;
        this.removed = removed;
    }

    public int getAddedCount() {
        return this.added;
    }

    public HexEditor getHexEditor() {
        return (HexEditor) this.getSource();
    }

    public int getOffset() {
        return this.offset;
    }

    public int getRemovedCount() {
        return this.removed;
    }

    public boolean isModification() {
        return this.getAddedCount() == this.getRemovedCount();
    }
}
