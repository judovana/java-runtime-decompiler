package org.jrd.frontend.frame.overwrite;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.DecompilationController;
import org.jrd.frontend.utility.CommonUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;


public class OverwriteClassDialog extends JDialog {

    private static class TextFieldBasedStus implements CommonUtils.StatusKeeper {
        private final JTextField status;

        TextFieldBasedStus(JTextField status) {
            this.status = status;
        }

        @Override
        public void setText(String s) {
            status.setText(s);
        }

        @Override
        public void onException(Exception ex) {
            Logger.getLogger().log(ex);
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    private static final String[] SAVE_OPTIONS = new String[]{
            "fully qualified name",
            "src subdirectories name",
            "custom name"
    };
    private final JTabbedPane dualPane;

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
    private final JTextField statusCompileCurrentBuffer;

    private final JPanel manualPane;
    private final JPanel inputs;
    private final JPanel buttons;
    private final JLabel validation;
    private final JTextField filePath;
    private final JTextField className;
    private final JButton selectSrc;
    private final JLabel nothing;
    private final JButton ok;
    private final PluginManager pluginManager;
    private final DecompilerWrapper decompiler;
    private boolean haveCompiler;

    private final JPanel externalFiles;
    private final JTextField filesToCompile;
    private final JButton selectExternalFiles;
    private final JCheckBox recursive;
    private final JTextField outputExternalFilesDir;
    private final JComboBox<String> namingExternal;
    private final JButton selectExternalFilesSave;
    private final JButton compileExternalFiles;
    private final JButton compileExternalFilesAndUpload;
    private final JTextField statusExternalFiles;

    private final JPanel binaryView;
    private final JLabel binaryFilename;
    private final JComboBox<String> namingBinaryView;
    private final JTextField outputBinaries;
    private final JButton selectBinary;
    private final JButton saveBinary;
    private final JButton uploadBinary;
    private final JTextField statusBinary;

    private final String origName;
    private final String origBuffer;
    private final byte[] origBin;
    private final VmInfo vmInfo;
    private final VmManager vmManager;

    public OverwriteClassDialog(
            final String name,
            final LatestPaths latestPaths,
            final String currentBuffer,
            final byte[] cBinBuffer,
            VmInfo vmInfo,
            VmManager vmManager,
            PluginManager pluginManager,
            DecompilerWrapper selectedDecompiler,
            boolean isBinaryVisible
    ) {
        super((JFrame) null, "Specify class and selectSrc its bytecode", true);
        this.setSize(400, 400);
        this.setLayout(new BorderLayout());

        this.origName = name;
        this.origBuffer = currentBuffer;
        this.origBin = Arrays.copyOf(cBinBuffer, cBinBuffer.length);
        this.vmInfo = vmInfo;
        this.vmManager = vmManager;

        dualPane = new JTabbedPane();

        currentBufferPane = new JPanel();
        currentBufferPane.setName("Current source buffer");
        currentBufferPane.setLayout(new GridLayout(0, 1));
        statusCompileCurrentBuffer = new JTextField();
        statusCompileCurrentBuffer.setEditable(false);
        if (origBuffer == null || origBuffer.length() == 0) {
            currentClass = new JLabel(origName + " !!MISSING!!");
        } else {
            currentClass = new JLabel(origName + " - " + origBuffer.length() + " chars");
        }
        saveSrcBuffer = new JButton("Save current buffer");
        compileAndSave = new JButton("Compile and save as");
        namingBinary = new JComboBox<String>(SAVE_OPTIONS);
        namingSource = new JComboBox<String>(SAVE_OPTIONS);
        namingSource.setSelectedIndex(CommonUtils.FULLY_QUALIFIED_NAME);
        namingBinary.setSelectedIndex(CommonUtils.SRC_SUBDIRS_NAME);
        futureBinTarget = new JTextField(latestPaths.getLastSaveBin());
        futureSrcTarget = new JTextField(latestPaths.getLastSaveSrc());
        selectBinTarget = new JButton("...");
        selectSrcTarget = new JButton("...");
        compileAndUpload = new JButton("Compile " + origName + "and directly upload to " + vmInfo.getVmId());
        compileAndUpload.setFont(compileAndSave.getFont().deriveFont(Font.BOLD));

        manualPane = new JPanel();
        manualPane.setName("Manual upload from file");
        manualPane.setLayout(new BorderLayout());
        inputs = new JPanel(new GridLayout(3, 1));
        buttons = new JPanel(new GridLayout(3, 1));
        validation = new JLabel("???");
        filePath = new JTextField(latestPaths.getLastManualUpload());
        className = new JTextField(origName);
        selectSrc = new JButton("...");
        nothing = new JLabel();
        ok = new JButton("upload to vm - " + vmInfo.getVmId());

        externalFiles = new JPanel(new GridLayout(0, 1));
        externalFiles.setName("Compile external files");
        externalFiles.add(new JLabel("Select external files to compile against runtime classpath"), BorderLayout.NORTH);
        JPanel exFilesIn = new JPanel(new BorderLayout());
        filesToCompile = new JTextField(latestPaths.getFilesToCompile());
        exFilesIn.add(filesToCompile, BorderLayout.CENTER);
        recursive = new JCheckBox("recursive");
        exFilesIn.add(recursive, BorderLayout.WEST);
        selectExternalFiles = new JButton("...");
        exFilesIn.add(selectExternalFiles, BorderLayout.EAST);
        externalFiles.add(exFilesIn);
        outputExternalFilesDir = new JTextField(latestPaths.getOutputExternalFilesDir());
        namingExternal = new JComboBox<>(SAVE_OPTIONS);
        namingExternal.setSelectedIndex(CommonUtils.SRC_SUBDIRS_NAME);
        selectExternalFilesSave = new JButton("...");
        JPanel saveExFilesIn = new JPanel(new BorderLayout());
        saveExFilesIn.add(selectExternalFilesSave, BorderLayout.EAST);
        saveExFilesIn.add(outputExternalFilesDir, BorderLayout.CENTER);
        saveExFilesIn.add(namingExternal, BorderLayout.WEST);
        externalFiles.add(saveExFilesIn);
        compileExternalFiles = new JButton("Compile and save");
        externalFiles.add(compileExternalFiles);
        compileExternalFilesAndUpload = new JButton("Compile and upload to " + vmInfo.getVmId());
        compileExternalFilesAndUpload.setFont(compileExternalFilesAndUpload.getFont().deriveFont(Font.BOLD));
        externalFiles.add(compileExternalFilesAndUpload);
        statusExternalFiles = new JTextField("");
        statusExternalFiles.setEditable(false);
        externalFiles.add(statusExternalFiles);

        binaryView = new JPanel(new GridLayout(0, 1));
        binaryView.setName("Current binary buffer");
        binaryFilename = new JLabel(origName + " - " + origBin.length);
        namingBinaryView = new JComboBox<>(SAVE_OPTIONS);
        namingBinaryView.setSelectedIndex(CommonUtils.SRC_SUBDIRS_NAME);
        outputBinaries = new JTextField(latestPaths.getOutputBinaries());
        selectBinary = new JButton("...");
        saveBinary = new JButton("Save current binary buffer");
        uploadBinary = new JButton("Upload current binary " + origName + " to " + vmInfo.getVmId());
        uploadBinary.setFont(uploadBinary.getFont().deriveFont(Font.BOLD));
        statusBinary = new JTextField("");
        statusBinary.setEditable(false);
        binaryView.add(binaryFilename);
        JPanel binarySaving = new JPanel(new BorderLayout());
        binarySaving.add(namingBinaryView, BorderLayout.WEST);
        binarySaving.add(outputBinaries, BorderLayout.CENTER);
        binarySaving.add(selectBinary, BorderLayout.EAST);
        binaryView.add(binarySaving);
        binaryView.add(saveBinary);
        binaryView.add(uploadBinary);
        binaryView.add(statusBinary);

        setLocationRelativeTo(null);
        setValidation();
        setSelectListener();
        setOkListener();
        addComponentsToPanels();

        this.pluginManager = pluginManager;
        this.decompiler = selectedDecompiler;
        try {
            this.haveCompiler = false;
            boolean haveDecompiler = this.pluginManager.hasBundledCompiler(decompiler);
            String s = "Default runtime compiler will be used";
            if (haveDecompiler) {
                s = selectedDecompiler.getName() + " plugin is delivered with its own compiler!!";
                this.haveCompiler = true;
            }
            statusExternalFiles.setText(s);
            statusCompileCurrentBuffer.setText(s);
        } catch (Exception ex) {
            statusExternalFiles.setText(ex.getMessage());
            statusCompileCurrentBuffer.setText(ex.getMessage());
            dualPane.setSelectedIndex(1);
        }
        if (isBinaryVisible) {
            dualPane.setSelectedIndex(3);
        }

        saveSrcBuffer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CommonUtils.saveByGui(
                        futureSrcTarget.getText(),
                        namingSource.getSelectedIndex(),
                        ".java",
                        new TextFieldBasedStus(statusCompileCurrentBuffer),
                        origName,
                        origBuffer.getBytes(StandardCharsets.UTF_8)
                );
            }
        });

        saveBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CommonUtils.saveByGui(
                        outputBinaries.getText(),
                        namingBinaryView.getSelectedIndex(),
                        ".class",
                        new TextFieldBasedStus(statusBinary),
                        origName,
                        origBin
                );
            }
        });

        uploadBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CommonUtils.uploadByGui(vmInfo, vmManager, new TextFieldBasedStus(statusBinary), origName, origBin);
            }
        });
    }

    private void setValidation() {
        DocumentListener v = new FileToClassValidator(validation, filePath, className);
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


    private static void setSelectSaveListener(JButton selectTarget, JTextField futureTarget, JComboBox<String> naming) {
        selectTarget.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(futureTarget.getText());
            if (naming.getSelectedIndex() < CommonUtils.CUSTOM_NAME) {
                jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            } else {
                jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
            }
            int returnVal = jf.showOpenDialog(selectTarget);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                futureTarget.setText(jf.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private void setOkListener() {
        setSelectSaveListener(selectSrcTarget, futureSrcTarget, namingSource);
        setSelectSaveListener(selectBinTarget, futureBinTarget, namingBinary);
        setSelectSaveListener(selectExternalFilesSave, outputExternalFilesDir, namingExternal);
        setSelectSaveListener(selectBinary, outputBinaries, namingBinary);

        selectExternalFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String[] sss = filesToCompile.getText().split(File.pathSeparator);
                File ff = new File(sss[sss.length - 1]);
                if (ff.isFile()) {
                    ff = ff.getParentFile();
                }
                JFileChooser jf = new JFileChooser(ff.getAbsolutePath());
                jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int returnVal = jf.showOpenDialog(selectExternalFiles);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    if (filesToCompile.getText().trim().isEmpty()) {
                        filesToCompile.setText(jf.getSelectedFile().getAbsolutePath());
                    } else {
                        filesToCompile.setText(
                                filesToCompile.getText() + File.pathSeparator + jf.getSelectedFile().getAbsolutePath()
                        );
                    }
                }
            }
        });

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String response = CommonUtils.uploadBytecode(
                            className.getText(),
                            vmManager,
                            vmInfo,
                            DecompilationController.fileToBytes(filePath.getText())
                    );

                    if (response.equals(DecompilerRequestReceiver.ERROR_RESPONSE)) {
                        JOptionPane.showMessageDialog(
                                null, "Class overwrite failed.", "Error", JOptionPane.ERROR_MESSAGE
                        );
                    } else {
                        validation.setForeground(Color.black);
                        validation.setText("Upload looks ok");
                    }
                } catch (Exception ex) {
                    Logger.getLogger().log(Logger.Level.ALL, ex);
                    JOptionPane.showMessageDialog(null, ex, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        compileAndSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                IdentifiedSource currentIs = new IdentifiedSource(
                        new ClassIdentifier(origName), origBuffer.getBytes(StandardCharsets.UTF_8)
                );

                new SavingCompilerOutputAction(
                        statusCompileCurrentBuffer,
                        vmInfo,
                        vmManager,
                        pluginManager,
                        decompiler,
                        haveCompiler,
                        namingBinary.getSelectedIndex(),
                        futureBinTarget.getText()
                ).run(currentIs);
            }
        });

        compileAndUpload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                IdentifiedSource currentIs = new IdentifiedSource(
                        new ClassIdentifier(origName), origBuffer.getBytes(StandardCharsets.UTF_8)
                );

                new UploadingCompilerOutputAction(
                        statusCompileCurrentBuffer,
                        vmInfo,
                        vmManager,
                        pluginManager,
                        decompiler,
                        haveCompiler,
                        namingBinary.getSelectedIndex(),
                        futureBinTarget.getText()
                ).run(currentIs);
            }
        });

        compileExternalFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String[] sources = filesToCompile.getText().split(File.pathSeparator);

                try {
                    IdentifiedSource[] loaded = CommonUtils.toIdentifiedSources(recursive.isSelected(), sources);
                    new SavingCompilerOutputAction(
                            statusExternalFiles,
                            vmInfo,
                            vmManager,
                            pluginManager,
                            decompiler,
                            haveCompiler,
                            namingExternal.getSelectedIndex(),
                            outputExternalFilesDir.getText()
                    ).run(loaded);
                } catch (Exception ex) {
                    Logger.getLogger().log(ex);
                    statusExternalFiles.setText(ex.getMessage());
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
            }
        });
    }


    private static OverwriteClassDialog.CompilationWithResult compileWithGui(
            VmInfo vmInfo,
            VmManager vmManager,
            DecompilerWrapper wrapper,
            boolean hasCompiler,
            IdentifiedSource... sources
    ) {
        ClassesProvider cp = new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager);
        ClasspathlessCompiler rc;

        if (hasCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(wrapper);
        } else {
            boolean useHostClasses = Config.getConfig().doUseHostSystemClasses();
            ClasspathlessCompiler.Arguments arguments = new ClasspathlessCompiler
                    .Arguments()
                    .useHostJavaClasses(useHostClasses);

            rc = new io.github.mkoncek.classpathless.impl.CompilerJavac(arguments);
        }
        JDialog compilationRunningDialog = new JDialog((JFrame) null, "Compiling", true);
        JTextArea compilationLog = new JTextArea();
        compilationRunningDialog.setSize(300, 400);
        compilationRunningDialog.add(new JScrollPane(compilationLog));

        OverwriteClassDialog.CompilationWithResult compiler = new OverwriteClassDialog.CompilationWithResult(
                rc, cp, compilationLog, sources
        );
        Thread t = new Thread(compiler);
        t.start();
        compilationRunningDialog.setLocationRelativeTo(null);
        compilationRunningDialog.setVisible(true);

        return compiler;
    }

    private void addComponentsToPanels() {
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
        currentBufferPane.add(statusCompileCurrentBuffer);
        dualPane.add(currentBufferPane);
        dualPane.add(manualPane);
        dualPane.add(externalFiles);
        dualPane.add(binaryView);
        this.add(dualPane);
        this.pack();
    }

    public String getManualUploadPath() {
        return this.filePath.getText();
    }

    public String getSaveSrcPath() {
        return this.futureSrcTarget.getText();
    }

    public String getSaveBinPath() {
        return this.futureBinTarget.getText();
    }

    public String getFilesToCompile() {
        return filesToCompile.getText();
    }

    public String getOutputExternalFilesDir() {
        return outputExternalFilesDir.getText();
    }

    public String getOutputBinaries() {
        return outputBinaries.getText();
    }

    private static class CompilationWithResult implements Runnable {
        private final ClasspathlessCompiler rc;
        private final ClassesProvider cp;
        private final JTextArea compilationLog;
        private final IdentifiedSource[] sources;
        private Exception ex;
        private Collection<IdentifiedBytecode> result;


        CompilationWithResult(
                ClasspathlessCompiler rc, ClassesProvider cp, JTextArea compilationLog, IdentifiedSource... sources
        ) {
            this.rc = rc;
            this.cp = cp;
            this.compilationLog = compilationLog;
            this.sources = sources;
        }

        @Override
        public void run() {
            try {
                result = rc.compileClass(cp, Optional.of(new MessagesListener() {
                    @Override
                    public void addMessage(Level level, String s) {
                        Logger.getLogger().log(Logger.Level.ALL, s);
                        compilationLog.setText(compilationLog.getText() + s + "\n");
                    }
                }), sources);
            } catch (Exception e) {
                this.ex = e;
                Logger.getLogger().log(e);
                compilationLog.setText(compilationLog.getText() + e.getMessage() + "\n");
            } finally {
                Logger.getLogger().log(Logger.Level.DEBUG, "Compilation finished");
                compilationLog.setText(compilationLog.getText() + "Compilation finished, you may close dialog\n");
            }

        }
    }

    private static class CompilerOutputActionFields {

        protected final JTextField status;
        protected final VmInfo vmInfo;
        protected final VmManager vmManager;
        protected final int namingSchema;
        protected final String destination;
        protected final PluginManager pluginManager;
        protected final DecompilerWrapper decompilerWrapper;
        protected final boolean haveCompiler;

        CompilerOutputActionFields(
                JTextField status,
                VmInfo vmInfo,
                VmManager vmManager,
                PluginManager pm,
                DecompilerWrapper dwi,
                boolean haveCompiler,
                int namingSchema,
                String destination
        ) {
            this.status = status;
            this.vmInfo = vmInfo;
            this.vmManager = vmManager;
            this.namingSchema = namingSchema;
            this.destination = destination;
            this.pluginManager = pm;
            this.decompilerWrapper = dwi;
            this.haveCompiler = haveCompiler;
        }
    }

    private static class SavingCompilerOutputAction extends CompilerOutputActionFields {

        SavingCompilerOutputAction(
                JTextField status,
                VmInfo vmInfo,
                VmManager vmManager,
                PluginManager pm,
                DecompilerWrapper dwi,
                boolean hasCompiler,
                int namingSchema,
                String destination
        ) {
            super(status, vmInfo, vmManager, pm, dwi, hasCompiler, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            OverwriteClassDialog.CompilationWithResult compiler = compileWithGui(
                    this.vmInfo, this.vmManager, decompilerWrapper, haveCompiler, sources
            );

            if (compiler.ex == null && compiler.result == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else {
                if (compiler.result.size() <= 0) {
                    status.setText("compilation finished, but no output.. nothing to save");
                    status.repaint();
                    return;
                } else {
                    status.setText("something done, will save now");
                    status.repaint();
                }

                if (namingSchema == CommonUtils.CUSTOM_NAME) {
                    if (compiler.result.size() > 0) {
                        String s = "Output of compilation was " + compiler.result.size() + "classes. " +
                                "Cannot save more then one file to exact filename";
                        JOptionPane.showMessageDialog(null, s);
                        status.setText(s);
                        return;
                    }
                }

                int saved = 0;
                for (IdentifiedBytecode clazz : compiler.result) {
                    boolean r = CommonUtils.saveByGui(
                            destination,
                            namingSchema,
                            ".class",
                            new TextFieldBasedStus(status),
                            clazz.getClassIdentifier().getFullName(),
                            clazz.getFile()
                    );
                    if (r) {
                        saved++;
                    }
                }
                if (compiler.result.size() > 1) {
                    if (saved == compiler.result.size()) {
                        status.setText("Saved all " + saved + "classes to" + destination);
                    } else {
                        status.setText("Saved only " + saved +
                                " out of " + compiler.result.size() +
                                " classes to" + destination
                        );
                    }
                }
            }
        }
    }

    private static class UploadingCompilerOutputAction extends CompilerOutputActionFields {

        UploadingCompilerOutputAction(
                JTextField status,
                VmInfo vmInfo,
                VmManager vmManager,
                PluginManager pm,
                DecompilerWrapper wrapper,
                boolean hasCompiler,
                int namingSchema,
                String destination) {
            super(status, vmInfo, vmManager, pm, wrapper, hasCompiler, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            OverwriteClassDialog.CompilationWithResult compiler = compileWithGui(
                    this.vmInfo, this.vmManager, decompilerWrapper, haveCompiler, sources
            );

            if (compiler.ex == null && compiler.result == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else {
                if (compiler.result.size() <= 0) {
                    status.setText("compilation finished, but no output.. nothing to upload");
                    status.repaint();
                    return;
                } else {
                    status.setText("something done, will upload now");
                    status.repaint();
                }

                int saved = 0;
                for (IdentifiedBytecode clazz : compiler.result) {
                    boolean r = CommonUtils.uploadByGui(
                            vmInfo,
                            vmManager,
                            new TextFieldBasedStus(status),
                            clazz.getClassIdentifier().getFullName(),
                            clazz.getFile()
                    );

                    if (r) {
                        saved++;
                    }
                }

                if (compiler.result.size() > 1) {
                    if (saved == compiler.result.size()) {
                        status.setText("Uploaded all " + saved + " classes to " + vmInfo.getVmId());
                    } else {
                        status.setText("Uploaded only " + saved +
                                " out of " + compiler.result.size() +
                                " classes to " + vmInfo.getVmId()
                        );
                    }
                }
            }
        }
    }
}
