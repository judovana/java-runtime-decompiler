package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.jrd.backend.completion.ClassesAndMethodsProvider;

public class JavacCompileAction extends CompileAction {
    public JavacCompileAction(String title) {
        this(title, null);
    }

    public JavacCompileAction(String title, ClassesAndMethodsProvider classesAndMethodsProvider) {
        super(title);
    }
}
