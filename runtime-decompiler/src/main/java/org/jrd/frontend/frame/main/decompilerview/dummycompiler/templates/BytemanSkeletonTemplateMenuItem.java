package org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class BytemanSkeletonTemplateMenuItem extends JMenuItem {

    private static final String DYNAMIC_SKELETON =
            "\n" + "# Warning! If you change name of the rule, the automatic unloads/updates/deletes may stop to work.\n"
                    + "# use final name before first submit to remote vm\n" + "\n" + "# rule skeleton\n" + "RULE <unique rule name>\n"
                    + "  CLASS <class name>\n" + "  METHOD <method name>\n" + "  BIND <bindings>\n" + "  IF  <condition>\n"
                    + "  DO  <actions>\n" + " ENDRULE";

    public BytemanSkeletonTemplateMenuItem(final RSyntaxTextArea source) {
        super("byteman skeleton");
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append(getDynamicSkeleton(null));
            }
        });
    }

    public static String getDynamicSkeleton(String clazz) {
        if (clazz == null) {
            return DYNAMIC_SKELETON;
        } else {
            return DYNAMIC_SKELETON.replace("<class name>", clazz).replace("<unique rule name>", clazz+".unique.rule.name");
        }
    }
}
