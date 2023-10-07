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
        int tab = getTab();
        if (
            isAnyPrimaryTabSelected()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getBytecodeBuffer().getText(), bytecodeDecompilerView.getBinary().get(), tab
            );
        } else if (
            isSecondaryTabSelected()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getAdditionalBytecodeBuffer().getText(), bytecodeDecompilerView.getAdditionalBinary().get(), tab
            );
        } else if (bytecodeDecompilerView.isAdditionalSrcBufferVisible()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getAdditionalSrcBuffer().getText(), new byte[0], tab
            );
        } else if (bytecodeDecompilerView.isBytemanBufferVisible()) {
            worker.overwriteClass(
                    bytecodeDecompilerView.getSelectedDecompiler(), bytecodeDecompilerView.getLastDecompiledClass(),
                    bytecodeDecompilerView.getBytemanScript().getText(), new byte[0], tab
            );
        } else {
            JOptionPane.showMessageDialog(
                    bytecodeDecompilerView.getBuffers(), "No tab selected? \n " + bytecodeDecompilerView.getBuffers().getSelectedComponent()
            );
        }

    }

    private boolean isSecondaryTabSelected() {
        return bytecodeDecompilerView.isAdditionalBinaryBufferVisible() ||
                bytecodeDecompilerView.isAdditionalDecompiledBytecodeBufferVisible();
    }

    private boolean isAnyPrimaryTabSelected() {
        return bytecodeDecompilerView.isBinaryBufferVisible() || bytecodeDecompilerView.isDecompiledBytecodeBufferVisible();
    }

    private int getTab() {
        int tab = -1;
        if (bytecodeDecompilerView.isBytemanBufferVisible()) {
            tab = 3;
        }
        if (bytecodeDecompilerView.isBinaryBufferVisible() || bytecodeDecompilerView.isAdditionalBinaryBufferVisible()) {
            tab = 2;
        }
        if (bytecodeDecompilerView.isDecompiledBytecodeBufferVisible() ||
                bytecodeDecompilerView.isAdditionalDecompiledBytecodeBufferVisible() ||
                bytecodeDecompilerView.isAdditionalSrcBufferVisible()
        ) {
            tab = 1;
        }
        return tab;
    }
}
