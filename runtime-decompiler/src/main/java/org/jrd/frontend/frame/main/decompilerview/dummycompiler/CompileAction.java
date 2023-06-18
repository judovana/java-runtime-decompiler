package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.jrd.backend.decompiling.DecompilerWrapper;

import javax.swing.JMenuItem;

public abstract class CompileAction extends JMenuItem {

    public CompileAction(String title) {
        super(title);
    }

}
