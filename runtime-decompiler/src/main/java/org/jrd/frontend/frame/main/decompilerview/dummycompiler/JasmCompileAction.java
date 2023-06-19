package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.QuickCompiler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JasmCompileAction extends AbstractCompileAction {

    private final DecompilerWrapper jasm;
    private final ClassesAndMethodsProvider classesAndMethodsProvider;
    private QuickCompiler compileAction;

    public JasmCompileAction(String title, DecompilerWrapper jasm, ClassesAndMethodsProvider classesAndMethodsProvider) {
        super(title);
        this.jasm = jasm;
        this.classesAndMethodsProvider = classesAndMethodsProvider;
    }

    public Collection<IdentifiedBytecode> compile(String s) {
        Collection<IdentifiedBytecode> r;
        if (classesAndMethodsProvider == null) {
            r = new RuntimeCompilerConnector.ForeignCompilerWrapper(jasm).compileClass(new ClassesProvider() {
                @Override
                public Collection<IdentifiedBytecode> getClass(ClassIdentifier... classIdentifiers) {
                    return new ArrayList<>(0);
                }

                @Override
                public List<String> getClassPathListing() {
                    return new ArrayList<String>(0);
                }
            }, Optional.of(GlobalConsole.getConsole()),
                    new IdentifiedSource(new ClassIdentifier("some.tmp.class"), s.getBytes(StandardCharsets.UTF_8))
            );
        } else {
            r = new RuntimeCompilerConnector.ForeignCompilerWrapper(jasm).compileClass(new ClassesProvider() {
                @Override
                public Collection<IdentifiedBytecode> getClass(ClassIdentifier... classIdentifiers) {
                    List<IdentifiedBytecode> result = new ArrayList<>(classIdentifiers.length);
                    for (ClassIdentifier ci : classIdentifiers) {
                        byte[] b = classesAndMethodsProvider.getClassItself(null, classIdentifiers[0].getFullName());
                        result.add(new IdentifiedBytecode(ci, b));
                    }
                    return result;
                }

                @Override
                public List<String> getClassPathListing() {
                    return Arrays.stream(classesAndMethodsProvider.getClasses(null)).collect(Collectors.toList());
                }
            }, Optional.of(GlobalConsole.getConsole()),
                    new IdentifiedSource(new ClassIdentifier("some.tmp.class"), s.getBytes(StandardCharsets.UTF_8))
            );
        }
        return r;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "looks good")
    public DecompilerWrapper getWrapper() {
        return jasm;
    }
}
