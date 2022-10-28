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
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class InitAddClassDialog extends JDialog {

    AddSingleFile addSingleClassPanel;
    AddSingleFile addSingleJar;

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

        init.setLayout(new GridLayout(2, 1));
        init.add(new JLabel("This will send command to VM, to init existing, but not yet used class"));
        initFqn = new JTextField(lastFqn);
        init.add(initFqn);

        addJar.setLayout(new GridLayout(2, 1));
        addJar.add(
                new JLabel(
                        "<html>This allows you to select JAR from HDD, and inject it to the running vm<br/>" +
                                "You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system, if remote system can not store, you must use<br/>" +
                                "legacy add single class "
                )
        );

        addClasses.setLayout(new GridLayout(1, 1));
        addClasses.add(
                new JLabel(
                        "<html>This allows you to select CLASSES from HDD, and inject it to the running vm<br/>" +
                                "Those classes will be packed to jar and sent." +
                                " You can then use the classes in it in overwritten classes<br/>" +
                                "Note, that jar is stored on remote system," +
                                " if remote system can not store, you must use legacy add single class "
                )
        );

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
            return new String[]{addSingleClassPanel.addSingleClassFqn.getText(), addSingleClassPanel.addSingleClassFile.getText()};
        } else {
            throw new RuntimeException();
        }
    }

    private static class AddSingleFile extends JPanel {
        protected final JTextField addSingleClassFqn;
        protected final JTextField addSingleClassFile;
        private final DocumentListener verifier;

        AddSingleFile(String lastFqn, String lastFile) {
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
            addSingleClassFile = new JTextField("");
            this.verifier = createListener();
            addSingleClassFile.getDocument().addDocumentListener(verifier);
            addSingleClassFile.setText(lastFile);
            this.add(selectSingleClass);
            this.add(addSingleClassFile);
            this.add(addSingleClassFqn);
        }

        protected DocumentListener createListener() {
            return new ClassVerifier(addSingleClassFile, addSingleClassFqn);
        }
    }

    private static class FileVerifier implements DocumentListener {
        protected final JTextField source;
        protected final JTextField target;

        FileVerifier(JTextField source, JTextField target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            verifySource(documentEvent);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            verifySource(documentEvent);
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            verifySource(documentEvent);
        }

        public boolean verifySource(DocumentEvent documentEvent) {
            File f = new File(source.getText());
            if (!f.exists()) {
                target.setText("file do not exists");
                return false;
            } else {
                if (f.isDirectory()) {
                    target.setText("is directory");
                    return false;
                }
                target.setText("ok");
                return true;
            }
        }
    }

    private static class JarVerifier extends FileVerifier {

        JarVerifier(JTextField source, JTextField target) {
            super(source, target);
        }

        public boolean verifySource(DocumentEvent documentEvent) {
            boolean intermezo = super.verifySource(documentEvent);
            if (intermezo) {
                File f = new File(source.getText());
                try {
                    JarFile jf = new JarFile(f);
                    int manfest = 0;
                    int others = 0;
                    for (Enumeration list = jf.entries(); list.hasMoreElements();) {
                        ZipEntry entry = (ZipEntry) list.nextElement();
                        if (entry.getName().contains("META-INF")) {
                            manfest++;
                        } else {
                            others++;
                        }
                    }
                    target.setText("contains " + others + " classes and " + manfest + " manifest items");
                    jf.close();
                    return true;
                } catch (Exception ex) {
                    target.setText(ex.getMessage());
                    Logger.getLogger().log(ex);
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private static class ClassVerifier extends FileVerifier {

        ClassVerifier(JTextField source, JTextField target) {
            super(source, target);
        }

        public boolean verifySource(DocumentEvent documentEvent) {
            boolean intermezo = super.verifySource(documentEvent);
            if (intermezo) {
                File f = new File(source.getText());
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
                        return true;
                    } catch (Exception ex) {
                        target.setText(ex.getMessage());
                        Logger.getLogger().log(ex);
                    }
                }
            }
            return false;
        }
    }

    private static class AddSingleJar extends AddSingleFile {

        AddSingleJar(String lastFqn, File lastAddFile) {
            super(lastFqn, lastAddFile.getAbsolutePath());

        }

        @Override
        protected DocumentListener createListener() {
            return new JarVerifier(addSingleClassFile, addSingleClassFqn);
        }
    }
}
