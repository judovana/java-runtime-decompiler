package org.jrd.frontend.frame.main.decompilerview;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class OverwriteActionListener implements ActionListener {

    private final ClassOverwriter worker;
    private final BytecodeDecompilerView bytecodeDecompilerView;

    OverwriteActionListener(BytecodeDecompilerView bytecodeDecompilerView, ClassOverwriter worker) {
        this.worker = worker;
        this.bytecodeDecompilerView = bytecodeDecompilerView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (
            bytecodeDecompilerView.isBinaryBufferVisible() || bytecodeDecompilerView.isDecompiledBytecodeBufferVisible()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getBytecodeBuffer().getText(), bytecodeDecompilerView.getBinary().get(),
                    bytecodeDecompilerView.isBinaryBufferVisible()
            );
        } else if (
            bytecodeDecompilerView.isAdditionalBinaryBufferVisible() ||
                    bytecodeDecompilerView.isAdditionalDecompiledBytecodeBufferVisible()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getAdditionalBytecodeBuffer().getText(), bytecodeDecompilerView.getAdditionalBinary().get(),
                    bytecodeDecompilerView.isAdditionalBinaryBufferVisible()
            );
        } else if (bytecodeDecompilerView.isAdditionalSrcBufferVisible()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getAdditionalSrcBuffer().getText(), new byte[0],
                    bytecodeDecompilerView.isAdditionalBinaryBufferVisible()
            );
        } else {
            JOptionPane.showMessageDialog(
                    bytecodeDecompilerView.getBuffers(), "No tab selected? \n " + bytecodeDecompilerView.getBuffers().getSelectedComponent()
            );
        }

    }
}
