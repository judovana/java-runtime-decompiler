package org.jrd.frontend.MainFrame;

import org.jc.api.ClassIdentifier;
import org.jc.api.ClassesProvider;
import org.jc.api.IdentifiedBytecode;
import org.jc.api.IdentifiedSource;
import org.jc.api.InMemoryCompiler;
import org.jc.api.MessagesListener;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.Utils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RewriteClassDialog extends JDialog {

    private static class TextFieldBasedStus implements Utils.StatusKeeper{
        private final JTextField status;

        public TextFieldBasedStus(JTextField status) {
            this.status = status;
        }

        @Override
        public void setText(String s) {
            status.setText(s);
        }

        @Override
        public void onException(Exception ex) {
            OutputController.getLogger().log(ex);
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

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
    private final DecompilerWrapperInformation decompiler;
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

    public RewriteClassDialog(final String name, final LatestPaths latestPaths, final String currentBuffer, final byte[] cBinBuffer, VmInfo vmInfo, VmManager vmManager, PluginManager pluginManager, DecompilerWrapperInformation selectedDecompiler, int supperSelection) {
        super((JFrame) null, "Specify class and selectSrc its bytecode", true);
        this.setSize(400, 400);
        this.setLayout(new BorderLayout());

        this.origName = name;
        this.origBuffer = currentBuffer;
        this.origBin = cBinBuffer;
        this.vmInfo = vmInfo;
        this.vmManager = vmManager;

        dualpane = new JTabbedPane();

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
        namingBinary = new JComboBox<String>(saveOptions);
        namingSource = new JComboBox<String>(saveOptions);
        namingSource.setSelectedIndex(Utils.FULLY_QUALIFIED_NAME);
        namingBinary.setSelectedIndex(Utils.SRC_SUBDIRS_NAME);
        futureBinTarget = new JTextField(latestPaths.lastSaveBin);
        futureSrcTarget = new JTextField(latestPaths.lastSaveSrc);
        selectBinTarget = new JButton("...");
        selectSrcTarget = new JButton("...");
        compileAndUpload = new JButton("Compile "+origName+"and directly upload to "  + vmInfo.getVmId());
        compileAndUpload.setFont(compileAndSave.getFont().deriveFont(Font.BOLD));

        manualPane = new JPanel();
        manualPane.setName("Manual uplaod from file");
        manualPane.setLayout(new BorderLayout());
        inputs = new JPanel(new GridLayout(3, 1));
        buttons = new JPanel(new GridLayout(3, 1));
        validation = new JLabel("???");
        filePath = new JTextField(latestPaths.lastManualUplaod);
        className = new JTextField(origName);
        selectSrc = new JButton("...");
        nothing = new JLabel();
        ok = new JButton("upload to vm - "+vmInfo.getVmId());

        externalFiles = new JPanel(new GridLayout(0, 1));
        externalFiles.setName("Compile external files");
        externalFiles.add(new JLabel("Select external files to compile against runtime classpath"), BorderLayout.NORTH);
        JPanel exFilesIn = new JPanel(new BorderLayout());
        filesToCompile = new JTextField(latestPaths.filesToCompile);
        exFilesIn.add(filesToCompile, BorderLayout.CENTER);
        recursive = new JCheckBox("recursive");
        exFilesIn.add(recursive, BorderLayout.WEST);
        selectExternalFiles = new JButton("...");
        exFilesIn.add(selectExternalFiles, BorderLayout.EAST);
        externalFiles.add(exFilesIn);
        outputExternalFilesDir = new JTextField(latestPaths.outputExternalFilesDir);
        namingExternal = new JComboBox<>(saveOptions);
        namingExternal.setSelectedIndex(Utils.SRC_SUBDIRS_NAME);
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

        binaryView = new JPanel(new GridLayout(0,1));
        binaryView.setName("Current binary buffer");
        binaryFilename = new JLabel(origName + " - " + origBin.length);
        namingBinaryView = new JComboBox<>(saveOptions);
        namingBinaryView.setSelectedIndex(Utils.SRC_SUBDIRS_NAME);
        outputBinaries = new JTextField(latestPaths.outputBinaries);
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
        adds();

        this.pluginManager = pluginManager;
        this.decompiler = selectedDecompiler;
        try {
            this.haveCompiler = false;
            boolean haveDecompiler = this.pluginManager.haveCompiler(decompiler);
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
            dualpane.setSelectedIndex(1);
        }
        if (supperSelection>0){
            dualpane.setSelectedIndex(3);
        }


        saveSrcBuffer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Utils.saveByGui(futureSrcTarget.getText(), namingSource.getSelectedIndex(), ".java", new TextFieldBasedStus(statusCompileCurrentBuffer), origName, origBuffer.getBytes());

            }
        });

        saveBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Utils.saveByGui(outputBinaries.getText(), namingBinaryView.getSelectedIndex(), ".class", new TextFieldBasedStus(statusBinary), origName, origBin);

            }
        });

        uploadBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Utils.uploadByGui(vmInfo, vmManager, new TextFieldBasedStus(statusBinary), origName, origBin);
            }
        });
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


    private static void setSelectSaveListenr(JButton selectTarget, JTextField futureTarget, JComboBox<String> naming) {
        selectTarget.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(futureTarget.getText());
            if (naming.getSelectedIndex() < Utils.CUSTOM_NAME) {
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
        setSelectSaveListenr(selectSrcTarget, futureSrcTarget, namingSource);
        setSelectSaveListenr(selectBinTarget, futureBinTarget, namingBinary);
        setSelectSaveListenr(selectExternalFilesSave, outputExternalFilesDir, namingExternal);
        setSelectSaveListenr(selectBinary, outputBinaries, namingBinary);
        selectExternalFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jf = new JFileChooser(filesToCompile.getText());
                jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jf.setMultiSelectionEnabled(true);
                int returnVal = jf.showOpenDialog(selectExternalFiles);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filesToCompile.setText(Stream.of(jf.getSelectedFiles()).map(a -> a.getAbsolutePath()).collect(Collectors.joining(File.pathSeparator)));
                }
            }
        });
        ok.addActionListener(e -> {
            try {
                String response = Utils.uploadBytecode(className.getText(), vmManager, vmInfo, VmDecompilerInformationController.fileToBytes(filePath.getText()));
                if (response.equals(DecompilerRequestReceiver.ERROR_RESPONSE)) {
                    JOptionPane.showMessageDialog(null, "class rewrite failed.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    validation.setForeground(Color.black);
                    validation.setText("Upload looks ok");
                }
            } catch (Exception ex) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, ex);
                JOptionPane.showMessageDialog(null, ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        compileAndSave.addActionListener(actionEvent -> {
            IdentifiedSource currrentIs = new IdentifiedSource(new ClassIdentifier(origName), origBuffer.getBytes(), Optional.empty());
            new SavingCompilerOutputAction(statusCompileCurrentBuffer, vmInfo, vmManager, pluginManager, decompiler, haveCompiler, namingBinary.getSelectedIndex(), futureBinTarget.getText()).run(currrentIs);
        });

        compileAndUpload.addActionListener(actionEvent -> {
            IdentifiedSource currrentIs = new IdentifiedSource(new ClassIdentifier(origName), origBuffer.getBytes(), Optional.empty());
            new UploadingCompilerOutputAction(statusCompileCurrentBuffer, vmInfo, vmManager, pluginManager, decompiler, haveCompiler, namingBinary.getSelectedIndex(), futureBinTarget.getText()).run(currrentIs);
        });

        compileExternalFiles.addActionListener(actionEvent -> {
            String[] srcs = filesToCompile.getText().split(File.pathSeparator);
            IdentifiedSource[] loaded = new IdentifiedSource[srcs.length];
            try {
                for (int i = 0; i < srcs.length; i++) {
                    loaded[i] = new IdentifiedSource(new ClassIdentifier(guessClass(srcs[i])), Files.readAllBytes(new File(srcs[i]).toPath()), Optional.empty());
                }
                new SavingCompilerOutputAction(statusExternalFiles, vmInfo, vmManager, pluginManager, decompiler, haveCompiler, namingExternal.getSelectedIndex(), outputExternalFilesDir.getText()).run(loaded);
            } catch (Exception ex) {
                OutputController.getLogger().log(ex);
                statusExternalFiles.setText(ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        });
    }

    private String guessClass(String src) throws IOException {
        return Cli.guessName(Files.readAllBytes(new File(src).toPath()));
    }

    private static RewriteClassDialog.CompilationWithResult xompileWithGui(VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapperInformation currentDecompiler, boolean haveCompiler, IdentifiedSource... sources) {
        ClassesProvider cp = new RuntimeCompilerConnector.JRDClassesProvider(vmInfo, vmManager);
        InMemoryCompiler rc;
        if (haveCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(pm, currentDecompiler);
        } else {
            rc = new RuntimeCompilerConnector.DummyRuntimeCompiler();
        }
        JDialog compialtionRunningDialog = new JDialog((JFrame) null, "Compiling", true);
        JTextArea compilationLog = new JTextArea();
        compialtionRunningDialog.setSize(300, 400);
        compialtionRunningDialog.add(new JScrollPane(compilationLog));
        RewriteClassDialog.CompilationWithResult compiler = new RewriteClassDialog.CompilationWithResult(rc, cp, compilationLog, sources);
        Thread t = new Thread(compiler);
        t.start();
        compialtionRunningDialog.setLocationRelativeTo(null);
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
        currentBufferPane.add(statusCompileCurrentBuffer);
        dualpane.add(currentBufferPane);
        dualpane.add(manualPane);
        dualpane.add(externalFiles);
        dualpane.add(binaryView);
        this.add(dualpane);
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
        private final InMemoryCompiler rc;
        private final ClassesProvider cp;
        private final JTextArea compilationLog;
        private final IdentifiedSource[] sources;
        private Exception ex;
        private Collection<IdentifiedBytecode> result;


        public CompilationWithResult(InMemoryCompiler rc, ClassesProvider cp, JTextArea compilationLog, IdentifiedSource... sources) {
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
                        OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, s);
                        compilationLog.setText(compilationLog.getText() + s + "\n");
                    }
                }), sources);
            } catch (Exception ex) {
                this.ex = ex;
                OutputController.getLogger().log(ex);
                compilationLog.setText(compilationLog.getText() + ex.getMessage() + "\n");
            } finally {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "Compilation finished");
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
        protected final DecompilerWrapperInformation decompilerWrapper;
        protected final boolean haveCompiler;

        public CompilerOutputActionFields(JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapperInformation dwi, boolean haveCompiler, int namingSchema, String destination) {
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


        public SavingCompilerOutputAction(JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapperInformation dwi, boolean haveCompiler, int namingSchema, String destination) {
            super(status, vmInfo, vmManager, pm, dwi, haveCompiler, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            RewriteClassDialog.CompilationWithResult compiler = xompileWithGui(this.vmInfo, this.vmManager, pluginManager, decompilerWrapper, haveCompiler, sources);
            if (compiler.ex == null && compiler.result == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else if (compiler.result != null) {
                if (compiler.result.size() <= 0) {
                    status.setText("compilation finished, but no output.. nothing to save");
                    status.repaint();
                    return;
                } else {
                    status.setText("something done, will save now");
                    status.repaint();
                }
                if (namingSchema == Utils.CUSTOM_NAME) {
                    if (compiler.result.size() > 0) {
                        String s = "Output of compilation was " + compiler.result.size() + "classes. Can not save more then one file to exact filename";
                        JOptionPane.showMessageDialog(null, s);
                        status.setText(s);
                        return;
                    }
                }
                int saved = 0;
                for (IdentifiedBytecode clazz : compiler.result) {
                    boolean r = Utils.saveByGui(destination, namingSchema, ".class", new TextFieldBasedStus(status), clazz.getClassIdentifier().getFullName(), clazz.getFile());
                    if (r) {
                        saved++;
                    }
                }
                if (compiler.result.size() > 1) {
                    if (saved == compiler.result.size()) {
                        status.setText("Saved all " + saved + "classes to" + destination);
                    } else {
                        status.setText("Saved only " + saved + " from total of " + compiler.result.size() + " classes to" + destination);
                    }
                }
            } else {
                status.setText("Really weird state, report bug how to achieve this");
            }
        }
    }

    private static class UploadingCompilerOutputAction extends CompilerOutputActionFields {


        public UploadingCompilerOutputAction(JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapperInformation dwi, boolean haveCompiler, int namingSchema, String destination) {
            super(status, vmInfo, vmManager, pm, dwi, haveCompiler, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            RewriteClassDialog.CompilationWithResult compiler = xompileWithGui(this.vmInfo, this.vmManager, pluginManager, decompilerWrapper, haveCompiler, sources);
            if (compiler.ex == null && compiler.result == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else if (compiler.result != null) {
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
                    boolean r = Utils.uploadByGui(vmInfo, vmManager, new TextFieldBasedStus(status), clazz.getClassIdentifier().getFullName(), clazz.getFile());
                    if (r) {
                        saved++;
                    }
                }
                if (compiler.result.size() > 1) {
                    if (saved == compiler.result.size()) {
                        status.setText("uploaded all " + saved + "classes to" + vmInfo.getVmId());
                    } else {
                        status.setText("uploaded only " + saved + " from total of " + compiler.result.size() + " classes to" + vmInfo.getVmId());
                    }
                }
            } else {
                status.setText("Really weird state, report bug how to achieve this");
            }
        }
    }
}
