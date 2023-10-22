package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.CommonUtils;

import java.util.Collection;
import java.util.logging.Level;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

public class QuickCompiler {

    private final ModelProvider modelProvider;
    private final PluginManager pluginManager;
    private Collection<IdentifiedBytecode> resultBackup;
    private Thread lastThread;

    public QuickCompiler(ModelProvider modelProvider, PluginManager pluginManager) {
        this.modelProvider = modelProvider;
        this.pluginManager = pluginManager;
    }

    public void upload(String clazz, String classloader, byte[] body) {
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
        }, clazz, classloader, body);
    }

    public void run(DecompilerWrapper wrapper, boolean upload, String classloader, IdentifiedSource... srcs) {
        PluginManager.BundledCompilerStatus internalCompiler = pluginManager.getBundledCompilerStatus(wrapper);
        GlobalConsole.getConsole().addMessage(Level.ALL, internalCompiler.getStatus());
        OverwriteClassDialog.CompilationWithResult compiler = new OverwriteClassDialog.CompilationWithResult(
                OverwriteClassDialog.getClasspathlessCompiler(wrapper, internalCompiler.isEmbedded(), Logger.getLogger().isVerbose()),
                modelProvider.getClassesProvider(), GlobalConsole.getConsole(), classloader, srcs
        ) {

            @Override
            public void run() {
                super.run();
                resultBackup = this.getResult();
                if (this.getResult() != null && !this.getResult().isEmpty()) {
                    for (IdentifiedBytecode bin : this.getResult()) {
                        if (upload) {
                            GlobalConsole.getConsole().addMessage(Level.ALL, "Uploading: " + bin.getClassIdentifier().getFullName());
                            upload(bin.getClassIdentifier().getFullName(), this.getUploadClassloader(), bin.getFile());
                        } else {
                            GlobalConsole.getConsole().addMessage(Level.ALL, "Compiled " + bin.getClassIdentifier().getFullName());
                        }
                    }
                } else {
                    GlobalConsole.getConsole().addMessage(Level.ALL, "No output from compilation");
                }
            }
        };
        lastThread = new Thread(compiler);
        lastThread.start();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "looks good")
    public Collection<IdentifiedBytecode> waitResult() {
        try {
            lastThread.join();
            return resultBackup;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
