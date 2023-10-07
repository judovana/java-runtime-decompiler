package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

class InitAddClassJar implements ActionListener {
    private final BytecodeDecompilerView bytecodeDecompilerView;

    InitAddClassJar(BytecodeDecompilerView bytecodeDecompilerView) {
        this.bytecodeDecompilerView = bytecodeDecompilerView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            String[] fqn = new InitAddClassDialog(
                    bytecodeDecompilerView.getLastFqn(), bytecodeDecompilerView.getLastAddedFqn(), bytecodeDecompilerView.getLastAddedFile()
            ).showAndGet();

            if (fqn != null && fqn.length > 0) {
                if (fqn.length == 1) {
                    bytecodeDecompilerView.initGui(fqn[0]);
                } else if (fqn.length == 2) {
                    bytecodeDecompilerView.addClassGui(fqn[0], fqn[1]);
                } else if (fqn.length == 3) {
                    bytecodeDecompilerView.addJar(Boolean.parseBoolean(fqn[0]), fqn[2], fqn[1]);
                } else {
                    bytecodeDecompilerView.addClassesGui(Boolean.parseBoolean(fqn[0]), Arrays.copyOfRange(fqn, 4, fqn.length));
                }
            }
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            JOptionPane.showMessageDialog(bytecodeDecompilerView.getBuffers(), ex.getMessage());
        }
    }
}
