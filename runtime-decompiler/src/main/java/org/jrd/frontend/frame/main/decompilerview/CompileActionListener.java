package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Model;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.BytemanCompileAction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

class CompileActionListener implements ActionListener {
    private final BytecodeDecompilerView mBytecodeDecompilerView;

    CompileActionListener(BytecodeDecompilerView bytecodeDecompilerView) {
        this.mBytecodeDecompilerView = bytecodeDecompilerView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        GlobalConsole.getConsole().show();
        Logger.getLogger().log("Compilation started");
        if (mBytecodeDecompilerView.isBytemanBufferVisible()) {
            Logger.getLogger().log(Logger.Level.ALL, "Byteman typecheck  called");
            BytemanCompileAction btmcheck = new BytemanCompileAction("Byteman typecheck  called", null, null);
            Collection<IdentifiedBytecode> l = btmcheck.compile(
                    Collections.singletonList(mBytecodeDecompilerView.getBytemanScript().getText()), Model.getModel().getPluginManager()
            );
            if (l == null || l.size() == 0 || new ArrayList<IdentifiedBytecode>(l).get(0).getFile().length == 0) {
                Logger.getLogger().log(Logger.Level.ALL, "Faield");
                Logger.getLogger().log(Logger.Level.ALL, "Note, Errors of ERROR : Could not load class...");
                Logger.getLogger().log(Logger.Level.ALL, "  As current byteman validator can not see to remote VM.");
                Logger.getLogger().log(Logger.Level.ALL, "  Thus they can be ignored. But not the others.");
                Logger.getLogger()
                        .log(Logger.Level.ALL, "To allow such scripts injection, we ignore result of tyepcheck " + "before injection");
                Logger.getLogger().log(Logger.Level.ALL, "  be sure you fixed all other issues");
            } else {
                Logger.getLogger().log(Logger.Level.ALL, "Ok.");
            }
            Logger.getLogger().log(Logger.Level.ALL, "Byteman typecheck  finished");
        } else if (mBytecodeDecompilerView.isAdditionalBinaryBufferVisible()) {
            JOptionPane.showMessageDialog(
                    mBytecodeDecompilerView.getBuffers(), "Unlike (compile) and upload, compile is only for source buffers"
            );
        } else if (mBytecodeDecompilerView.isBinaryBufferVisible()) {
            JOptionPane.showMessageDialog(
                    mBytecodeDecompilerView.getBuffers(), "Unlike (compile) and upload, compile is only for " + "source buffers"
            );
        } else if (mBytecodeDecompilerView.isDecompiledBytecodeBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), false,
                    new IdentifiedSource(
                            new ClassIdentifier(mBytecodeDecompilerView.getLastDecompiledClass()),
                            mBytecodeDecompilerView.getBytecodeBuffer().getTextAsBytes()
                    )
            );
        } else if (mBytecodeDecompilerView.isAdditionalDecompiledBytecodeBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), false,
                    new IdentifiedSource(
                            new ClassIdentifier(mBytecodeDecompilerView.getLastDecompiledClass()),
                            mBytecodeDecompilerView.getAdditionalBytecodeBuffer().getTextAsBytes()
                    )
            );
        } else if (mBytecodeDecompilerView.isAdditionalSrcBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), false,
                    new IdentifiedSource(
                            new ClassIdentifier(mBytecodeDecompilerView.getLastDecompiledClass()),
                            mBytecodeDecompilerView.getAdditionalSrcBuffer().getTextAsBytes()
                    )
            );
        } else {
            JOptionPane.showMessageDialog(
                    mBytecodeDecompilerView.getBuffers(),
                    "nothing selected - " + mBytecodeDecompilerView.getBuffers().getSelectedComponent()
            );
        }
    }
}
