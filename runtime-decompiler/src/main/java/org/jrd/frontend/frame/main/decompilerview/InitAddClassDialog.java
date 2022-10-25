package org.jrd.frontend.frame.main.decompilerview;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.io.File;

public class InitAddClassDialog extends JDialog {

    JTextField addText;
    JTextField addFile;
    JTextField initFqn;
    JTabbedPane tp = new JTabbedPane();
    JPanel init = new JPanel();
    JPanel add = new JPanel();
    int r = 0;

    public InitAddClassDialog(String lastFqn, String lastAdd, File lastAddFile) {
        this.add(tp);
        init.setName("Init");
        add.setName("Add");
        tp.add(init);
        tp.add(add);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModal(true);

        init.setLayout(new GridLayout(3, 1));
        init.add(new JLabel("This will send command to VM, to init existing, but not yet used class"));
        initFqn = new JTextField(lastFqn);
        init.add(initFqn);
        JPanel nice1 = new JPanel();
        JButton initB = new JButton("Init");
        initB.addActionListener(a -> {
            r = 1;
            InitAddClassDialog.this.setVisible(false);
        });
        JButton cancel1 = new JButton("Cancel");
        cancel1.addActionListener(a -> {
            r = 0;
            InitAddClassDialog.this.setVisible(false);
        });
        nice1.setLayout(new GridLayout(1, 2));
        nice1.add(initB);
        nice1.add(cancel1);
        init.add(nice1);

        add.setLayout(new GridLayout(6, 1));
        add.add(
                new JLabel(
                        "<html>This allows you to seelct class from HDD, and inject it to the running vm<br/>" +
                                "The class should be dependent only on already existing classes.<br/>" +
                                "You can then use the class in overwritten classes"
                )
        );
        addText = new JTextField(lastAdd);
        add.add(addText);
        add.add(new JButton("Select"));
        addFile = new JTextField(lastAddFile.getAbsolutePath());
        add.add(addFile);
        add.add(new JLabel("..verification.."));
        JButton addB = new JButton("Add");
        addB.addActionListener(a -> {
            r = 1;
            InitAddClassDialog.this.setVisible(false);
        });
        JButton cancel2 = new JButton("Cancel");
        cancel2.addActionListener(a -> {
            r = 0;
            InitAddClassDialog.this.setVisible(false);
        });
        JPanel nice2 = new JPanel();
        nice2.setLayout(new GridLayout(1, 2));
        nice2.add(addB);
        nice2.add(cancel2);
        add.add(nice2);

        this.pack();
        this.setLocationRelativeTo(null);
    }

    public String[] showAndGet() {
        this.setVisible(true);
        if (r != 1) {
            return null;
        }
        if (tp.getSelectedComponent() == init) {
            return new String[]{initFqn.getText()};
        } else if (tp.getSelectedComponent() == add) {
            return new String[]{addText.getText(), addFile.getText()};
        } else {
            throw new RuntimeException();
        }
    }
}
