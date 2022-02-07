package org.jrd.frontend.frame.main.decompilerview;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.undo.UndoManager;

class UndoRedoKeyAdapter extends KeyAdapter {
    private UndoManager undoManager = new UndoManager();

    @Override
    public void keyReleased(KeyEvent e) {
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
