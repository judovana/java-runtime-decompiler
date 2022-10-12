package org.jrd.frontend.frame.main.decompilerview;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.undo.UndoManager;

class UndoRedoKeyAdapter extends KeyAdapter {
    private final SearchControlsPanel searchControlsPanel;
    private UndoManager undoManager = new UndoManager();

    UndoRedoKeyAdapter(SearchControlsPanel searchControlsPanel) {
        this.searchControlsPanel = searchControlsPanel;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            searchControlsPanel.clickNextButton();
        }
        if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
            if (e.getKeyCode() == KeyEvent.VK_Z && undoManager.canUndo()) {
                undoManager.undo();
            } else if (e.getKeyCode() == KeyEvent.VK_Y && undoManager.canRedo()) {
                undoManager.redo();
            }
        }
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }
}
