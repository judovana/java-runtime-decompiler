package org.jrd.frontend.frame.main.decompilerview.verifiers;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.cli.Lib;

import javax.swing.event.DocumentEvent;
import java.io.File;
import java.nio.file.Files;

public class ClassVerifier extends FileVerifier {

    public ClassVerifier(GetSetText source, GetSetText target) {
        super(source, target);
    }

    public boolean verifySource(DocumentEvent documentEvent) {
        boolean intermezo = super.verifySource(documentEvent);
        if (intermezo) {
            File f = new File(source.getText());
            byte[] b = null;
            try {
                b = Files.readAllBytes(f.toPath());
            } catch (Exception ex) {
                target.setText(ex.getMessage());
                Logger.getLogger().log(ex);
            }
            if (b == null || b.length == 0) {
                target.setText("is empty");
            } else {
                try {
                    target.setText(Lib.readClassNameFromClass(b));
                    return true;
                } catch (Exception ex) {
                    target.setText(ex.getMessage());
                    Logger.getLogger().log(ex);
                }
            }
        }
        return false;
    }
}
