package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;
import org.objectweb.asm.ClassReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;

public class InitAddClassDialog extends JDialog {

    AddSingleClass addSingleClassPanel;

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

        addClass.setLayout(new GridLayout(3, 1));
        addClass.add(
                new JLabel(
                        "<html>This allows you to select single CLASS from HDD, and inject it to the running vm<br/>" +
                                "The classes must have all dependecnies in running vm already," +
                                " and are initiated by very fragile method<br/>" +
                                "In addition, this method works only for jdk11 and older."
                )
        );
        addSingleClassPanel = new AddSingleClass(lastFqn, lastAddFile.getAbsolutePath());
        addClass.add(addSingleClassPanel);

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
            return new String[]{addSingleClassPanel.addSingleClassFqn.getText(), addSingleClassPanel.addSingleClassFile.getText()};
        } else {
            throw new RuntimeException();
        }
    }

    private static class AddSingleClass extends JPanel {
        private final JTextField addSingleClassFqn;
        private final JTextField addSingleClassFile;

        AddSingleClass(String lastFqn, String lastFile) {
            this.setLayout(new GridLayout(3, 1));
            addSingleClassFqn = new JTextField(lastFqn);
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
            addSingleClassFile = new JTextField(lastFile);
            addSingleClassFile.getDocument().addDocumentListener(new InitAddClassVerifyListener(addSingleClassFile, addSingleClassFqn));
            this.add(selectSingleClass);
            this.add(addSingleClassFile);
            this.add(addSingleClassFqn);
        }
    }

    private static class InitAddClassVerifyListener implements DocumentListener {
        private final JTextField source;
        private final JTextField target;

        InitAddClassVerifyListener(JTextField source, JTextField target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            dome(documentEvent);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            dome(documentEvent);
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            dome(documentEvent);
        }

        public void dome(DocumentEvent documentEvent) {
            File f = new File(source.getText());
            if (!f.exists()) {
                target.setText("file do not exists");
            } else {
                byte[] b = null;
                try {
                    b = Files.readAllBytes(f.toPath());
                } catch (Exception ex) {
                    target.setText(ex.getMessage());
                    Logger.getLogger().log(ex);
                }
                if (b == null || b.length == 0) {
                    target.setText("is empty");
                } else {
                    try {
                        ClassReader cr = new ClassReader(b);
                        target.setText(cr.getClassName().replace('/', '.'));
                    } catch (Exception ex) {
                        target.setText(ex.getMessage());
                        Logger.getLogger().log(ex);
                    }
                }
            }
        }
    }
}
