package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Model;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.BytemanCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

class CompileAndUploadActionListener implements ActionListener {
    private final BytecodeDecompilerView mBytecodeDecompilerView;

    CompileAndUploadActionListener(BytecodeDecompilerView bytecodeDecompilerView) {
        mBytecodeDecompilerView = bytecodeDecompilerView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        GlobalConsole.getConsole().show();
        Logger.getLogger().log("Compilation with upload started");
        if (mBytecodeDecompilerView.isBytemanBufferVisible()) {
            Logger.getLogger().log(Logger.Level.ALL, "Byteman injection called");
            BytemanCompileAction btmcheck = new BytemanCompileAction(
                    "Byteman injection  called", mBytecodeDecompilerView.getClasspathProvider(), new UploadProvider() {
                        @Override
                        public ClasspathProvider getTarget() {
                            return mBytecodeDecompilerView.getClasspathProvider();
                        }

                        @Override
                        public boolean isUploadEnabled() {
                            return true;
                        }

                        @Override
                        public void resetUpload() {

                        }

                        @Override
                        public boolean isBoot() {
                            return false;
                        }
                    }
            );
            Collection<IdentifiedBytecode> l = btmcheck.compile(
                    Collections.singletonList(mBytecodeDecompilerView.getBytemanScript().getText()), Model.getModel().getPluginManager()
            );
            if (l == null || l.size() == 0 || new ArrayList<IdentifiedBytecode>(l).get(0).getFile().length == 0) {
                Logger.getLogger().log(Logger.Level.ALL, "Faield");
            } else {
                Logger.getLogger().log(Logger.Level.ALL, "Ok.");
            }
            Logger.getLogger().log(Logger.Level.ALL, "Byteman inject finished");
        } else if (mBytecodeDecompilerView.isAdditionalBinaryBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction()
                    .upload(mBytecodeDecompilerView.getLastDecompiledClass(), mBytecodeDecompilerView.getAdditionalBinary().get());
        } else if (mBytecodeDecompilerView.isBinaryBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction()
                    .upload(mBytecodeDecompilerView.getLastDecompiledClass(), mBytecodeDecompilerView.getBinary().get());
        } else if (mBytecodeDecompilerView.isDecompiledBytecodeBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), true,
                    new IdentifiedSource(
                            new ClassIdentifier(mBytecodeDecompilerView.getLastDecompiledClass()),
                            mBytecodeDecompilerView.getBytecodeBuffer().getTextAsBytes()
                    )
            );
        } else if (mBytecodeDecompilerView.isAdditionalDecompiledBytecodeBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), true,
                    new IdentifiedSource(
                            new ClassIdentifier(mBytecodeDecompilerView.getLastDecompiledClass()),
                            mBytecodeDecompilerView.getAdditionalBytecodeBuffer().getTextAsBytes()
                    )
            );
        } else if (mBytecodeDecompilerView.isAdditionalSrcBufferVisible()) {
            mBytecodeDecompilerView.getCompileAction().run(
                    mBytecodeDecompilerView.getPluginComboBox(), true,
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
