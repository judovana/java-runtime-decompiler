package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class JustBearerAction extends AbstractCompileAction {

    private final String append;
    private AbstractCompileAction original;

    public JustBearerAction(String placeholder, String append) {
        super(placeholder + " -  " + append);
        this.append = append;
    }

    @Override
    public String getText() {
        if (original == null) {
            return super.getText() + "<br/>";
        } else {
            return super.getText() + "<br/>" + original.getText();
        }
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "looks good")
    public void setOriginal(AbstractCompileAction original) {
        this.original = original;
        this.setEnabled(true);
        this.setText("last used - " + original.getText() + " " + append);
    }
}
