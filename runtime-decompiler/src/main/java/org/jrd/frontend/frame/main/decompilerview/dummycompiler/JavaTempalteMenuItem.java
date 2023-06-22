package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class JavaTempalteMenuItem extends JMenuItem {
    public JavaTempalteMenuItem(final RSyntaxTextArea source, String java) {
        super(java);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append("\n" + "package jrd.template;\n\nimport java.awt.Color;\n" + "import java.awt.Dimension;\n" + "import "
                        + "java" + ".awt.FlowLayout;\n" + "\n" + "import javax.swing.BorderFactory;\n" + "import javax.swing.JFrame;\n"
                        + "import javax.swing.JLabel;\n" + "import javax.swing.border.Border;\n"
                        + "import javax.swing.SwingUtilities;\n" + "\n" + "public class HelloWorldSwing {\n" + "\n"
                        + "    public static void start() {\n" + "        JFrame jFrame = new JFrame(\"Hello World Swing Example\");\n"
                        + "        jFrame.setLayout(new FlowLayout());\n" + "        jFrame.setSize(500, 360);\n"
                        + "        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n" + "\n" + "\n"
                        + "        JLabel label = new JLabel(\"Hello World Swing\");\n"
                        + "        Border border = BorderFactory.createLineBorder(Color.BLACK);\n"
                        + "        label.setBorder(border);\n" + "        label.setPreferredSize(new Dimension(150, 100));\n" + "\n"
                        + "        label.setText(\"Hello World Swing\");\n" + "        label.setHorizontalAlignment(JLabel.CENTER);\n"
                        + "        label.setVerticalAlignment(JLabel.CENTER);\n" + "\n" + "        jFrame.add(label);\n"
                        + "        jFrame.setVisible(true);\n" + "    }\n" + "\n" + "    public static void main(String[] args) {\n"
                        + "    \t    SwingUtilities.invokeLater(new Runnable() {\n" + "         @Override\n"
                        + "         public void run() {\n" + "             start();\n" + "          }\n" + "         });\n" + "    }\n"
                        + "}");
            }
        });
    }


}
