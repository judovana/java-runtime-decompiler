package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BytemanTempalteMenuItem extends JMenuItem {
    public BytemanTempalteMenuItem(final RSyntaxTextArea source, String java) {
        super(java);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append(
                        "\n  RULE trace Object.finalize\n" + "  CLASS ^java.lang.Object\n" + "  METHOD finalize\n" + "  IF TRUE\n" +
                                "  DO System.out.println(\"Finalizing \" + $0)\n" + "  ENDRULE"
                );
            }
        });
    }

}
