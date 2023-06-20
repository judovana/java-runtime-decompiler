package org.jrd.frontend.frame.main.decompilerview;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import org.jrd.frontend.frame.main.decompilerview.dummycompiler.JasmCompileAction;
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
            guess.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    List<CompletionItem.CompletionItemSet> guessed =
                            SupportedKeySets.JRD_KEY_SETS.recognize(bytecodeSyntaxTextArea.getText());
                    normalCodeCompletionGuess(guessed);
                }
            });
            menu.add(guess);
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
        compileAndRun.add(new JavacCompileAction("compile by javac and run with no classpath", null));
        if (classesAndMethodsProvider != null) {
            if (classesAndMethodsProvider instanceof DecompilationController) {
                compileAndRun.add(
                        new JavacCompileAction(
                                "compile by javac and run with selected vm classpath" + " (+additional)", classesAndMethodsProvider
                        )
                );
            }
            compileAndRun.add(new JavacCompileAction("compile by javac and run with settings additional cp", classesAndMethodsProvider));
        }
        if (jasm7 != null) {
            compileAndRun.add(new JasmCompileAction("compile by asmtools7 and run with no classpath", jasm7, classesAndMethodsProvider));
            if (classesAndMethodsProvider != null) {
                if (classesAndMethodsProvider instanceof DecompilationController) {
                    compileAndRun.add(
                            new JasmCompileAction(
                                    "compile by asmtools7 and run with selected vm " + "classpath " + "(+additional)", jasm7,
                                    classesAndMethodsProvider
                            )
                    );
                }
                compileAndRun.add(
                        new JasmCompileAction(
                                "compile by asmtools7 and run with settings " + "additional cp", jasm7, classesAndMethodsProvider
                        )
                );
            }
        }
        if (jasm8 != null) {
            compileAndRun.add(new JasmCompileAction("compile by asmtools8 and run with no classpath", jasm8, classesAndMethodsProvider));
            if (classesAndMethodsProvider != null) {
                if (classesAndMethodsProvider instanceof DecompilationController) {
                    compileAndRun.add(
                            new JasmCompileAction(
                                    "compile by asmtools8 and run with selected vm " + "classpath " + "(+additional)", jasm8,
                                    classesAndMethodsProvider
                            )
                    );
                }
                compileAndRun.add(
                        new JasmCompileAction(
                                "compile by asmtools8 and run with settings " + "additional cp", jasm8, classesAndMethodsProvider
                        )
                );
            }
        }
        compileAndRun.add(new BytemanCompileAction("compile by byteman and inject to selected vm"));
        return compileAndRun;
    }

    private JMenu getCompileMenu(PluginManager pluginManager, DecompilerWrapper jasm7, DecompilerWrapper jasm8) {
        JMenu compile = new JMenu("Compilation");
        final JavacCompileAction compileNoCp = new JavacCompileAction("compile by javac - no CP", null);
        compileNoCp.addActionListener(new CompileActionListener(pluginManager, compileNoCp));
        compile.add(compileNoCp);
        if (classesAndMethodsProvider != null) {
            if (classesAndMethodsProvider instanceof DecompilationController) {
                final JavacCompileAction compileCp1 =
                        new JavacCompileAction("compile by javac - selected vm classpath (+additional)", classesAndMethodsProvider);
                compileCp1.addActionListener(new CompileActionListener(pluginManager, compileCp1));
                compile.add(compileCp1);
            }
            final JavacCompileAction compileCp2 =
                    new JavacCompileAction("compile by javac - settings additional cp only", classesAndMethodsProvider);
            compileCp2.addActionListener(new CompileActionListener(pluginManager, compileCp2));
            compile.add(compileCp2);
        }
        if (jasm7 != null) {
            final JasmCompileAction asm7compile = new JasmCompileAction("compile by asmtools7", jasm7, classesAndMethodsProvider);
            asm7compile.addActionListener(new CompileActionListener(pluginManager, asm7compile));
            compile.add(asm7compile);

        }
        if (jasm8 != null) {
            final JasmCompileAction asm8compile = new JasmCompileAction("compile by asmtools8", jasm8, classesAndMethodsProvider);
            asm8compile.addActionListener(new CompileActionListener(pluginManager, asm8compile));
            compile.add(asm8compile);
        }
        compile.add(new BytemanCompileAction("compile by byteman"));
        return compile;
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
                        Collection<IdentifiedBytecode> l = compiler.compile(bytecodeSyntaxTextArea.getText(), pluginManager);
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
