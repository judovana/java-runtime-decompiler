package org.jrd.frontend.frame.overwrite;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;

import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.communication.TopLevelErrorCandidate;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.BytemanCompanion;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.FqnAndClassToJar;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.TextWithControls;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.BytemanCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;
import org.jrd.frontend.utility.CommonUtils;
import org.jrd.frontend.utility.TeeOutputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OverwriteClassDialog extends JDialog {

    public interface TextLog {

        void setText(String s);

        String getText();
    }

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

    private static final String[] SAVE_OPTIONS = new String[]{"fully qualified name", "src subdirectories name", "custom name"};
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
    private final JComboBox rewriteOrAdd;
    private final JButton ok;
    private final PluginManager pluginManager;
    private final DecompilerWrapper decompiler;
    private PluginManager.BundledCompilerStatus haveCompiler;

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

    private final JPanel bytemanView;
    private final JTextField saveBytemanAsFile; //read only!
    private final JLabel saveBytemanAsFileSize;
    private final JTextField bytemanStatus; //read only!
    private final JButton saveByteman;
    private final JButton saveBytemanAs;
    private final JButton loadByteman;
    private final JButton compileByteman;
    private final JButton compileAndUploadByteman;
    private final JButton unloadByteman;
    private final JButton unloadAllBytemans;
    private final JTextField bytemanHostPort;
    private final JTextField bytemanCompanionHostPort;
    private final JTextField bytemanPid;
    private final JButton createUpdateCompanion;
    private final String origName;
    private String origBuffer;
    private final byte[] origBin;
    private final VmInfo vmInfo;
    private final VmManager vmManager;
    private final ClasspathProvider cp;

    @SuppressWarnings({"VariableDeclarationUsageDistance", "CyclomaticComplexity", "LambdaBodyLength"})
    public OverwriteClassDialog(
            final String name, final LatestPaths latestPaths, final String currentBuffer, final byte[] cBinBuffer, VmInfo vmInfo,
            VmManager vmManager, PluginManager pluginManager, DecompilerWrapper selectedDecompiler, int tab, boolean isVerbose,
            ClasspathProvider cp
    ) {
        super((JFrame) null, "Specify class and selectSrc its bytecode", true);

        bytemanStatus = new JTextField("check/install status");

        this.setSize(400, 400);
        this.setLayout(new BorderLayout());

        this.origName = name;
        this.origBuffer = currentBuffer;
        this.origBin = Arrays.copyOf(cBinBuffer, cBinBuffer.length);
        this.vmInfo = vmInfo;
        this.vmManager = vmManager;
        this.cp = cp;

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
        rewriteOrAdd = new JComboBox(new String[]{"Overwrite", "Add", "Add to boot"});
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

        bytemanView = new JPanel(new GridLayout(6, 1));
        bytemanView.setName("Current byteman script");
        JPanel saveBytemanAsFilePane = new JPanel(new BorderLayout());
        saveBytemanAsFile = new JTextField(Config.getConfig().getBytemanScriptFile(name).getAbsolutePath());
        saveBytemanAsFile.setEditable(false);
        saveBytemanAsFilePane.add(saveBytemanAsFile);
        saveBytemanAsFileSize = new JLabel(origBuffer == null ? "?" : origBuffer.length() + " chars");
        saveBytemanAsFilePane.add(saveBytemanAsFileSize, BorderLayout.EAST);
        bytemanView.add(saveBytemanAsFilePane);
        saveByteman = new JButton("Save");
        saveByteman.addActionListener(a -> {
            try {
                File ff = Config.getConfig().getBytemanScriptFile(name);
                dealWithOverWrite(ff);
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                bytemanStatus.setText(ex.getMessage());
            }
        });
        saveBytemanAs = new JButton("Save copy as");
        saveBytemanAs.addActionListener(a -> {
            try {
                JFileChooser chooser = new JFileChooser(Config.getConfig().getBytemanScriptFile(name).getParentFile());
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                int selcted = chooser.showOpenDialog(OverwriteClassDialog.this);
                if (selcted != JFileChooser.APPROVE_OPTION) {
                    bytemanStatus.setText("not saving");
                    return;
                }
                dealWithOverWrite(chooser.getSelectedFile());
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                bytemanStatus.setText(ex.getMessage());
            }
        });
        loadByteman = new JButton("Replace from file");
        loadByteman.addActionListener(a -> {
            try {
                JFileChooser chooser = new JFileChooser(Config.getConfig().getBytemanScriptFile(name).getParentFile());
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);
                int selcted = chooser.showOpenDialog(OverwriteClassDialog.this);
                if (selcted != JFileChooser.APPROVE_OPTION) {
                    bytemanStatus.setText("not loading");
                    return;
                }
                dealWithNewContent(chooser.getSelectedFile());
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
                bytemanStatus.setText(ex.getMessage());
            }
        });
        compileByteman = new JButton("Type check");
        compileByteman.addActionListener(a -> {
            BytemanCompileAction bca = new BytemanCompileAction("unused", null, new DummyUploadProvider(cp));
            Collection<IdentifiedBytecode> l = bca.compile(Collections.singletonList(origBuffer), pluginManager);
            if (l == null || l.size() == 0 || new ArrayList<IdentifiedBytecode>(l).get(0).getFile().length == 0) {
                bytemanStatus.setText("typecheck failed, consult console log");
            } else {
                bytemanStatus.setText("typecheck OK, consult console log");
            }
        });
        compileAndUploadByteman = new JButton("Inject rules to vm: " + pidOrPort());
        compileAndUploadByteman.addActionListener(a -> {
            BytemanCompileAction bca = new BytemanCompileAction("unused", cp, new DummyUploadProvider(cp));
            Collection<IdentifiedBytecode> l = bca.compile(Collections.singletonList(origBuffer), pluginManager);
            if (l == null || l.size() == 0 || new ArrayList<IdentifiedBytecode>(l).get(0).getFile().length == 0) {
                bytemanStatus.setText("inject failed, consult console log");
            } else {
                bytemanStatus.setText("inject OK, consult console log");
            }
        });
        compileAndUploadByteman.setFont(compileAndUploadByteman.getFont().deriveFont(Font.BOLD));
        unloadByteman = new JButton("List/Unload rules from this file");
        unloadByteman.addActionListener(a -> {
            BytemanCompileAction bca = new BytemanCompileAction("this file rules", cp, new DummyUploadProvider(cp));
            String s = origBuffer;
            TextWithControls.listRulesDialog(s, b -> bca.removeSingleRuleSet(s), bca.getText(), null);
            bytemanStatus.setText("consult console log");
        });
        unloadAllBytemans = new JButton("List/Unload all byteman rules");
        unloadAllBytemans.addActionListener(a -> {
            BytemanCompileAction bca = new BytemanCompileAction("all rules", cp, new DummyUploadProvider(cp));
            String s = bca.listAll();
            TextWithControls.listRulesDialog(s, actionEvent1 -> bca.removeAllRules(), bca.getText(), null);
            bytemanStatus.setText("consult console log");
        });
        JPanel saveBytemanButtons = new JPanel(new GridLayout(1, 3));
        saveBytemanButtons.add(saveByteman);
        saveBytemanButtons.add(saveBytemanAs);
        saveBytemanButtons.add(loadByteman);
        bytemanView.add(saveBytemanButtons);
        JPanel compileBytemanButtons = new JPanel(new GridLayout(1, 2));
        compileBytemanButtons.add(compileByteman);
        compileBytemanButtons.add(compileAndUploadByteman);
        bytemanView.add(compileBytemanButtons);
        JPanel unloadBytemanButtons = new JPanel(new GridLayout(1, 2));
        unloadBytemanButtons.add(unloadByteman);
        unloadBytemanButtons.add(unloadAllBytemans);
        bytemanView.add(unloadBytemanButtons);

        JPanel companion = new JPanel(new GridLayout(2, 2));
        bytemanHostPort = new JTextField("bytemanHostAndPort:99666");
        bytemanCompanionHostPort = new JTextField("secondJrdAgenHosttIfPresentAndWanted:19001");
        bytemanPid = new JTextField("-1");
        bytemanPid.setEditable(false);
        createUpdateCompanion = new JButton("create/update companion");
        companion.add(bytemanHostPort);
        companion.add(bytemanCompanionHostPort);
        companion.add(bytemanPid);
        companion.add(createUpdateCompanion);
        createUpdateCompanion.addActionListener(a -> {
            if (vmInfo.getVmDecompilerStatus() == null) {
                bytemanStatus.setText("Can't create/update companion, the selected vm was never connected. Connect it first");
            }
            BytemanCompanion original = vmInfo.getVmDecompilerStatus().getBytemanCompanion();
            BytemanCompanion future = new BytemanCompanion(bytemanHostPort.getText(), bytemanCompanionHostPort.getText());
            vmInfo.getVmDecompilerStatus().setBytemanCompanion(future);
            if (vmInfo.getType() == VmInfo.Type.LOCAL) {
                KnownAgents.getInstance().setBytemanCompanion(vmInfo.getVmPid(), vmInfo.getVmDecompilerStatus().getListenPort(), future);
            }
            bytemanStatus.setText("updated");
        });
        bytemanView.add(companion);
        bytemanStatus.setEditable(false);
        bytemanView.add(bytemanStatus);
        setLocationRelativeTo(null);
        setValidation();
        setSelectListener();
        setOkListener(isVerbose);
        addComponentsToPanels();

        this.pluginManager = pluginManager;
        this.decompiler = selectedDecompiler;
        try {
            this.haveCompiler = this.pluginManager.getBundledCompilerStatus(decompiler);
            statusExternalFiles.setText(haveCompiler.getStatus());
            statusCompileCurrentBuffer.setText(haveCompiler.getStatus());
        } catch (Exception ex) {
            statusExternalFiles.setText(ex.getMessage());
            statusCompileCurrentBuffer.setText(ex.getMessage());
            dualPane.setSelectedIndex(1);
        }

        if (tab == 1) {
            dualPane.setSelectedIndex(0);
        } else if (tab == 2) {
            dualPane.setSelectedIndex(3);
        } else if (tab == 3) {
            dualPane.setSelectedIndex(4);
        } else {
            dualPane.setSelectedIndex(2);
        }

        saveSrcBuffer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CommonUtils.saveByGui(
                        futureSrcTarget.getText(), namingSource.getSelectedIndex(), ".java",
                        new TextFieldBasedStus(statusCompileCurrentBuffer), origName, origBuffer.getBytes(StandardCharsets.UTF_8)
                );
            }
        });

        saveBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CommonUtils.saveByGui(
                        outputBinaries.getText(), namingBinaryView.getSelectedIndex(), ".class", new TextFieldBasedStus(statusBinary),
                        origName, origBin
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

    private void dealWithOverWrite(File toSaveTo) throws IOException {
        byte[] src = origBuffer.getBytes(StandardCharsets.UTF_8);
        if (toSaveTo.exists()) {
            long target = toSaveTo.length();
            long origin = src.length;
            int answer = JOptionPane.showConfirmDialog(
                    OverwriteClassDialog.this,
                    "Do you want to replace existing " + toSaveTo.getAbsolutePath() + " " + target + " bytes by content of " + origin +
                            " bytes?"
            );
            if (answer == JOptionPane.OK_OPTION) {
                Files.write(toSaveTo.toPath(), src);
                bytemanStatus.setText("written: " + toSaveTo);
            } else {
                bytemanStatus.setText("canceled");
            }
        } else {
            Files.write(toSaveTo.toPath(), src);
            bytemanStatus.setText("written: " + toSaveTo);
        }
    }

    private void dealWithNewContent(File toLoadFrom) throws IOException {
        byte[] internalTarget = origBuffer.getBytes(StandardCharsets.UTF_8);
        if (toLoadFrom.exists()) {
            long nwContent = toLoadFrom.length();
            long target = internalTarget.length;
            int answer = JOptionPane.showConfirmDialog(
                    OverwriteClassDialog.this,
                    "Do you want to replace current byteman script of " + target + " bytes by content from " +
                            toLoadFrom.getAbsolutePath() + " of " + nwContent + " bytes?"
            );
            if (answer == JOptionPane.OK_OPTION) {
                String override = Files.readString(toLoadFrom.toPath(), StandardCharsets.UTF_8);
                bytemanStatus.setText("loaded: " + toLoadFrom + " (reload editor manually)");
                origBuffer = override;
                Files.write(new File(saveBytemanAsFile.getText()).toPath(), override.getBytes(StandardCharsets.UTF_8));
                saveBytemanAsFileSize.setText(origBuffer.length() + " chars");
            } else {
                bytemanStatus.setText("canceled");
            }
        } else {
            bytemanStatus.setText("don't exists: " + toLoadFrom);
        }
    }

    private String pidOrPort() {
        if (vmInfo.getType() == VmInfo.Type.LOCAL) {
            return "" + vmInfo.getVmPid();
        } else {
            return vmInfo.getVmName() + vmInfo.getVmDecompilerStatus().getListenPort();
        }
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

    private void setOkListener(boolean isVerbose) {
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
                        filesToCompile.setText(filesToCompile.getText() + File.pathSeparator + jf.getSelectedFile().getAbsolutePath());
                    }
                }
            }
        });

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String response = null;
                    if (rewriteOrAdd.getSelectedIndex() == 0) {
                        response = CommonUtils.uploadBytecode(
                                className.getText(), vmManager, vmInfo, DecompilationController.fileToBytes(filePath.getText())
                        );
                    } else if (rewriteOrAdd.getSelectedIndex() == 1) {
                        response = Lib.addFileClassesViaJar(
                                vmInfo, Collections.singletonList(new FqnAndClassToJar(className.getText(), new File(filePath.getText()))),
                                false, vmManager
                        );
                    } else if (rewriteOrAdd.getSelectedIndex() == 2) {
                        response = Lib.addFileClassesViaJar(
                                vmInfo, Collections.singletonList(new FqnAndClassToJar(className.getText(), new File(filePath.getText()))),
                                true, vmManager
                        );
                    }
                    if (new TopLevelErrorCandidate(response).isError()) {
                        JOptionPane.showMessageDialog(null, response + "\nClass overwrite failed.", "Error", JOptionPane.ERROR_MESSAGE);
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
                IdentifiedSource currentIs =
                        new IdentifiedSource(new ClassIdentifier(origName), origBuffer.getBytes(StandardCharsets.UTF_8));

                new SavingCompilerOutputAction(
                        statusCompileCurrentBuffer, vmInfo, vmManager, pluginManager, decompiler, haveCompiler.isEmbedded(), isVerbose,
                        namingBinary.getSelectedIndex(), futureBinTarget.getText()
                ).run(currentIs);
            }
        });

        compileAndUpload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                IdentifiedSource currentIs =
                        new IdentifiedSource(new ClassIdentifier(origName), origBuffer.getBytes(StandardCharsets.UTF_8));

                new UploadingCompilerOutputAction(
                        statusCompileCurrentBuffer, vmInfo, vmManager, pluginManager, decompiler, haveCompiler.isEmbedded(), isVerbose,
                        namingBinary.getSelectedIndex(), futureBinTarget.getText()
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
                            statusExternalFiles, vmInfo, vmManager, pluginManager, decompiler, haveCompiler.isEmbedded(), isVerbose,
                            namingExternal.getSelectedIndex(), outputExternalFilesDir.getText()
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
            VmInfo vmInfo, VmManager vmManager, DecompilerWrapper wrapper, boolean hasCompiler, boolean isVerbose,
            IdentifiedSource... sources
    ) {
        ClassesProvider cp = new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager);
        ClasspathlessCompiler rc = getClasspathlessCompiler(wrapper, hasCompiler, isVerbose);
        GlobalConsole.getConsole().hide();
        OverwriteClassDialog.CompilationWithResult compiler = new OverwriteClassDialog.CompilationWithResult(rc, cp, new TextLog() {
            @Override
            public void setText(String s) {
                GlobalConsole.getConsole().setText(s);
            }

            @Override
            public String getText() {
                return GlobalConsole.getConsole().getText();
            }
        }, sources);
        Thread t = new Thread(compiler);
        t.start();
        GlobalConsole.getConsole().show(true);

        return compiler;
    }

    public static ClasspathlessCompiler getClasspathlessCompiler(DecompilerWrapper wrapper, boolean hasCompiler, boolean isVerbose) {
        ClasspathlessCompiler rc;
        if (hasCompiler) {
            rc = new RuntimeCompilerConnector.ForeignCompilerWrapper(wrapper);
        } else {
            boolean useHostClasses = Config.getConfig().doUseHostSystemClasses();
            boolean useHostObject = Config.getConfig().doUseHostJavaLangObject();
            List<String> compilerArgs = new ArrayList<>(Config.getConfig().getCompilerArgs());
            if (Config.getConfig().doOverwriteST() && Config.getConfig().getBestSourceTarget().isPresent()) {
                purgeSourceTarget(compilerArgs);
            }
            if (isVerbose) {
                compilerArgs.add("-verbose");
            }
            Logger.getLogger().log(Logger.Level.DEBUG, "compiler args: " + compilerArgs.stream().collect(Collectors.joining(" ")));
            ClasspathlessCompiler.Arguments arguments = new ClasspathlessCompiler.Arguments().useHostSystemClasses(useHostClasses)
                    .compilerOptions(compilerArgs).useHostJavaLangObject(useHostObject);

            rc = new io.github.mkoncek.classpathless.impl.CompilerJavac(arguments);
        }
        return rc;
    }

    static void purgeSourceTarget(List<String> compilerArgs) {
        for (int i = compilerArgs.size() - 1; i >= 0; i--) {
            if (compilerArgs.get(i).trim().equals("-source") || compilerArgs.get(i).trim().equals("-target")) {
                Logger.getLogger().log(Logger.Level.DEBUG, "removing " + compilerArgs.get(i) + " and " + compilerArgs.get(i + 1));
                compilerArgs.remove(i); //source/target
                compilerArgs.remove(i); //the number itself
            }
        }
        compilerArgs.add(0, "-source");
        compilerArgs.add(1, "" + Config.getConfig().getBestSourceTarget().get());
        compilerArgs.add(2, "-target");
        compilerArgs.add(3, "" + Config.getConfig().getBestSourceTarget().get());
    }

    private void addComponentsToPanels() {
        inputs.add(filePath);
        inputs.add(className);
        inputs.add(className);
        inputs.add(validation);
        buttons.add(selectSrc);
        buttons.add(rewriteOrAdd);
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
        prepareBytemanLayout();
        this.add(dualPane);
        this.pack();
    }

    /*
     *for byteman for remote vm it is simple:
     *   simply set byteman connection to user-set byteman host/port to byteman agent
     *   the local pid/companion have no meaning, and should be empty and disabled
     *
     * fore local vm it is wierder
     *   if the companion exists,then it can be updated to absolutely anything. User should be warned
     *   if it does not exists, then what?
     *   Offer auto creation - that would do first action? Offer manual creation as in remote case?
     *     Is companion field good for anything?
     */
    private void prepareBytemanLayout() {
        if (vmInfo.getType() != VmInfo.Type.FS) {
            if (vmInfo.getBytemanCompanion() == null) {
                createUpdateCompanion.setText("Create byteman connector");
                if (vmInfo.getType() == VmInfo.Type.REMOTE) {
                    bytemanStatus.setText("Byteman companion can be created manually, but be sure you know what you are doing.");
                    compileAndUploadByteman.setEnabled(false);
                } else {
                    //local
                    bytemanStatus.setText(
                            "Byteman companion will be created automatically on first Inject action. It can be" +
                                    " created also manually if you really shold know what you are doing."
                    );
                }

            } else {
                if (vmInfo.getType() == VmInfo.Type.REMOTE) {
                    compileAndUpload.setText(
                            "Inject rules to vm: " + vmInfo.getBytemanCompanion().getBytemanHost() + ":" +
                                    vmInfo.getBytemanCompanion().getBytemanPort()
                    );
                }
                bytemanHostPort
                        .setText(vmInfo.getBytemanCompanion().getBytemanHost() + ":" + vmInfo.getBytemanCompanion().getBytemanPort());

                bytemanCompanionHostPort.setText(
                        vmInfo.getBytemanCompanion().getPostBytemanAgentHost() + ":" +
                                vmInfo.getBytemanCompanion().getPostBytemanAgentPort()
                );
                bytemanPid.setText("" + vmInfo.getVmPid());
                createUpdateCompanion.setText("Update byteman connector");
                bytemanStatus.setText("Byteman companion is already set up");
            }
            dualPane.add(bytemanView);
        } else {
            bytemanStatus.setText("Byteman is useless for FS classpath operations");
        }
        if (vmInfo.getType() == VmInfo.Type.LOCAL) {
            bytemanPid.setText("" + vmInfo.getVmPid());
        }
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

    public static class CompilationWithResult implements Runnable {
        private final ClasspathlessCompiler rc;
        private final ClassesProvider cp;
        private final TextLog compilationLog;
        private final IdentifiedSource[] sources;
        private Exception ex;
        private Collection<IdentifiedBytecode> result;

        public CompilationWithResult(ClasspathlessCompiler rc, ClassesProvider cp, TextLog compilationLog, IdentifiedSource... sources) {
            this.rc = rc;
            this.cp = cp;
            this.compilationLog = compilationLog;
            this.sources = sources;
        }

        @Override
        public void run() {
            PrintStream origSerr = System.err;
            try {
                //our plugins can log only to stderr
                TeeOutputStream tee = new TeeOutputStream(System.err);
                System.setErr(tee);
                try {
                    result = rc.compileClass(cp, Optional.of(new MessagesListener() {
                        @Override
                        public void addMessage(Level level, String s) {
                            if (System.getProperty(GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT) != null) {
                                if (s.startsWith("Ignoring lambda") &&
                                        !System.getProperty(GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT)
                                                .contains(GlobalConsole.CPLC_IL)) {
                                    return;
                                }
                                if (s.startsWith("Ignoring array") &&
                                        !System.getProperty(GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT)
                                                .contains(GlobalConsole.CPLC_IA)) {
                                    return;
                                }
                                if (s.startsWith("Skipping over") &&
                                        !System.getProperty(GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT)
                                                .contains(GlobalConsole.CPLC_SO)) {
                                    return;
                                }
                                if (s.startsWith("Adding class") &&
                                        !System.getProperty(GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT)
                                                .contains(GlobalConsole.CPLC_AC)) {
                                    return;
                                }
                            }
                            Logger.getLogger().log(Logger.Level.ALL, s);
                            compilationLog.setText(compilationLog.getText() + s + "\n");
                            GlobalConsole.getConsole().addMessage(level, s);
                        }
                    }), sources);
                } finally {
                    System.setErr(origSerr);
                    //CompilerJavac is able to log throug above listener.
                    //others may not. Eg our plugins - ForeignCompilerWrapper - can log only to stderr
                    if (!(rc instanceof io.github.mkoncek.classpathless.impl.CompilerJavac)) {
                        compilationLog.setText(compilationLog.getText() + new String(tee.getByteArray(), StandardCharsets.UTF_8) + "\n");
                    }
                }
            } catch (Exception e) {
                this.ex = e;
                Logger.getLogger().log(e);
                compilationLog.setText(compilationLog.getText() + e.getMessage() + "\n");
            } finally {
                String s = "Attempt to compile finished successfully";
                if (ex != null) {
                    s = "Attempt to compile failed with - " + ex.getMessage();
                }
                Logger.getLogger().log(Logger.Level.DEBUG, s + ".");
                compilationLog.setText(compilationLog.getText() + s + ", you may close dialog\n");
            }

        }

        public Collection<IdentifiedBytecode> getResult() {
            if (result == null) {
                return null;
            } else {
                return Collections.unmodifiableCollection(result);
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
        protected final boolean isVerbose;

        CompilerOutputActionFields(
                JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapper dwi, boolean haveCompiler,
                boolean isVerbose, int namingSchema, String destination
        ) {
            this.status = status;
            this.vmInfo = vmInfo;
            this.vmManager = vmManager;
            this.namingSchema = namingSchema;
            this.destination = destination;
            this.pluginManager = pm;
            this.decompilerWrapper = dwi;
            this.haveCompiler = haveCompiler;
            this.isVerbose = isVerbose;
        }
    }

    private static class SavingCompilerOutputAction extends CompilerOutputActionFields {

        SavingCompilerOutputAction(
                JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapper dwi, boolean hasCompiler,
                boolean isVerbose, int namingSchema, String destination
        ) {
            super(status, vmInfo, vmManager, pm, dwi, hasCompiler, isVerbose, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            OverwriteClassDialog.CompilationWithResult compiler =
                    compileWithGui(this.vmInfo, this.vmManager, decompilerWrapper, haveCompiler, isVerbose, sources);

            if (compiler.ex == null && compiler.getResult() == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else {
                if (compiler.getResult().size() <= 0) {
                    status.setText("compilation finished, but no output.. nothing to save");
                    status.repaint();
                    return;
                } else {
                    status.setText("something done, will save now");
                    status.repaint();
                }

                if (namingSchema == CommonUtils.CUSTOM_NAME) {
                    if (compiler.getResult().size() > 0) {
                        String s = "Output of compilation was " + compiler.getResult().size() + "classes. " +
                                "Cannot save more then one file to exact filename";
                        JOptionPane.showMessageDialog(null, s);
                        status.setText(s);
                        return;
                    }
                }

                int saved = 0;
                for (IdentifiedBytecode clazz : compiler.getResult()) {
                    boolean r = CommonUtils.saveByGui(
                            destination, namingSchema, ".class", new TextFieldBasedStus(status), clazz.getClassIdentifier().getFullName(),
                            clazz.getFile()
                    );
                    if (r) {
                        saved++;
                    }
                }
                if (compiler.getResult().size() > 1) {
                    if (saved == compiler.getResult().size()) {
                        status.setText("Saved all " + saved + "classes to" + destination);
                    } else {
                        status.setText("Saved only " + saved + " out of " + compiler.getResult().size() + " classes to" + destination);
                    }
                }
            }
        }
    }

    private static class UploadingCompilerOutputAction extends CompilerOutputActionFields {

        UploadingCompilerOutputAction(
                JTextField status, VmInfo vmInfo, VmManager vmManager, PluginManager pm, DecompilerWrapper wrapper, boolean hasCompiler,
                boolean isVerbose, int namingSchema, String destination
        ) {
            super(status, vmInfo, vmManager, pm, wrapper, hasCompiler, isVerbose, namingSchema, destination);
        }

        public void run(IdentifiedSource... sources) {
            OverwriteClassDialog.CompilationWithResult compiler =
                    compileWithGui(this.vmInfo, this.vmManager, decompilerWrapper, haveCompiler, isVerbose, sources);

            if (compiler.ex == null && compiler.getResult() == null) {
                String s = "No output from compiler, maybe still running?";
                JOptionPane.showMessageDialog(null, s);
                status.setText(s);
            } else if (compiler.ex != null) {
                JOptionPane.showMessageDialog(null, compiler.ex.getMessage());
                status.setText("Failed - " + compiler.ex.getMessage());
            } else {
                if (compiler.getResult().size() <= 0) {
                    status.setText("compilation finished, but no output.. nothing to upload");
                    status.repaint();
                    return;
                } else {
                    status.setText("something done, will upload now");
                    status.repaint();
                }

                int saved = 0;
                for (IdentifiedBytecode clazz : compiler.getResult()) {
                    boolean r = CommonUtils.uploadByGui(
                            vmInfo, vmManager, new TextFieldBasedStus(status), clazz.getClassIdentifier().getFullName(), clazz.getFile()
                    );

                    if (r) {
                        saved++;
                    }
                }

                if (compiler.getResult().size() > 1) {
                    if (saved == compiler.getResult().size()) {
                        status.setText("Uploaded all " + saved + " classes to " + vmInfo.getVmId());
                    } else {
                        status.setText(
                                "Uploaded only " + saved + " out of " + compiler.getResult().size() + " classes to " + vmInfo.getVmId()
                        );
                    }
                }
            }
        }
    }

    private static final class DummyUploadProvider implements UploadProvider {
        private final ClasspathProvider mCp;

        private DummyUploadProvider(ClasspathProvider cp) {
            mCp = cp;
        }

        @Override
        public ClasspathProvider getTarget() {
            return mCp;
        }

        @Override
        public boolean isUploadEnabled() {
            return true;
        }

        @Override
        public void resetUpload() {

        }

        @Override
        public boolean isBoot() {
            return false;
        }
    }
}
