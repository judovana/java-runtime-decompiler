package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import javax.swing.JMenuItem;
import java.io.File;

public abstract class AbstractCompileAction extends JMenuItem {

    private final String stub;

    public AbstractCompileAction(String stub) {
        super();
        this.stub = stub;
    }

    @Override
    public String getText() {
        return "<html><u>" + stub + "</u>";
    }

    public Object getStub() {
        return stub;
    }

}
