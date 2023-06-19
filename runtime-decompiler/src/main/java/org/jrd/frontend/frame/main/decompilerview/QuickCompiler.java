package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.CommonUtils;

import java.util.logging.Level;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

public class QuickCompiler {

    private final ModelProvider modelProvider;
    private final PluginManager pluginManager;

    public QuickCompiler(ModelProvider modelProvider, PluginManager pluginManager) {
        this.modelProvider = modelProvider;
        this.pluginManager = pluginManager;
    }

    public void upload(String clazz, byte[] body) {
        CommonUtils.uploadByGui(modelProvider.getVmInfo(), modelProvider.getVmManager(), new CommonUtils.StatusKeeper() {
            @Override
            public void setText(String s) {
                GlobalConsole.getConsole().addMessage(Level.WARNING, s);
            }

            @Override
            public void onException(Exception ex) {
                Logger.getLogger().log(ex);
                GlobalConsole.getConsole().addMessage(Level.WARNING, ex.toString());
            }
        }, clazz, body);
    }

    public void run(DecompilerWrapper wrapper, boolean upload, IdentifiedSource... srcs) {
        PluginManager.BundledCompilerStatus internalCompiler = pluginManager.getBundledCompilerStatus(wrapper);
        GlobalConsole.getConsole().addMessage(Level.ALL, internalCompiler.getStatus());
        OverwriteClassDialog.CompilationWithResult compiler = new OverwriteClassDialog.CompilationWithResult(
                OverwriteClassDialog.getClasspathlessCompiler(wrapper, internalCompiler.isEmbedded(), Logger.getLogger().isVerbose()),
                modelProvider.getClassesProvider(), GlobalConsole.getConsole(), srcs
        ) {

            @Override
            public void run() {
                super.run();
                if (this.getResult() != null && !this.getResult().isEmpty()) {
                    for (IdentifiedBytecode bin : this.getResult()) {
                        if (upload) {
                            GlobalConsole.getConsole().addMessage(Level.ALL, "Uploading: " + bin.getClassIdentifier().getFullName());
                            upload(bin.getClassIdentifier().getFullName(), bin.getFile());
                        } else {
                            GlobalConsole.getConsole().addMessage(Level.ALL, "Compiled " + bin.getClassIdentifier().getFullName());
                        }
                    }
                } else {
                    GlobalConsole.getConsole().addMessage(Level.ALL, "No output from compilation");
                }
            }
        };
        Thread t = new Thread(compiler);
        t.start();
    }
}
