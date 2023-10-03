package org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class JasmTemplateMenuItem extends JMenuItem {
    public JasmTemplateMenuItem(final RSyntaxTextArea source) {
        super("jasm hello world");
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append(
                        "\npackage  jrd/template;\n" + "\n" + "super public class HelloJasm\n" + "\tversion 52:0\n" + "{\n" +
                                "  public Method \"<init>\":\"()V\"\n" + "\tstack 1 locals 1\n" + "  {\n" + "\t\taload_0;\n" +
                                "\t\tinvokespecial\tMethod java/lang/Object.\"<init>\":\"()V\";\n" + "\t\treturn;\n" + "  }\n" +
                                "  public static Method start:\"()V\"\n" + "\tstack 2 locals 0\n" + "  {\n" +
                                "\t\tgetstatic\tField java/lang/System.out:\"Ljava/io/PrintStream;\";\n" +
                                "\t\tldc\tString \"hello on stdout. See terminal!\";\n" +
                                "\t\tinvokevirtual\tMethod java/io/PrintStream.println:\"(Ljava/lang/String;)V\";\n" + "\t\treturn;\n" +
                                "  }\n" + "  public static Method main:\"([Ljava/lang/String;)V\"\n" + "\tstack 0 locals 1\n" + "  {\n" +
                                "\t\tinvokestatic\tMethod start:\"()V\";\n" + "\t\treturn;\n" + "  }\n" + "\n" +
                                "} // end Class HelloJasm\n"
                );
            }
        });
    }

}
