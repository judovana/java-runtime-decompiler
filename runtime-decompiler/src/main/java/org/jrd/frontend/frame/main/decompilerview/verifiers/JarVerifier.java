package org.jrd.frontend.frame.main.decompilerview.verifiers;

import org.jrd.backend.core.Logger;

import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarVerifier extends FileVerifier {

    public JarVerifier(GetSetText source, GetSetText target) {
        super(source, target);
    }

    public boolean verifySource(DocumentEvent documentEvent) {
        boolean intermezo = super.verifySource(documentEvent);
        if (intermezo) {
            File f = new File(source.getText());
            try {
                JarFile jf = new JarFile(f);
                int manfest = 0;
                int others = 0;
                for (Enumeration list = jf.entries(); list.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    if (entry.getName().contains("META-INF")) {
                        manfest++;
                    } else {
                        others++;
                    }
                }
                target.setText("contains " + others + " classes and " + manfest + " manifest items");
                jf.close();
                return true;
            } catch (Exception ex) {
                target.setText(ex.getMessage());
                Logger.getLogger().log(ex);
                return false;
            }
        } else {
            return false;
        }
    }
}
