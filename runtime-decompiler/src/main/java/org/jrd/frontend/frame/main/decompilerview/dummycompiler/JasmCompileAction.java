package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.decompiling.DecompilerWrapper;


public class JasmCompileAction extends CompileAction {

    private final DecompilerWrapper jasm;
    private final ClassesAndMethodsProvider classesAndMethodsProvider;

    public JasmCompileAction(String title, DecompilerWrapper jasm,
                             ClassesAndMethodsProvider classesAndMethodsProvider) {
        super(title);
        this.jasm = jasm;
        this.classesAndMethodsProvider = classesAndMethodsProvider;
    }

}
