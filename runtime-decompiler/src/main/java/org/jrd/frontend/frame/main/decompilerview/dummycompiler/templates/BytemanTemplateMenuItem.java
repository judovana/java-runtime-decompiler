package org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BytemanTemplateMenuItem extends JMenuItem {

    private static final String STATIC_OBJECTFINALIZE =
            "\n" + "# Warning! If you change name of the rule, the automatic unloads/updates/deletes may stop to work.\n" +
                    "# use final name before first submit to remote vm\n" + "RULE trace Object.finalize\n" + "  CLASS ^java.lang.Object\n" +
                    "  METHOD " + "finalize\n" + "  IF TRUE\n" + "  DO System.out.println(\"Finalizing \" + $0)\n" + " ENDRULE";

    public BytemanTemplateMenuItem(final RSyntaxTextArea source) {
        super("byteman all finalizers");
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append(STATIC_OBJECTFINALIZE);
            }
        });
    }

}
