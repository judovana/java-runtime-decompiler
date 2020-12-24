package org.jrd.frontend.MainFrame;

import org.jc.api.ClassIdentifier;
import org.jc.api.ClassesProvider;
import org.jc.api.IdentifiedBytecode;
import org.jc.api.IdentifiedSource;
import org.jc.api.InMemoryCompiler;
import org.jc.api.MessagesListener;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;

public class RewriteClassDialog extends JDialog {

    private static final String[] saveOptions = new String[]{"fully qualified name", "src subdirectories name", "custom name"};
    private final JTabbedPane dualpane;

    private final JPanel currentBufferPane;
    private final JLabel currentClass;
    private final JButton selectSrcTarget;
    private final JTextField futureSrcTarget;
    private final JComboBox<String> namingSource;
    private final JButton selectBinTarget;
    private final JTextField futureBinTarget;
    private final JComboBox<String> namingBinary;
    private final JButton saveSrcBuffer;
    private final JButton compileAndSave;
    private final JButton compileAndUpload;
    private final JTextField status;

    private final JPanel manualPane;
    private final JPanel inputs;
    private final JPanel buttons;
    private final JLabel validation;
    private final JTextField filePath;
    private final JTextField className;
    private final JButton selectSrc;
    private final JLabel nothing;
    private final JButton ok;
    private boolean wasOkPressed;

    private final String origName;
    private final String origBuffer;
    private final VmInfo vmInfo;
    private final VmManager vmManager;

