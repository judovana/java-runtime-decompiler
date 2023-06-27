package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.main.decompilerview.QuickCompiler;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ExecuteMethodProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.SaveProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class JasmCompileAction extends AbstractCompileAndRunAction implements CanCompile {

    private final DecompilerWrapper jasm;

    public JasmCompileAction(String title, DecompilerWrapper jasm, ClasspathProvider classesAndMethodsProvider, SaveProvider save,
                             UploadProvider upload, ExecuteMethodProvider execute) {
        super(title,classesAndMethodsProvider, save, upload, execute);
        this.jasm = jasm;

    }

    @Override
    public Collection<IdentifiedBytecode> compile(final String s, final PluginManager pluginManager) {
        final ClassesProvider classesProvider;
        if (classesAndMethodsProvider == null) {
            classesProvider = new NullClassesProvider();
        } else {
            classesProvider = new ClassesAndMethodsProviderBasedClassesProvider(classesAndMethodsProvider.getClasspath());
        }
        QuickCompiler qc = new QuickCompiler(new ModelProvider() {
            @Override
            public VmInfo getVmInfo() {
                return null;
            }

            @Override
            public VmManager getVmManager() {
                return null;
            }

            @Override
            public ClassesProvider getClassesProvider() {
                return classesProvider;
            }
        }, pluginManager);
        Collection<IdentifiedBytecode> result;
        try {
            byte[] file = s.getBytes(StandardCharsets.UTF_8);
            String fqn = Lib.guessName(file);
            qc.run(jasm, false, new IdentifiedSource(new ClassIdentifier(fqn), file));
            result = qc.waitResult();
            if (result != null && result.size() > 0) {
                if (save!=null) {
                    CanCompile.save(result, save.getSaveDirectory());
                }
                if (upload!=null && upload.isUploadEnabled()) {
                    CanCompile.upload(result, upload);
                    upload.resetUpload();
                }
                if (execute != null) {
                    CanCompile.run(fqn, result, execute.getMethodToExecute(), classesProvider);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return new ArrayList<>(0);
        }
        return result;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "looks good")
    @Override
    public DecompilerWrapper getWrapper() {
        return jasm;
    }

}
