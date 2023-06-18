package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

public class JustBearerAction extends CompileAction {

    private final String append;
    private CompileAction original;

    public JustBearerAction(String placeholder, String append) {
        super(placeholder + " " + append);
        this.append = append;
    }

    public void setOriginal(CompileAction original) {
        this.original = original;
        this.setEnabled(true);
        this.setText("last used - " + original.getText() + " " + append);
    }
}
