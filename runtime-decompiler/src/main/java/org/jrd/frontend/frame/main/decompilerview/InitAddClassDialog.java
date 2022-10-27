package org.jrd.frontend.frame.main.decompilerview;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class InitAddClassDialog extends JDialog {

    JTextField addSingleClassFqn;
    JTextField addSingleClassFile;
    JTextField initFqn;
    JTabbedPane tp = new JTabbedPane();
    JPanel init = new JPanel();
    JPanel addJar = new JPanel();
    JPanel addClasses = new JPanel();
    JPanel addClass = new JPanel();
    int r = 0;

    public InitAddClassDialog(String lastFqn, String lastAdd, File lastAddFile) {
        this.add(tp);
        init.setName("Init");
        addJar.setName("Add Jar");
        addClasses.setName("Add classes");
        addClass.setName("Add single class");
        tp.add(init);
        tp.add(addJar);
        tp.add(addClasses);
        tp.add(addClass);

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

        addJar.setLayout(new GridLayout(6, 1));
        addJar.add(
                new JLabel(
                        "<html>This allows you to select JAR from HDD, and inject it to the running vm<br/>" +
                                "You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system, if remote system can not store, you must use<br/>" +
                                "legacy add single class "
                )
        );

        addClasses.setLayout(new GridLayout(2, 1));
        addClasses.add(
                new JLabel(
                        "<html>This allows you to select CLASSES from HDD, and inject it to the running vm<br/>" +
                                "Those classes will be packed to jar and sent." +
                                " You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system," +
                                " if remote system can not store, you must use legacy add single class "
                )
        );

        addClass.setLayout(new GridLayout(6, 1));
        addClass.add(
                new JLabel(
                        "<html>This allows you to select single CLASS from HDD, and inject it to the running vm<br/>" +
                                "The classes must have all dependecnies in running vm already," +
                                " and are initiated by very fragile method<br/>" +
                                "In addition, this method works only for jdk11 and older."
                )
        );
        addSingleClassFqn = new JTextField(lastAdd);
        addClass.add(addSingleClassFqn);
        JButton selectSingleClass = new JButton("Select");
        selectSingleClass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jf = new JFileChooser(addSingleClassFile.getText());
                int rr = jf.showOpenDialog(selectSingleClass);
                jf.setMultiSelectionEnabled(false);
                if (rr == JFileChooser.APPROVE_OPTION && jf.getSelectedFile() != null) {
                    String q = jf.getSelectedFile().getAbsolutePath();
                    addSingleClassFile.setText(q);
                }
            }
        });
        addClass.add(selectSingleClass);
        addSingleClassFile = new JTextField(lastAddFile.getAbsolutePath());
        addClass.add(addSingleClassFile);
        addClass.add(new JLabel("..verification.."));
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
        addClass.add(nice2);

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
        } else if (tp.getSelectedComponent() == addClass) {
            return new String[]{addSingleClassFqn.getText(), addSingleClassFile.getText()};
        } else {
            throw new RuntimeException();
        }
    }
}
