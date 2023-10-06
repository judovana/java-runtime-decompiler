package org.jrd.frontend.frame.main.decompilerview;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jboss.byteman.agent.submit.ScriptText;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.completion.JrdCompletionSettings;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.hex.FeatureFullHex;
import org.jrd.frontend.frame.hex.StandaloneHex;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.BytemanCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.AbstractCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates.BytemanTemplateMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.CanCompile;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates.Jasm2TemplateMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JasmCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates.JasmTemplateMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates.JavaTemplateMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JavacCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JustBearerAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.templates.BytemanSkeletonTemplateMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ClasspathProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.ExecuteMethodProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.LastScriptProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.MainProviders;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.SaveProvider;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers.UploadProvider;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.ContextSuggestionsNarrower;
import org.kcc.KeywordBasedCodeCompletion;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.BytemanKeywords;
import org.kcc.wordsets.ConnectedKeywords;
import org.kcc.wordsets.JrdApiKeywords;

public class TextWithControls extends JPanel
        implements LinesProvider, ClasspathProvider, ExecuteMethodProvider, SaveProvider, UploadProvider, LastScriptProvider {

    private final RSyntaxTextArea bytecodeSyntaxTextArea;
    private final SearchControlsPanel bytecodeSearchControls;
    private final CodeCompletionType cct;
    private ClassesAndMethodsProvider classesAndMethodsProvider;
    private DecompilationController.AgentApiGenerator popup;
    private File decorativeFilePlaceholder;
    private KeywordBasedCodeCompletion codeCompletion;
    private JrdCompletionSettings oldSettings;
    private AbstractCompileAction lastCompile;
    private AbstractCompileAction lastCompileAndRun;
    private String execute = "start";
    private File save;
    private ScriptText lastScriptForByteman;
    private boolean addToRunningVm = false;
    private boolean useBootForBytemanAndUpload = false;

    private final JButton completionButton = ImageButtonFactory.createEditButton("Code completion and compilation");

    public TextWithControls(String title, CodeCompletionType cct) {
        this(title, null, cct, null);
    }

    public void setClassesAndMethodsProvider(ClassesAndMethodsProvider classesAndMethodsProvider) {
        this.classesAndMethodsProvider = classesAndMethodsProvider;
        if (codeCompletion != null) {
            codeCompletion.setBeforeFilteringNarrowing(
                    new ContextSuggestionsNarrower.ClassesAndMethodsEnforcingNarrower(classesAndMethodsProvider)
            );
        }
    }

    public TextWithControls(String title, String codeSelect, CodeCompletionType cct, ClassesAndMethodsProvider classesAndMethodsProvider) {
        this.cct = cct;
        this.classesAndMethodsProvider = classesAndMethodsProvider;
        HexWithControls.initTabLayers(this, title);
        bytecodeSyntaxTextArea = createSrcTextArea();
        bytecodeSearchControls = SearchControlsPanel.createBytecodeControls(this);
        RTextScrollPane bytecodeScrollPane = new RTextScrollPane(bytecodeSyntaxTextArea);
        this.add(bytecodeScrollPane);
        JPanel searchAndCode = new JPanel(new BorderLayout());
        this.add(searchAndCode, BorderLayout.SOUTH);
        searchAndCode.add(bytecodeSearchControls, BorderLayout.CENTER);
        if (cct != CodeCompletionType.FORBIDDEN) {
            completionButton.setToolTipText(BytecodeDecompilerView.styleTooltip() + "F8 - code completion, run and " + "compile and more");
            completionButton.addActionListener(new CompletionSettingsButtonPopUp(classesAndMethodsProvider, completionButton));
            completionButton.setOpaque(true);
            searchAndCode.add(completionButton, BorderLayout.WEST);
        }
        if (codeSelect != null) {
            JComboBox<String> hgltr = new JComboBox<String>(getAllLexers());
            hgltr.setSelectedItem(codeSelect);
            hgltr.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (bytecodeSyntaxTextArea != null) {
                        bytecodeSyntaxTextArea.setSyntaxEditingStyle(hgltr.getSelectedItem().toString());
                    }
                }
            });
            this.add(hgltr, BorderLayout.NORTH);
        }
    }

    private void normalCodeCompletionGuess(List<CompletionItem.CompletionItemSet> guessed) {
        removeCodecompletion();
        if (guessed.isEmpty()) {
            Logger.getLogger().log(Logger.Level.DEBUG, "Completion discarded");
        } else if (guessed.size() == 1) {
            codeCompletion = new KeywordBasedCodeCompletion(
                    bytecodeSyntaxTextArea,
                    new JrdCompletionSettings(
                            guessed.get(0), oldSettings.getOp(), oldSettings.isCaseSensitive(), oldSettings.isShowHelp(),
                            oldSettings.isDynamicClasses(), oldSettings.isConfigAdditionalClasses(), oldSettings.isMethodNames(),
                            oldSettings.isMethodFullSignatures()
                    )
            );
            setCompletionHelper();
        } else {
            codeCompletion = new KeywordBasedCodeCompletion(
                    bytecodeSyntaxTextArea,
                    new JrdCompletionSettings(
                            new ConnectedKeywords(guessed.toArray(new CompletionItem.CompletionItemSet[0])), oldSettings.getOp(),
                            oldSettings.isCaseSensitive(), oldSettings.isShowHelp(), oldSettings.isDynamicClasses(),
                            oldSettings.isConfigAdditionalClasses(), oldSettings.isMethodNames(), oldSettings.isMethodFullSignatures()
                    )
            );
            setCompletionHelper();
        }
    }

    private void saveOldSettings() {
        if (codeCompletion == null) {
            if (oldSettings == null) {
                oldSettings = JrdCompletionSettings.getDefault(classesAndMethodsProvider);
            }
        } else {
            //in JRD, we work ony with extended verison
            oldSettings = (JrdCompletionSettings) codeCompletion.getSettings();
        }

    }

    public void removeCodecompletion() {
        saveOldSettings();
        if (codeCompletion != null) {
            codeCompletion.dispose();
            codeCompletion = null;
        }
    }

    public String getText() {
        return bytecodeSyntaxTextArea.getText();
    }

    public byte[] getTextAsBytes() {
        return bytecodeSyntaxTextArea.getText().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void undo() {
        bytecodeSyntaxTextArea.undoLastAction();
    }

    @Override
    public void redo() {
        bytecodeSyntaxTextArea.redoLastAction();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "to lazy to invert control here")
    public RSyntaxTextArea getSyntaxTextArea() {
        return bytecodeSyntaxTextArea;
    }

    void resetSrcArea(String data) {
        bytecodeSyntaxTextArea.setText(data);
        resetUndoRedo();
    }

    private RSyntaxTextArea createSrcTextArea() {
        final RSyntaxTextArea rst = new RSyntaxTextAreaWithCompletion();
        rst.addKeyListener(new MainRsyntaxKeyListener(rst));
        rst.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        rst.setCodeFoldingEnabled(true);
        return rst;
    }

    private void quickSave() {
        try {
            Logger.getLogger().log(Logger.Level.ALL, "saving " + getFile().getAbsolutePath());
            save(getFile());
            Logger.getLogger().log(Logger.Level.ALL, "saved " + getFile().getAbsolutePath());
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
        }
    }

    public void setPopup(DecompilationController.AgentApiGenerator ap) {
        this.popup = ap;
    }

    void showApiMenu(Point forcedLocation) {
        Point caretPosition = bytecodeSyntaxTextArea.getCaret().getMagicCaretPosition();
        if (caretPosition == null || forcedLocation != null) {
            caretPosition = forcedLocation;
        }
        if (caretPosition == null) {
            return;
        }

        // y is offset to the next row
        popup.getFor(bytecodeSyntaxTextArea, forcedLocation == null).show(
                bytecodeSyntaxTextArea, caretPosition.x,
                caretPosition.y + bytecodeSyntaxTextArea.getFontMetrics(bytecodeSyntaxTextArea.getFont()).getHeight()
        );
    }

    @Override
    public List<String> getLines(LinesFormat type) {
        return Arrays.asList(bytecodeSyntaxTextArea.getText().split("\n"));
    }

    @Override
    public void setLines(LinesFormat type, List<String> lines) {
        bytecodeSyntaxTextArea.setText(lines.stream().collect(Collectors.joining("\n")));
    }

    @Override
    public boolean isBin() {
        return false;
    }

    @Override
    public File getFile() {
        return decorativeFilePlaceholder;
    }

    @Override
    public void setFile(File f) {
        this.decorativeFilePlaceholder = f;
    }

    @Override
    public void open(File f) throws IOException {
        String s = Files.readString(f.toPath());
        resetSrcArea(s);
    }

    @Override
    public void save(File f) throws IOException {
        Files.writeString(f.toPath(), bytecodeSyntaxTextArea.getText());
    }

    @Override
    public JComponent asComponent() {
        return this;
    }

    @Override
    public void resetUndoRedo() {
        bytecodeSyntaxTextArea.discardAllEdits(); // makes the bytecode upload not undoable
        bytecodeSyntaxTextArea.setCaretPosition(0);
    }

    @Override
    public void close() {
        removeCodecompletion();
    }

    @Override
    public String getName() {
        if (getFile() != null) {
            return getFile().getName();
        } else {
            return super.getName();
        }
    }

    public String getOrigName() {
        return super.getName();
    }

    private static String[] getAllLexers() {
        try {
            List<String> r = new ArrayList<>();
            Field[] fields = SyntaxConstants.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(String.class)) {
                    r.add(field.get(null).toString());
                }
            }
            return r.toArray(new String[0]);
        } catch (Exception ex) {
            Logger.getLogger().log(ex);
            return new String[]{ex.getMessage()};
        }
    }

    public void setText(String s) {
        bytecodeSyntaxTextArea.setText(s);
    }

    public void scrollDown() {
        try {
            bytecodeSyntaxTextArea.setCaretPosition(bytecodeSyntaxTextArea.getDocument().getLength());
        } catch (Exception ex) {
            //belive or not, rsyntax are can throw exception form here, and asnothing expects that, it may be fatal
            ex.printStackTrace();
        }
    }

    public enum CodeCompletionType {
        FORBIDDEN,
        JRD,
        STANDALONE
    }

    private class RSyntaxTextAreaWithCompletion extends RSyntaxTextArea {
        @Override
        public void setText(String t) {
            super.setText(t);
            if (cct != CodeCompletionType.FORBIDDEN) {
                removeCodecompletion();
                List<CompletionItem.CompletionItemSet> r = SupportedKeySets.JRD_KEY_SETS.recognize(t);
                if (r == null || r.isEmpty()) {
                    return;
                }
                if (cct == CodeCompletionType.STANDALONE) {
                    normalCodeCompletionGuess(r);
                } else if (cct == CodeCompletionType.JRD) {
                    for (CompletionItem.CompletionItemSet set : r) {
                        if (SupportedKeySets.JRD_KEY_SETS.isByteman(set)) {
                            codeCompletion = new KeywordBasedCodeCompletion(
                                    bytecodeSyntaxTextArea,
                                    new JrdCompletionSettings(
                                            new BytemanKeywords(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp(), oldSettings.isDynamicClasses(),
                                            oldSettings.isConfigAdditionalClasses(), oldSettings.isMethodNames(),
                                            oldSettings.isMethodFullSignatures()
                                    )
                            );
                            setCompletionHelper();
                            return;
                        }
                    }
                    for (CompletionItem.CompletionItemSet set : r) {
                        if (SupportedKeySets.JRD_KEY_SETS.isJasm(set)) {
                            codeCompletion = new KeywordBasedCodeCompletion(
                                    bytecodeSyntaxTextArea,
                                    new JrdCompletionSettings(
                                            new BytecodeKeywordsWithHelp(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp(), oldSettings.isDynamicClasses(),
                                            oldSettings.isConfigAdditionalClasses(), oldSettings.isMethodNames(),
                                            oldSettings.isMethodFullSignatures()
                                    )
                            );
                            setCompletionHelper();
                            return;
                        }
                    }
                    for (CompletionItem.CompletionItemSet set : r) {
                        if (SupportedKeySets.JRD_KEY_SETS.isJava(set)) {
                            codeCompletion = new KeywordBasedCodeCompletion(
                                    bytecodeSyntaxTextArea,
                                    new JrdCompletionSettings(
                                            new JrdApiKeywords(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp(), oldSettings.isDynamicClasses(),
                                            oldSettings.isConfigAdditionalClasses(), oldSettings.isMethodNames(),
                                            oldSettings.isMethodFullSignatures()
                                    )
                            );
                            setCompletionHelper();
                            return;
                        }
                    }
                    //nothing found?
                    normalCodeCompletionGuess(r);
                    setCompletionHelper();
                }
            }
        }
    }

    private void setCompletionHelper() {
        codeCompletion
                .setBeforeFilteringNarrowing(new ContextSuggestionsNarrower.ClassesAndMethodsEnforcingNarrower(classesAndMethodsProvider));
    }

    private final class CompletionSettingsButtonPopUp implements ActionListener {
        private final ClassesAndMethodsProvider mClassesAndMethodsProvider;
        private final JButton mCompletion;

        private CompletionSettingsButtonPopUp(ClassesAndMethodsProvider classesAndMethodsProvider, JButton completion) {
            mClassesAndMethodsProvider = classesAndMethodsProvider;
            mCompletion = completion;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            showEslewhere(mCompletion, mCompletion.getWidth() / 2, mCompletion.getHeight() / 2);
        }

        void showEslewhere(JComponent c, int x, int y) {
            final JPopupMenu menu = createJPopupMenu();
            SwingUtilities.invokeLater(() -> {
                menu.show(c, x, y);
                menu.requestFocusInWindow();
            });

        }

        private JPopupMenu createJPopupMenu() {
            final JPopupMenu menu = new JPopupMenu("Settings");
            if (getFile() != null) {
                JMenuItem quickSave = new JMenuItem("ctrl+s - " + getFile().getAbsolutePath());
                quickSave.addActionListener(actionEvent -> quickSave());
                menu.add(quickSave);
            }
            JMenuItem completionMenu = new JMenuItem("Code completion");
            completionMenu.addActionListener(new CodeCompletionMenuActionListener());
            menu.add(completionMenu);
            JMenuItem guess = new JMenuItem("guess completion (see verbose console for analyse)");
            addGuessCompletionItem(menu, guess);
            createAdvancedSubmenu(menu);
            JMenu templatesMenu = createTemplatesMenu();
            menu.add(templatesMenu);
            Object[] detectedJasms = detectJasms();
            PluginManager pluginManager = (PluginManager) detectedJasms[0];
            DecompilerWrapper jasm7 = (DecompilerWrapper) detectedJasms[1];
            DecompilerWrapper jasm8 = (DecompilerWrapper) detectedJasms[2];
            JMenu compile = getCompileMenu(pluginManager, jasm7, jasm8);
            menu.add(compile);
            JMenu compileAndRun = getCompileAndRunMenu(pluginManager, jasm7, jasm8);
            menu.add(compileAndRun);
            for (Component c : compile.getMenuComponents()) {
                ((AbstractCompileAction) c).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        lastCompile = (AbstractCompileAction) actionEvent.getSource();
                    }
                });
            }
            for (Component c : compileAndRun.getMenuComponents()) {
                if (c instanceof AbstractCompileAction) {
                    ((AbstractCompileAction) c).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            lastCompileAndRun = (AbstractCompileAction) actionEvent.getSource();
                        }
                    });
                }
            }
            menu.add(new JustBearerAction("Run last used compilation", "(F9)"));
            menu.add(new JustBearerAction("Run last used compile+run", "(F10)"));
            JMenuItem logConsole = new JMenuItem("Log Console");
            logConsole.setFont(logConsole.getFont().deriveFont(Font.BOLD));
            logConsole.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    GlobalConsole.getConsole().show();
                }
            });
            menu.add(logConsole);
            ((JMenuItem) menu.getComponents()[menu.getComponents().length - 3]).setEnabled(false);
            if (lastCompile != null) {
                lastUsed((JustBearerAction) menu.getComponents()[menu.getComponents().length - 3], lastCompile);

            }
            ((JMenuItem) menu.getComponents()[menu.getComponents().length - 2]).setEnabled(false);
            if (lastCompileAndRun != null) {
                lastUsed((JustBearerAction) menu.getComponents()[menu.getComponents().length - 2], lastCompileAndRun);
            }
            return menu;
        }

        private JMenu createTemplatesMenu() {
            JMenu templatesMenu = new JMenu("Templates");
            templatesMenu.add(new BytemanTemplateMenuItem(bytecodeSyntaxTextArea));
            templatesMenu.add(new BytemanSkeletonTemplateMenuItem(bytecodeSyntaxTextArea));
            templatesMenu.add(new JasmTemplateMenuItem(bytecodeSyntaxTextArea));
            templatesMenu.add(new Jasm2TemplateMenuItem(bytecodeSyntaxTextArea));
            templatesMenu.add(new JavaTemplateMenuItem(bytecodeSyntaxTextArea));
            return templatesMenu;
        }

        private class CodeCompletionMenuActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        saveOldSettings();
                        CompletionSettings newSettings = new CompletionSettingsDialogue(mClassesAndMethodsProvider)
                                .showForResults(TextWithControls.this, oldSettings);
                        if (newSettings != null) {
                            removeCodecompletion();
                            if (newSettings.getSet() != null) {
                                codeCompletion = new KeywordBasedCodeCompletion(bytecodeSyntaxTextArea, newSettings);
                                setCompletionHelper();
                            }
                        }
                    }

                });
            }
        }
    }

    private void createAdvancedSubmenu(JPopupMenu menu) {
        JMenu advanced = new JMenu("advanced");
        JMenuItem saveMenuItem = new JMenuItem("set compilation output directory (otherwise in memory only)");
        if (save != null) {
            saveMenuItem.setText("reset " + save.getAbsolutePath());
        }
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jf;
                if (save == null) {
                    jf = new JFileChooser();
                    jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int r = jf.showOpenDialog(saveMenuItem);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        save = jf.getSelectedFile();
                    }
                } else {
                    save = null;
                }
                repaintMenu(menu);
            }
        });
        advanced.add(saveMenuItem);
        JMenuItem setMethod = new JMenuItem(
                "set public static method for launch" +
                        " (\"start\" by default, \"main [Ljava.lang.String;\" would be normal main(String... args)..."
        );
        setMethod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String newExec = JOptionPane.showInputDialog(setMethod, "set method to launch", execute);
                if (newExec != null && newExec.trim().length() > 0) {
                    execute = newExec;
                    repaintMenu(menu);
                }
            }
        });
        advanced.add(setMethod);
        if (getParentWindow() != null) {
            JCheckBox treatAllAsOne = new JCheckBox("Treat all tabs in this window as single batch");
            Container parent = getParentWindow();
            if (parent != null) {
                treatAllAsOne.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        setTreatAllTabsAsOneBatch(treatAllAsOne.isSelected());
                        repaintMenu(menu);
                    }
                });
                treatAllAsOne.setSelected(isTreatAllTabsAsOneBatch());
                advanced.add(treatAllAsOne);
            }
        }
        if (hasVm(classesAndMethodsProvider)) {
            JCheckBox addToBoot = new JCheckBox(
                    "instead of adding to classpath of - " + ((DecompilationController) classesAndMethodsProvider).cpTextInfo() +
                            " - class/or byteman agent - will be added to boot classpath"
            );
            addToBoot.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    useBootForBytemanAndUpload = addToBoot.isSelected();
                    repaintMenu(menu);
                }
            });
            addToBoot.setSelected(useBootForBytemanAndUpload);
            advanced.add(addToBoot);
            JCheckBox addToSeelctedVm = new JCheckBox(
                    "after compiling  running vm classpath - " + ((DecompilationController) classesAndMethodsProvider).cpTextInfo() +
                            " - class will be added to it - this can be done only once for each class." + " Not  applicable to byteman"
            );
            addToSeelctedVm.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    addToRunningVm = addToSeelctedVm.isSelected();
                    repaintMenu(menu);
                }
            });
            addToSeelctedVm.setSelected(addToRunningVm);
            advanced.add(addToSeelctedVm);
        }
        menu.add(advanced);
    }

    private static void repaintMenu(JPopupMenu menu) {
        menu.setVisible(false);
        menu.setVisible(true);
    }

    private void addGuessCompletionItem(JPopupMenu menu, JMenuItem guess) {
        guess.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                List<CompletionItem.CompletionItemSet> guessed = SupportedKeySets.JRD_KEY_SETS.recognize(bytecodeSyntaxTextArea.getText());
                normalCodeCompletionGuess(guessed);
            }
        });
        menu.add(guess);
    }

    private Object[] detectJasms() {
        PluginManager pluginManager = new PluginManager();
        List<DecompilerWrapper> wrappers = pluginManager.getWrappers();
        DecompilerWrapper jasm7 = null;
        DecompilerWrapper jasm8 = null;
        for (DecompilerWrapper wrapper : wrappers) {
            if (!wrapper.isInvalidWrapper()) {
                if (wrapper.getName().equals("jasm")) {
                    if (jasm8 == null) {
                        jasm8 = wrapper;
                    } else {
                        if (wrapper.isLocal()) {
                            jasm8 = wrapper;
                        }
                    }
                }
                if (wrapper.getName().equals("jasm7")) {
                    if (jasm7 == null) {
                        jasm7 = wrapper;
                    } else {
                        if (wrapper.isLocal()) {
                            jasm7 = wrapper;
                        }
                    }
                }

            }
        }
        return new Object[]{pluginManager, jasm7, jasm8};
    }

    private JMenu getCompileAndRunMenu(PluginManager pluginManager, DecompilerWrapper jasm7, DecompilerWrapper jasm8) {
        JMenu compileAndRun = new JMenu("Compile and run");
        addJavacAction(pluginManager, "compile by <b>javac</b> and run with no classpath", compileAndRun, null, this, this, null);
        if (hasVm(classesAndMethodsProvider)) {
            addJavacAction(
                    pluginManager, "compile by <b>javac</b> and run with selected vm classpath ", compileAndRun, this, this, this, null
            );
        }
        addJavacAction(
                pluginManager, "compile by <b>javac</b> and run with settings additional cp", compileAndRun,
                new SettingsClasspathProvider(), this, this, null
        );
        if (jasm7 != null) {
            addJasmAction(
                    pluginManager, jasm7, "compile by <b>jasmtools7</b> and run with no classpath", compileAndRun,
                    new MainProviders(null, this, this, null)
            );
            if (classesAndMethodsProvider != null) {
                if (hasVm(classesAndMethodsProvider)) {
                    addJasmAction(
                            pluginManager, jasm7, "compile by <b>jasmtools7</b> and run with selected vm classpath ", compileAndRun,
                            new MainProviders(this, this, this, null)
                    );
                }
                addJasmAction(
                        pluginManager, jasm7, "compile by <b>jasmtools7</b> and run with settings additional cp", compileAndRun,
                        new MainProviders(new SettingsClasspathProvider(), this, this, null)
                );
            }
        }
        if (jasm8 != null) {
            addJasmAction(
                    pluginManager, jasm8, "compile by <b>jasmtools8</b> and run with no classpath", compileAndRun,
                    new MainProviders(null, this, this, null)
            );
            if (classesAndMethodsProvider != null) {
                if (hasVm(classesAndMethodsProvider)) {
                    addJasmAction(
                            pluginManager, jasm8, "compile by <b>jasmtools8</b> and run with selected vm classpath ", compileAndRun,
                            new MainProviders(this, this, this, null)
                    );
                }
                addJasmAction(
                        pluginManager, jasm8, "compile by <b>jasmtools8</b> and run with settings additional cp", compileAndRun,
                        new MainProviders(new SettingsClasspathProvider(), this, this, null)
                );
            }
        }
        if (hasVm(classesAndMethodsProvider) &&
                ((DecompilationController) classesAndMethodsProvider).getVmInfo().getType() == VmInfo.Type.LOCAL) {
            BytemanCompileAction btmSubm = new BytemanCompileAction(
                    "compile by byteman and inject to selected vm " +
                            +((DecompilationController) classesAndMethodsProvider).getVmInfo().getVmPid(),
                    this, this, this
            );
            btmSubm.addActionListener(new CompileActionListener(pluginManager, btmSubm));
            compileAndRun.add(btmSubm);
            JMenuItem btmRemove = new JMenuItem(
                    "TODO remove current rules from " + ((DecompilationController) classesAndMethodsProvider).getVmInfo().getVmPid()
            );
            btmRemove.setEnabled(false);
            compileAndRun.add(btmRemove);
            JMenuItem btmRemoveAll = new JMenuItem(
                    "TODO remove all byteman rules from " + ((DecompilationController) classesAndMethodsProvider).getVmInfo().getVmPid()
            );
            btmRemoveAll.setEnabled(false);
            compileAndRun.add(btmRemoveAll);
        }
        return compileAndRun;
    }

    private static boolean hasVm(ClassesAndMethodsProvider lclassesAndMethodsProvider) {
        return lclassesAndMethodsProvider instanceof DecompilationController &&
                ((DecompilationController) lclassesAndMethodsProvider).getVmInfo() != null;
    }

    private JMenu getCompileMenu(PluginManager pluginManager, DecompilerWrapper jasm7, DecompilerWrapper jasm8) {
        JMenu compile = new JMenu("Compilation");
        addJavacAction(pluginManager, "compile by <b>javac</b> - no CP", compile, null, null, this, this);
        if (classesAndMethodsProvider != null) {
            if (hasVm(classesAndMethodsProvider)) {
                addJavacAction(pluginManager, "compile by <b>javac</b> - selected vm classpath ", compile, this, null, this, this);
            }
            addJavacAction(
                    pluginManager, "compile by <b>javac</b> - with settings additional cp", compile, new SettingsClasspathProvider(), null,
                    this, this
            );
        }
        if (jasm7 != null) {
            addJasmAction(pluginManager, jasm7, "compile by <b>jasmtools7</b>", compile, new MainProviders(null, null, this, this));

        }
        if (jasm8 != null) {
            addJasmAction(pluginManager, jasm8, "compile by <b>jasmtools8</b>", compile, new MainProviders(null, null, this, this));
        }
        BytemanCompileAction btmCheck = new BytemanCompileAction("compile by byteman", null, this, null);
        btmCheck.addActionListener(new CompileActionListener(pluginManager, btmCheck));
        compile.add(btmCheck);
        return compile;
    }

    private void addJavacAction(
            PluginManager pluginManager, String title, JMenu compile, ClasspathProvider classpathProvider, ExecuteMethodProvider lexecute,
            SaveProvider lsave, UploadProvider uploadProvider
    ) {
        final JavacCompileAction compileJavac = new JavacCompileAction(title, classpathProvider, lsave, uploadProvider, lexecute);
        compileJavac.addActionListener(new CompileActionListener(pluginManager, compileJavac));
        compile.add(compileJavac);
    }

    private
            void
            addJasmAction(PluginManager pluginManager, DecompilerWrapper jasm, String title, JMenu compile, MainProviders mainProviders) {
        final JasmCompileAction asmcompile = new JasmCompileAction(
                title, jasm, mainProviders.getClasspathProvider(), mainProviders.getSave(), mainProviders.getUploadProvider(),
                mainProviders.getExecute()
        );
        asmcompile.addActionListener(new CompileActionListener(pluginManager, asmcompile));
        compile.add(asmcompile);
    }

    private static void lastUsed(JustBearerAction component, AbstractCompileAction last) {
        component.setOriginal(last);
        for (ActionListener l : component.getActionListeners()) {
            component.removeActionListener(l);
        }
        if (last.getActionListeners().length > 1) {
            component.addActionListener(last.getActionListeners()[1]);
        }

    }

    private final class CompileActionListener implements ActionListener {
        private final PluginManager pluginManager;
        private final CanCompile compiler;

        private CompileActionListener(PluginManager pluginManager, CanCompile compiler) {
            this.pluginManager = pluginManager;
            this.compiler = compiler;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        repaintButton(Color.BLUE);
                        if (compiler.getWrapper() != null) {
                            pluginManager.initializeWrapper(compiler.getWrapper());
                        }
                        List<String> toCompile = new ArrayList<>();
                        if (isTreatAllTabsAsOneBatch()) {
                            toCompile.addAll(getAllTabsTexts());
                        } else {
                            toCompile.add(bytecodeSyntaxTextArea.getText());
                        }
                        Collection<IdentifiedBytecode> l = compiler.compile(toCompile, pluginManager);
                        if (l == null || l.size() == 0 || new ArrayList<IdentifiedBytecode>(l).get(0).getFile().length == 0) {
                            repaintButton(Color.RED);
                        } else {
                            repaintButton(Color.GREEN);
                        }
                    } catch (Throwable t) {
                        Logger.getLogger().log(Logger.Level.ALL, t);
                    }
                    return null;
                }
            }.execute();
        }

        private void repaintButton(final Color color) {
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        completionButton.setBackground(color);
                        completionButton.repaint();
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
            }
        }
    }

    @Override
    public ClassesAndMethodsProvider getClasspath() {
        return classesAndMethodsProvider;
    }

    @Override
    public String getMethodToExecute() {
        return execute;
    }

    @Override
    public File getSaveDirectory() {
        return save;
    }

    @Override
    public boolean isUploadEnabled() {
        return addToRunningVm;
    }

    @Override
    public void resetUpload() {
        addToRunningVm = false;
    }

    @Override
    public VmInfo getVmInfo() {
        if (classesAndMethodsProvider instanceof DecompilationController) {
            return ((DecompilationController) classesAndMethodsProvider).getVmInfo();
        } else {
            return null;
        }
    }

    @Override
    public VmManager getVmManager() {
        if (classesAndMethodsProvider instanceof DecompilationController) {
            return ((DecompilationController) classesAndMethodsProvider).getVmManager();
        } else {
            return null;
        }
    }

    @Override
    public ScriptText getLastScript() {
        return lastScriptForByteman;
    }

    @Override
    public void setLastScript(ScriptText st) {
        lastScriptForByteman = st;
    }

    @Override
    public ClasspathProvider getTarget() {
        return this;
    }

    @Override
    public boolean isBoot() {
        return useBootForBytemanAndUpload;
    }

    private void setTreatAllTabsAsOneBatch(boolean selected) {
        if (getParentWindow() == null) {
            return;
        }
        getParentWindow().setTreatAllTabsAsOneBatch(selected);
    }

    private boolean isTreatAllTabsAsOneBatch() {
        if (getParentWindow() == null) {
            return false;
        }
        return getParentWindow().isTreatAllTabsAsOneBatch();
    }

    private Collection<String> getAllTabsTexts() {
        //it is necessary, that THIS tab is first in array
        // for correct detection of pkg.class for running of "main" method
        if (getParentPane() == null) {
            return Collections.singletonList(bytecodeSyntaxTextArea.getText());
        }
        return getParentWindow().getAllTexts(getParentPane());
    }

    private StandaloneHex getParentWindow() {
        FeatureFullHex pane = getParentPane();
        if (pane == null) {
            return null;
        }
        Container r = pane;
        while (r != null) {
            if (r instanceof StandaloneHex) {
                return (StandaloneHex) r;
            }
            r = r.getParent();
        }
        return null;
    }

    private FeatureFullHex getParentPane() {
        Container parent = this.getParent();
        if (parent instanceof FeatureFullHex) {
            return (FeatureFullHex) parent;
        } else {
            return null;
        }
    }

    private class MainRsyntaxKeyListener extends KeyAdapter {
        private final RSyntaxTextArea rst;

        private MainRsyntaxKeyListener(RSyntaxTextArea rst) {
            this.rst = rst;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (proceedFkeys(e)) {
                return;
            }
            proceedCtrlKeys(e);
        }

        private void proceedCtrlKeys(KeyEvent e) {
            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                if (e.getKeyCode() == KeyEvent.VK_F) {
                    bytecodeSearchControls.focus(); //!!global :(
                }
            }
            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                if (e.getKeyCode() == KeyEvent.VK_S && getFile() != null) {
                    quickSave();
                }
            }
        }

        private boolean proceedFkeys(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F8) {
                CompletionSettingsButtonPopUp keyBound = new CompletionSettingsButtonPopUp(classesAndMethodsProvider, null);
                Point caretPosition = rst.getCaret().getMagicCaretPosition();
                if (caretPosition == null) {
                    return true;
                }
                // y is offset to the next row
                keyBound.showEslewhere(rst, caretPosition.x, caretPosition.y + rst.getFontMetrics(rst.getFont()).getHeight());
            }
            if (e.getKeyCode() == KeyEvent.VK_F9 && lastCompile != null) {
                ActionListener[] acts = lastCompile.getActionListeners();
                acts[1].actionPerformed(null);
            }
            if (e.getKeyCode() == KeyEvent.VK_F10 && lastCompileAndRun != null) {
                ActionListener[] acts = lastCompileAndRun.getActionListeners();
                acts[1].actionPerformed(null);
            }
            if (e.getKeyCode() == KeyEvent.VK_F3) {
                bytecodeSearchControls.clickNextButton();
            }
            return false;
        }
    }
}
