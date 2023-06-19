package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.main.decompilerview.QuickCompiler;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class JasmCompileAction extends AbstractCompileAction implements  CanCompile {

    private final DecompilerWrapper jasm;
    private final ClassesAndMethodsProvider classesAndMethodsProvider;

    public JasmCompileAction(String title, DecompilerWrapper jasm, ClassesAndMethodsProvider classesAndMethodsProvider) {
        super(title);
        this.jasm = jasm;
        this.classesAndMethodsProvider = classesAndMethodsProvider;
    }

    @Override
    public Collection<IdentifiedBytecode> compile(final String s, final PluginManager pluginManager) {
        final ClassesProvider classesProvider;
        if (classesAndMethodsProvider == null) {
            classesProvider = new NullClassesProvider();
        } else {
            classesProvider = new ClassesAndMethodsProviderBasedClassesProvider(classesAndMethodsProvider);
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
        qc.run(jasm, false, new IdentifiedSource(new ClassIdentifier("some.tmp.jasm"),
                s.getBytes(StandardCharsets.UTF_8)));
        return qc.waitResult();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "looks good")
    @Override
    public DecompilerWrapper getWrapper() {
        return jasm;
    }

}
