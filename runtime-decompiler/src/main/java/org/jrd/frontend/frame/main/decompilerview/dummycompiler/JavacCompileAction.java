package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.main.decompilerview.QuickCompiler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;


public class JavacCompileAction extends AbstractCompileAction implements  CanCompile{

    private final ClassesAndMethodsProvider classesAndMethodsProvider;

    public JavacCompileAction(String title, ClassesAndMethodsProvider classesAndMethodsProvider) {
        super(title);
        this.classesAndMethodsProvider = classesAndMethodsProvider;
    }

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
        try {
            byte[] file = s.getBytes(StandardCharsets.UTF_8);
            String[] fqn = Lib.guessNameImpl(file);
            String clazz;
            if (fqn.length == 1) {
                clazz = fqn[0];
            } else {
                clazz = fqn[0] + "." + fqn[1];
            }
            qc.run(null, false, new IdentifiedSource(new ClassIdentifier(clazz),
                    file));
            return qc.waitResult();
        } catch(Exception ex) {
            Logger.getLogger().log(ex);
            return new ArrayList<>(0);
        }
    }

    @Override
    public DecompilerWrapper getWrapper() {
        return null;
    }


}