    public RewriteClassDialog(final String name, final String lastLoad, final String currentBuffer, final String lastSaveSrc, final String lastSaveBin, VmInfo vmInfo, VmManager vmManager) {
        super((JFrame) null, "Specify class and selectSrc its bytecode", true);
        this.setSize(400, 400);
        this.setLayout(new BorderLayout());

        this.origName = name;
        this.origBuffer = currentBuffer;
        this.vmInfo = vmInfo;
        this.vmManager = vmManager;

        dualpane = new JTabbedPane();

        currentBufferPane = new JPanel();
        currentBufferPane.setName("Current buffer");
        currentBufferPane.setLayout(new GridLayout(0, 1));
        status = new JTextField();
        status.setEditable(false);
        if (origBuffer == null || origBuffer.length() == 0) {
            currentClass = new JLabel(origName + " !!MISSING!!");
        } else {
            currentClass = new JLabel(origName + " - " + origBuffer.length() + " chars");
        }
        saveSrcBuffer = new JButton("Save current buffer");
        compileAndSave = new JButton("Compile and save as");
        namingBinary = new JComboBox<String>(saveOptions);
        namingSource = new JComboBox<String>(saveOptions);
        namingBinary.setSelectedIndex(0);
        namingBinary.setSelectedIndex(1);
        futureBinTarget = new JTextField(lastSaveBin);
        futureSrcTarget = new JTextField(lastSaveSrc);
        selectBinTarget = new JButton("...");
        selectSrcTarget = new JButton("...");
        compileAndUpload = new JButton("Compile and directly upload");
        compileAndUpload.setFont(compileAndSave.getFont().deriveFont(Font.BOLD));

        manualPane = new JPanel();
        manualPane.setName("Manual uplaod from file");
        manualPane.setLayout(new BorderLayout());
        inputs = new JPanel(new GridLayout(3, 1));
        buttons = new JPanel(new GridLayout(3, 1));
        validation = new JLabel("???");
        filePath = new JTextField(lastLoad);
        className = new JTextField(name);
        selectSrc = new JButton("...");
        nothing = new JLabel();
        ok = new JButton("ok");
        wasOkPressed = false;
        setLocation(ScreenFinder.getCurrentPoint());
        setValidation();
        setSelectListener();
        setOkListener();
        adds();

        saveSrcBuffer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveByGui(futureSrcTarget.getText(), namingSource.getSelectedIndex(), ".java", status, origName, origBuffer.getBytes());

            }
        });
    }

    private static boolean saveByGui(String fileNameBase, int naming, String suffix, JTextField status, String clazz, byte[] content) {
        String name = "???";
        String ss = "Error to save: ";
        boolean r = true;
        try {
            name = cheatName(fileNameBase, naming, suffix, clazz);
            File f = new File(name);
            if (naming == 1) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), content);
            ss = "Saved: ";
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage());
            r = false;
        }
        status.setText(ss + name);
        return r;
    }

    private static String cheatName(String base, int selectedIndex, String suffix, String fullyClasifiedName) {
        if (selectedIndex == 2) {
            return base;
        }
        if (selectedIndex == 0) {
            return base + "/" + fullyClasifiedName + suffix;
        }
        if (selectedIndex == 1) {
            return base + "/" + fullyClasifiedName.replaceAll("\\.", "/") + suffix;
        }
        throw new RuntimeException("Unknown name target " + selectedIndex);
    }

    private void setValidation() {
        DocumentListener v = new FiletoClassValidator(validation, filePath, className);
        filePath.getDocument().addDocumentListener(v);
        className.getDocument().addDocumentListener(v);
        v.changedUpdate(null);
    }

    private void setSelectListener() {
        selectSrc.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(filePath.getText());
            jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = jf.showOpenDialog(selectSrc);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                filePath.setText(jf.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private void setSelectSaveListenrListener() {
        selectSrcTarget.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(futureSrcTarget.getText());
            if (namingSource.getSelectedIndex() < 2) {
                jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            } else {
                jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
            }
            int returnVal = jf.showOpenDialog(selectSrcTarget);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                futureSrcTarget.setText(jf.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private void setSelectSaveBinListener() {
        selectBinTarget.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(futureBinTarget.getText());
            if (namingBinary.getSelectedIndex() < 2) {
                jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            } else {
                jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
            }
            int returnVal = jf.showOpenDialog(selectBinTarget);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                futureBinTarget.setText(jf.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private void setOkListener() {
        ok.addActionListener(e -> {
            this.wasOkPressed = true;
            this.setVisible(false);
        });

        compileAndSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CompilationWithResult compiler = xompileWithGui();
                if (compiler.ex == null && compiler.result == null) {
                    String s = "No output from compiler, maybe still running?";
                    JOptionPane.showMessageDialog(null, s);
                    status.setText(s);
                } else if (compiler.ex != null) {
                    JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                    status.setText("Failed - " + compiler.ex.getMessage());
                } else if (compiler.result != null) {
                    status.setText("something done, will save now");
                    status.repaint();
                    if (namingBinary.getSelectedIndex() == 2) {
                        if (compiler.result.size() != 1) {
                            String s = "Output of compilation was " + compiler.result.size() + "classes. Can not save more then one file to exact filename";
                            JOptionPane.showMessageDialog(null, s);
                            status.setText(s);
                            return;
                        }
                    }
                    int saved = 0;
                    for (IdentifiedBytecode clazz : compiler.result) {
                        boolean r = saveByGui(futureBinTarget.getText(), namingBinary.getSelectedIndex(), ".class", status, clazz.getClassIdentifier().getFullName(), clazz.getFile());
                        if (r) {
                            saved++;
                        }
                    }
                    if (compiler.result.size() > 1) {
                        if (saved == compiler.result.size()) {
                            status.setText("Saved all " + saved + "classes to" + futureBinTarget.getText());
                        } else {
                            status.setText("Saved only " + saved + " from total of " + compiler.result.size() + " classes to" + futureBinTarget.getText());
                        }
                    }
                } else {
                    status.setText("Really weird state, report bug how to achieve this");
                }
            }
        });
    }


    private CompilationWithResult xompileWithGui() {
        ClassesProvider cp = new RuntimeCompilerConnector.JRDClassesProvider(vmInfo, vmManager);
        InMemoryCompiler rc = new RuntimeCompilerConnector.DummyRuntimeCompiler();
        JDialog compialtionRunningDialog = new JDialog(RewriteClassDialog.this, "Compiling", true);
        JTextArea compilationLog = new JTextArea();
        compialtionRunningDialog.setSize(300, 400);
        compialtionRunningDialog.add(new JScrollPane(compilationLog));
        CompilationWithResult compiler = new CompilationWithResult(rc, cp, compilationLog);
        Thread t = new Thread(compiler);
        t.start();
        compialtionRunningDialog.setLocationRelativeTo(RewriteClassDialog.this);
        compialtionRunningDialog.setVisible(true);
        return compiler;
    }

    private void adds() {
        inputs.add(filePath);
        inputs.add(className);
        inputs.add(className);
        inputs.add(validation);
        buttons.add(selectSrc);
        buttons.add(nothing);
        buttons.add(ok);
        manualPane.add(inputs);
        manualPane.add(buttons, BorderLayout.EAST);

        currentBufferPane.add(currentClass);
        JPanel p11 = new JPanel(new BorderLayout());
        JPanel p12 = new JPanel(new GridLayout(1, 2));
        p11.add(selectSrcTarget, BorderLayout.WEST);
        p11.add(p12);
        p12.add(futureSrcTarget);
        p12.add(namingSource);
        currentBufferPane.add(p11);
        currentBufferPane.add(saveSrcBuffer);
        JPanel p21 = new JPanel(new BorderLayout());
        JPanel p22 = new JPanel(new GridLayout(1, 2));
        p21.add(selectBinTarget, BorderLayout.WEST);
        p21.add(p22);
        p22.add(futureBinTarget);
        p22.add(namingBinary);
        currentBufferPane.add(p21);
        currentBufferPane.add(compileAndSave);
        currentBufferPane.add(compileAndUpload);
        currentBufferPane.add(status);
        dualpane.add(currentBufferPane);
        dualpane.add(manualPane);
        this.add(dualpane);
        this.pack();
    }

    public boolean isOkPressed() {
        return this.wasOkPressed;
    }

    public String getClassName() {
        return this.className.getText();
    }

    public String getLoadFilePath() {
        return this.filePath.getText();
    }

    public String getSaveSrcPath() {
        return this.futureSrcTarget.getText();
    }

    public String getSaveBinPath() {
        return this.futureBinTarget.getText();
    }

    private class CompilationWithResult implements Runnable {
        private final InMemoryCompiler rc;
        private final ClassesProvider cp;
        private final JTextArea compilationLog;
        private Exception ex;
        private Collection<IdentifiedBytecode> result;


        public CompilationWithResult(InMemoryCompiler rc, ClassesProvider cp, JTextArea compilationLog) {
            this.rc = rc;
            this.cp = cp;
            this.compilationLog = compilationLog;
        }

        @Override
        public void run() {
            try {
                result = rc.compileClass(cp, Optional.of(new MessagesListener() {
                    @Override
                    public void addMessage(Level level, String s) {
                        OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, s);
                        compilationLog.setText(compilationLog.getText() + s + "\n");
                    }
                }), new IdentifiedSource(new ClassIdentifier(origName), origBuffer.getBytes(), Optional.empty()));
            } catch (Exception ex) {
                this.ex = ex;
                ex.printStackTrace();
                compilationLog.setText(compilationLog.getText() + ex.getMessage() + "\n");
            } finally {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "Operation finished");
                compilationLog.setText(compilationLog.getText() + "Operatin finished, you may close dialog\n");
            }

        }
    }
}
