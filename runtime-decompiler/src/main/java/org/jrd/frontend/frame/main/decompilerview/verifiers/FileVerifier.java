package org.jrd.frontend.frame.main.decompilerview.verifiers;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;

public class FileVerifier implements DocumentListener {

    protected final GetSetText source;
    protected final GetSetText target;

    public FileVerifier(GetSetText source, GetSetText target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        verifySource(documentEvent);
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        verifySource(documentEvent);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        verifySource(documentEvent);
    }

    public boolean verifySource(DocumentEvent documentEvent) {
        File f = new File(source.getText());
        if (!f.exists()) {
            target.setText("file do not exists");
            return false;
        } else {
            if (f.isDirectory()) {
                target.setText("is directory");
                return false;
            }
            target.setText("ok");
            return true;
        }
    }
}
