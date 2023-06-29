package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class JavaTempalteMenuItem extends JMenuItem {
    public JavaTempalteMenuItem(final RSyntaxTextArea source) {
        super("java swing hello with dependence example");
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                source.append(
                        "\n" + "\n" + "package jrd.template;\n" + "\n" + "import java.awt.Color;\n" + "import java.awt.Dimension;\n" +
                                "import java.awt.FlowLayout;\n" + "import java.awt.event.ActionEvent;\n" +
                                "import java.awt.event.ActionListener;\n" + "\n" + "import javax.swing.BorderFactory;\n" +
                                "import javax.swing.JFrame;\n" + "import javax.swing.JButton;\n" + "import javax.swing.JLabel;\n" +
                                "import javax.swing.border.Border;\n" + "import javax.swing.SwingUtilities;\n" + "\n" +
                                "public class HelloWorldSwing {\n" + "\n" + "    public static void start() {\n" +
                                "        System.out.println(\"start!\");\n" +
                                "        JFrame jFrame = new JFrame(\"Hello World Swing Example\");\n" +
                                "        jFrame.setLayout(new FlowLayout());\n" + "        jFrame.setSize(500, 360);\n" +
                                "        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n" + "\n" + "\n" +
                                "        JButton label = new JButton(\"Hello World Swing\");\n" +
                                "        Border border = BorderFactory.createLineBorder(Color.BLACK);\n" +
                                "        label.setBorder(border);\n" + "        label.setPreferredSize(new Dimension(150, 100));\n" +
                                "        label.setHorizontalAlignment(JLabel.CENTER);\n" +
                                "        label.setVerticalAlignment(JLabel.CENTER);\n" +
                                "        label.addActionListener(new ActionListener() {\n" + "                    @Override\n" +
                                "                    public void actionPerformed(ActionEvent actionEvent) {\n" +
                                "                        System.out.println(\"Pressed to stdout! See terminal!\");\n" +
                                "                        // should cause failure with no cp\n" +
                                "                        // should pass if you run agasint artificial CP with runtime-decompiler.jar\n" +
                                "                        // may, or may not (if not, INIT this class) against runtime CP\n" +
                                "                        //new org.jrd.frontend.frame.about.AboutView(null, false).setVisible(true);\n" +
                                "                        // Agent class is not in runtime-decompiler.jar\n" +
                                "                        // but should be always reachable once you connect agent to JRD itself\n" +
                                "                        //org.jrd.agent.api.Variables.dumpAll();\n" +
                                "                        System.out.println(\"done\");\n" + "                    }\n" +
                                "                });\n" + "\n" + "        jFrame.add(label);\n" + "        jFrame.setVisible(true);\n" +
                                "    }\n" + "\n" + "    public static void main(String[] args) {\n\t    System.out.println(\"main!\");\n" +
                                "    \t    SwingUtilities.invokeLater(new Runnable() {\n" + "         @Override\n" +
                                "         public void run() {\n" + "             start();\n" + "          }\n" + "         });\n" +
                                "    }\n" + "}"
                );
            }
        });
    }

}
