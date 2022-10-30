package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.frontend.frame.main.decompilerview.verifiers.ClassVerifier;
import org.jrd.frontend.frame.main.decompilerview.verifiers.FileVerifier;
import org.jrd.frontend.frame.main.decompilerview.verifiers.GetSetText;
import org.jrd.frontend.frame.main.decompilerview.verifiers.JarVerifier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InitAddClassDialog extends JDialog {

    AddSingleFile addSingleClassPanel;
    AddSingleFile addSingleJar;

    MyJTextField initFqn;
    JCheckBox jarBoot = new JCheckBox("Force to boot loader instead of application one (eg java.lang...)");

    JCheckBox classesBoot = new JCheckBox("Force to boot loader instead of application one (eg java.lang...)");
    String lastClassesSelection;
    JPanel addClassesGuis = new JPanel(new GridLayout(0, 1));

    JTabbedPane tp = new JTabbedPane();
    JPanel init = new JPanel();
    JPanel addJar = new JPanel();
    JPanel addClasses = new JPanel();
    JPanel addClass = new JPanel();
    int r = 0;

    public InitAddClassDialog(String lastFqn, String lastAdd, File lastAddFile) {
        this.add(tp);
        lastClassesSelection = lastAddFile.getAbsolutePath();
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

        init.setLayout(new GridLayout(2, 1));
        init.add(new JLabel("This will send command to VM, to init existing, but not yet used class"));
        initFqn = new MyJTextField(lastFqn);
        init.add(initFqn);

        addJar.setLayout(new GridLayout(3, 1));
        addJar.add(
                new JLabel(
                        "<html>This allows you to select JAR from HDD, and inject it to the running vm<br/>" +
                                "You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system, if remote system can not store, you must use<br/>" +
                                "legacy add single class "
                )
        );
        addJar.add(jarBoot);
        addClasses.setLayout(new BorderLayout());
        JPanel addClassesTopPanel = new JPanel(new GridLayout(3, 1));
        addClassesTopPanel.add(
                new JLabel(
                        "<html>This allows you to select CLASSES from HDD, and inject it to the running vm<br/>" +
                                "Those classes will be packed to jar and sent." +
                                " You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system," +
                                " if remote system can not store, you must use legacy add single class "
                )
        );
        addClassesTopPanel.add(classesBoot);
        JButton addClassButton = new JButton("+");
        addClassButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jf = new JFileChooser(lastClassesSelection);
                jf.setMultiSelectionEnabled(true);
                int rr = jf.showOpenDialog(addClassButton);
                if (rr == JFileChooser.APPROVE_OPTION && jf.getSelectedFile() != null) {
                    addClassesGuis.setLayout(new GridLayout(addSingleClassPanel.getComponentCount() + jf.getSelectedFiles().length, 1));
                    for (File f : jf.getSelectedFiles()) {
                        lastClassesSelection = f.getParent();
                        String q = f.getAbsolutePath();
                        addClassesGuis.add(new AddMultiFilePart("", q));
                    }
                    InitAddClassDialog.this.pack();
                }
            }
        });
        addClassesTopPanel.add(addClassButton);
        addClasses.add(addClassesTopPanel, BorderLayout.NORTH);
        addClasses.add(new JScrollPane(addClassesGuis));
        addClass.setLayout(new GridLayout(2, 1));
        addClass.add(
                new JLabel(
                        "<html>This allows you to select single CLASS from HDD, and inject it to the running vm<br/>" +
                                "The classes must have all dependecnies in running vm already," +
                                " and are initiated by very fragile method<br/>" +
                                "In addition, this method works only for jdk11 and older."
                )
        );
        addSingleClassPanel = new AddSingleFile(lastFqn, lastAddFile.getAbsolutePath());
        addClass.add(addSingleClassPanel);
        addSingleJar = new AddSingleJar(lastFqn, lastAddFile);
        addJar.add(addSingleJar);

        JButton addB = new JButton("Add/Init");
        addB.addActionListener(a -> {
            r = 1;
            InitAddClassDialog.this.setVisible(false);
        });
        JButton cancel2 = new JButton("Cancel");
        cancel2.addActionListener(a -> {
            r = 0;
            InitAddClassDialog.this.setVisible(false);
        });
        JPanel okCancelButtonsPane = new JPanel();
        okCancelButtonsPane.setLayout(new GridLayout(1, 2));
        okCancelButtonsPane.add(addB);
        okCancelButtonsPane.add(cancel2);
        this.add(okCancelButtonsPane, BorderLayout.SOUTH);

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
            MyJTextField fakeReply = new MyJTextField();
            boolean verified = new ClassVerifier(addSingleClassPanel.addSingleClassFile, fakeReply).verifySource(null);
            if (!verified) {
                throw new RuntimeException(fakeReply.getText());
            }
            return new String[]{addSingleClassPanel.addSingleClassFqn.getText(), addSingleClassPanel.addSingleClassFile.getText()};
        } else if (tp.getSelectedComponent() == addJar) {
            boolean verified = addSingleJar.verifier.verifySource(null);
            if (!verified) {
                throw new RuntimeException(addSingleJar.addSingleClassFqn.getText());
            }
            return new String[]{jarBoot.isSelected() + "", new File(addSingleJar.addSingleClassFile.getText()).getName(),
                    new File(addSingleJar.addSingleClassFile.getText()).getAbsolutePath()};
        } else if (tp.getSelectedComponent() == addClasses) {
            List<String> futureArray = new ArrayList<>();
            futureArray.add(classesBoot.isSelected() + "");
            futureArray.add("header");
            futureArray.add("header");
            futureArray.add("header");
            Component[] fqnAndFile = addClassesGuis.getComponents();
            if (fqnAndFile.length == 0) {
                throw new RuntimeException("No classes to add");
            }
            for (Component c : fqnAndFile) {
                AddSingleFile addSingleFile = (AddSingleFile) c;
                boolean verified = addSingleFile.verifier.verifySource(null);
                if (!verified) {
                    throw new RuntimeException(addSingleFile.addSingleClassFqn.getText());
                }
                futureArray.add(addSingleFile.addSingleClassFqn.getText());
                futureArray.add(addSingleFile.addSingleClassFile.getText());
            }
            return futureArray.toArray(String[]::new);
        } else {
            throw new RuntimeException("unsupported tab");
        }
    }

    private static class AddMultiFilePart extends AddSingleFile {

        AddMultiFilePart(String lastFqn, String lastFile) {
            super(lastFqn, lastFile);
            this.setLayout(new GridLayout(4, 1));
            JButton removeB = new JButton("-^-Remove-^-");
            removeB.addActionListener(a -> {
                Container p = AddMultiFilePart.this.getParent();
                AddMultiFilePart.this.getParent().remove(AddMultiFilePart.this);
                p.repaint();
            });
            this.add(removeB);
        }
    }

    private static class AddSingleFile extends JPanel {
        protected final MyJTextField addSingleClassFqn;
        protected final MyJTextField addSingleClassFile;
        private final FileVerifier verifier;

        AddSingleFile(String lastFqn, String lastFile) {
            this.setLayout(new GridLayout(3, 1));
            addSingleClassFqn = new MyJTextField(lastFqn);
            JButton selectSingleClass = new JButton("-ˇ-Select-ˇ-");
            selectSingleClass.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JFileChooser jf = new JFileChooser(addSingleClassFile.getText());
                    jf.setMultiSelectionEnabled(false);
                    int rr = jf.showOpenDialog(selectSingleClass);
                    if (rr == JFileChooser.APPROVE_OPTION && jf.getSelectedFile() != null) {
                        String q = jf.getSelectedFile().getAbsolutePath();
                        addSingleClassFile.setText(q);
                    }
                }
            });
            addSingleClassFile = new MyJTextField("");
            this.verifier = createListener();
            addSingleClassFile.getDocument().addDocumentListener(verifier);
            addSingleClassFile.setText(lastFile);
            this.add(selectSingleClass);
            this.add(addSingleClassFile);
            this.add(addSingleClassFqn);
        }

        protected FileVerifier createListener() {
            return new ClassVerifier(addSingleClassFile, addSingleClassFqn);
        }
    }

    private static class MyJTextField extends JTextField implements GetSetText {

        MyJTextField() {
        }

        MyJTextField(String s) {
            super(s);
        }
    }

    private static class AddSingleJar extends AddSingleFile {

        AddSingleJar(String lastFqn, File lastAddFile) {
            super(lastFqn, lastAddFile.getAbsolutePath());

        }

        @Override
        protected FileVerifier createListener() {
            return new JarVerifier(addSingleClassFile, addSingleClassFqn);
        }
    }
}
