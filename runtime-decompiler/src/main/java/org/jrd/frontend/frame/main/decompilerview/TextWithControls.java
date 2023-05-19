package org.jrd.frontend.frame.main.decompilerview;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
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
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.GlobalConsole;
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
    private CompletionSettings oldSettings;

    public TextWithControls(String title, CodeCompletionType cct) {
        this(title, null, cct, null);
    }

    public void setClassesAndMethodsProvider(ClassesAndMethodsProvider classesAndMethodsProvider) {
        this.classesAndMethodsProvider = classesAndMethodsProvider;
        if (codeCompletion!=null) {
            codeCompletion.setBeforeFilteringNarrowing(new ContextSuggestionsNarrower.ClassesAndMethodsEnforcingNarrower(classesAndMethodsProvider));
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
            final JButton completion = ImageButtonFactory.createEditButton("Code completion and compilation");
            completion.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final JPopupMenu menu = new JPopupMenu("Settings");
                    JMenuItem completionMenu = new JMenuItem("Code completion");
                    completionMenu.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            saveOldSettings();
                            CompletionSettings newSettings =
                                    new CompletionSettingsDialogue().showForResults(TextWithControls.this, oldSettings);
                            if (newSettings != null) {
                                removeCodecompletion();
                                if (newSettings.getSet() != null) {
                                    codeCompletion = new KeywordBasedCodeCompletion(bytecodeSyntaxTextArea, newSettings);
                                    setCompletionHelper();
                                }
                            }
                        }
                    });
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
                    menu.add(new JMenuItem("Dummy compilation"));
                    JMenuItem logConsole = new JMenuItem("Log Console");
                    logConsole.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            GlobalConsole.getConsole().show();
                        }
                    });
                    menu.add(logConsole);
                    menu.show(completion, completion.getWidth() / 2, completion.getHeight() / 2);
                }
            });
            searchAndCode.add(completion, BorderLayout.WEST);
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
                    new CompletionSettings(guessed.get(0), oldSettings.getOp(), oldSettings.isCaseSensitive(), oldSettings.isShowHelp())
            );
            setCompletionHelper();
        } else {
            codeCompletion = new KeywordBasedCodeCompletion(
                    bytecodeSyntaxTextArea,
                    new CompletionSettings(
                            new ConnectedKeywords(guessed.toArray(new CompletionItem.CompletionItemSet[0])), oldSettings.getOp(),
                            oldSettings.isCaseSensitive(), oldSettings.isShowHelp()
                    )
            );
            setCompletionHelper();
        }
    }

    private void saveOldSettings() {
        if (codeCompletion == null) {
            if (oldSettings == null) {
                oldSettings = SupportedKeySets.JRD_DEFAULT;
            }
        } else {
            oldSettings = codeCompletion.getSettings();
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
                                    new CompletionSettings(
                                            new BytemanKeywords(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp()
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
                                    new CompletionSettings(
                                            new BytecodeKeywordsWithHelp(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp()
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
                                    new CompletionSettings(
                                            new JrdApiKeywords(), oldSettings.getOp(), oldSettings.isCaseSensitive(),
                                            oldSettings.isShowHelp()
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
        codeCompletion.setBeforeFilteringNarrowing( new ContextSuggestionsNarrower.ClassesAndMethodsEnforcingNarrower(classesAndMethodsProvider));
    }
}
