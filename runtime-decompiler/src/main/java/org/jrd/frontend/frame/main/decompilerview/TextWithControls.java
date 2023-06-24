package org.jrd.frontend.frame.main.decompilerview;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.completion.JrdCompletionSettings;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.BytemanCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.AbstractCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.CanCompile;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.Jasm2TempalteMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JasmCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JasmTempalteMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JavaTempalteMenuItem;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JavacCompileAction;
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JustBearerAction;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.ContextSuggestionsNarrower;
import org.kcc.KeywordBasedCodeCompletion;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.BytemanKeywords;
import org.kcc.wordsets.ConnectedKeywords;
import org.kcc.wordsets.JrdApiKeywords;

public class TextWithControls extends JPanel implements LinesProvider {

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
    private String execute = "";
    private File save;
    private boolean addToRunningVm = false; //fixme, dont forget to reset it after sucesfull addition!

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
        rst.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    bytecodeSearchControls.clickNextButton();
                }
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        bytecodeSearchControls.focus(); //!!global :(
                    }
                }
            }
        });
        rst.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        rst.setCodeFoldingEnabled(true);
        return rst;
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
            final JPopupMenu menu = createJPopupMenu();
            menu.show(mCompletion, mCompletion.getWidth() / 2, mCompletion.getHeight() / 2);
        }

        private JPopupMenu createJPopupMenu() {
            final JPopupMenu menu = new JPopupMenu("Settings");
            JMenuItem completionMenu = new JMenuItem("Code completion");
            completionMenu.addActionListener(new CodeCompletionMenuActionListener());
            menu.add(completionMenu);
            JMenuItem guess = new JMenuItem("guess completion (see verbose console for analyse)");
            addGuessCompletionItem(menu, guess);
            createAdvancedSubmenu(menu);
            JMenu templatesMenu = new JMenu("Templates");
            templatesMenu.add(new JMenuItem("byteman"));
            templatesMenu.add(new JasmTempalteMenuItem(bytecodeSyntaxTextArea, "jasm1"));
            templatesMenu.add(new Jasm2TempalteMenuItem(bytecodeSyntaxTextArea, "jasm2"));
            templatesMenu.add(new JavaTempalteMenuItem(bytecodeSyntaxTextArea, "java"));
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
                ((AbstractCompileAction) c).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        lastCompileAndRun = (AbstractCompileAction) actionEvent.getSource();
                    }
                });
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
        advanced.add(new JMenuItem("set compilation output directory (otherwise in memory only)"));
        advanced.add(new JMenuItem("set public static method for launch (\"start()\" by default)"));
        if (hasVm(classesAndMethodsProvider)) {
            advanced.add(
                    new JCheckBox(
                            "add to running vm - this can be done only once for each class, not " + "applicable to byteman - " +
                                    ((DecompilationController) classesAndMethodsProvider).getVmInfo()
                    )
            );
        }
        menu.add(advanced);
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
        addJavacAction(pluginManager, "compile by javac and run with no classpath", compileAndRun, null, execute);
        if (hasVm(classesAndMethodsProvider)) {
            addJavacAction(
                    pluginManager,
                    "compile by javac and run with selected vm classpath " +
                            ((DecompilationController) classesAndMethodsProvider).getVmInfo() + "(+additional)",
                    compileAndRun, classesAndMethodsProvider, execute
            );
        }
        addJavacAction(
                pluginManager, "compile by javac and run with settings " + "additional cp", compileAndRun,
                new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider(), execute
        );
        if (jasm7 != null) {
            addJasmAction(pluginManager, jasm7, "compile by asmtools7 and run with no classpath", compileAndRun, null, execute);
            if (classesAndMethodsProvider != null) {
                if (hasVm(classesAndMethodsProvider)) {
                    addJasmAction(
                            pluginManager, jasm7,
                            "compile by asmtools7 and run with selected vm classpath " +
                                    ((DecompilationController) classesAndMethodsProvider).getVmInfo() + "(+additional)",
                            compileAndRun, classesAndMethodsProvider, execute
                    );
                }
                addJasmAction(
                        pluginManager, jasm7, "compile by asmtools7 and run with settings additional cp", compileAndRun,
                        new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider(), execute
                );
            }
        }
        if (jasm8 != null) {
            addJasmAction(pluginManager, jasm8, "compile by asmtools8 and run with no classpath", compileAndRun, null, execute);
            if (classesAndMethodsProvider != null) {
                if (hasVm(classesAndMethodsProvider)) {
                    addJasmAction(
                            pluginManager, jasm8,
                            "compile by asmtools8 and run with selected vm classpath " +
                                    ((DecompilationController) classesAndMethodsProvider).getVmInfo() + "(+additional)",
                            compileAndRun, classesAndMethodsProvider, execute
                    );
                }
                addJasmAction(
                        pluginManager, jasm8, "compile by asmtools8 and run with settings additional cp", compileAndRun,
                        new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider(), execute
                );
            }
        }
        compileAndRun.add(new BytemanCompileAction("compile by byteman and inject to selected vm"));
        return compileAndRun;
    }

    private static boolean hasVm(ClassesAndMethodsProvider lclassesAndMethodsProvider) {
        return lclassesAndMethodsProvider instanceof DecompilationController &&
                ((DecompilationController) lclassesAndMethodsProvider).getVmInfo() != null;
    }

    private JMenu getCompileMenu(PluginManager pluginManager, DecompilerWrapper jasm7, DecompilerWrapper jasm8) {
        JMenu compile = new JMenu("Compilation");
        addJavacAction(pluginManager, "compile by javac - no CP", compile, null, null);
        if (classesAndMethodsProvider != null) {
            if (hasVm(classesAndMethodsProvider)) {
                addJavacAction(
                        pluginManager,
                        "compile by javac - selected vm classpath " + ((DecompilationController) classesAndMethodsProvider).getVmInfo() +
                                "(+additional)",
                        compile, classesAndMethodsProvider, null
                );
            }
            addJavacAction(
                    pluginManager, "compile by javac - settings additional cp only", compile,
                    new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider(), null
            );
        }
        if (jasm7 != null) {
            addJasmAction(pluginManager, jasm7, "compile by jasmtools7", compile, null, null);

        }
        if (jasm8 != null) {
            addJasmAction(pluginManager, jasm8, "compile by jasmtools8", compile, null, null);
        }
        compile.add(new BytemanCompileAction("compile by byteman"));
        return compile;
    }

    private void addJavacAction(
            PluginManager pluginManager, String title, JMenu compile, ClassesAndMethodsProvider lclassesAndMethodsProvider, String lexecute
    ) {
        final JavacCompileAction compileJavac = new JavacCompileAction(title, lclassesAndMethodsProvider);
        compileJavac.addActionListener(new CompileActionListener(pluginManager, compileJavac, lexecute));
        compile.add(compileJavac);
    }

    private void addJasmAction(
            PluginManager pluginManager, DecompilerWrapper jasm, String title, JMenu compile,
            ClassesAndMethodsProvider lclassesAndMethodsProvider, String lexecute
    ) {
        final JasmCompileAction asmcompile = new JasmCompileAction(title, jasm, lclassesAndMethodsProvider);
        asmcompile.addActionListener(new CompileActionListener(pluginManager, asmcompile, lexecute));
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
        private final String execute;

        private CompileActionListener(PluginManager pluginManager, CanCompile compiler, String execute) {
            this.pluginManager = pluginManager;
            this.compiler = compiler;
            this.execute = execute;
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
                        Collection<IdentifiedBytecode> l = compiler.compile(bytecodeSyntaxTextArea.getText(), pluginManager, execute);
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
}
