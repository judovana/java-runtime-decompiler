package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class Jasm2TempalteMenuItem extends JMenuItem {
    public Jasm2TempalteMenuItem(final RSyntaxTextArea source, String java) {
        super(java);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append("package  jrd/template;\n" + "\n" + "super public class HelloJasm\n" + "\tversion 52:0\n" + "{\n"
                        + "  public Method \"<init>\":\"()V\"\n" + "\tstack 1 locals 1\n" + "  {\n" + "\t\taload_0;\n"
                        + "\t\tinvokespecial\tMethod java/lang/Object.\"<init>\":\"()V\";\n" + "\t\treturn;\n" + "  }\n"
                        + "  public static Method start:\"()V\"\n" + "\tstack 2 locals 0\n" + "  {\n"
                        + "\t\tgetstatic\tField java/lang/System.out:\"Ljava/io/PrintStream;\";\n"
                        + "\t\tldc\tString \"Hello from jasm! See terminal!\";\n"
                        + "\t\tinvokevirtual\tMethod java/io/PrintStream.println:\"(Ljava/lang/String;)V\";\n"
                        + "\t\t//invokestatic\tMethod showJrdAbout:\"()V\";\n" + "\t\t//invokestatic\tMethod dunmpAgentVars:\"()V\";\n"
                        + "\t\tgetstatic\tField java/lang/System.out:\"Ljava/io/PrintStream;\";\n" + "\t\tldc\tString \"done\";\n"
                        + "\t\tinvokevirtual\tMethod java/io/PrintStream.println:\"(Ljava/lang/String;)V\";\n" + "\t\treturn;\n"
                        + "  }\n" + "  public static varargs Method main:\"([Ljava/lang/String;)V\"\n" + "\tstack 0 locals 1\n"
                        + "  {\n" + "\t\tinvokestatic\tMethod start:\"()V\";\n" + "\t\treturn;\n" + "  }\n"
                        + "  public static Method showJrdAbout:\"()V\"\n" + "\tstack 4 locals 0\n" + "  {\n"
                        + "\t\tnew\tclass org/jrd/frontend/frame/about/AboutView;\n" + "\t\tdup;\n" + "\t\taconst_null;\n"
                        + "\t\ticonst_0;\n"
                        + "\t\tinvokespecial\tMethod org/jrd/frontend/frame/about/AboutView.\"<init>\":\"(Ljavax/swing/JFrame;Z)V\";\n"
                        + "\t\ticonst_1;\n" + "\t\tinvokevirtual\tMethod org/jrd/frontend/frame/about/AboutView.setVisible:\"(Z)V\";\n"
                        + "\t\treturn;\n" + "  }\n" + "  public static Method dunmpAgentVars:\"()V\"\n" + "\tstack 1 locals 0\n"
                        + "  {\n" + "\t\tinvokestatic\tMethod org/jrd/agent/api/Variables.dumpAll:\"()Ljava/lang/String;\";\n"
                        + "\t\tpop;\n" + "\t\treturn;\n" + "  }\n" + "\n" + "} // end Class HelloJasm\n");
            }
        });
    }


}
