package org.kcc;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordBasedCodeCompletion {

    private final JTextArea source;
    private final CompletionSettings settings;
    private final JLabel statusLabel;

    private Pattern nondelimiter;
    private CompletionItem[] keywords;

    private final JFrame popup; //FIXME, rework to always dispose after hide, otherwise it is impossible to close application to often...
    private JFrame help;
    private final JList<CompletionItem> suggested;
    private final JScrollPane scroll;
    private final CaretListener caretListenerToRemove;
    private final KeyListener keyListenerToRemove;
    private final FocusListener focusListenerToRemove;
    private boolean debug = false;
    private Point futureLocation;

    private ContextSuggestionsNarrower afterFilteringNarrowing;
    private ContextSuggestionsNarrower beforeFilteringNarrowing;

    public KeywordBasedCodeCompletion(JTextArea source, CompletionSettings settings) {
        this.source = source;
        this.settings = settings;
        if (debug) {
            afterFilteringNarrowing = new ContextSuggestionsNarrower.DebugNarrower(5, 2);
            beforeFilteringNarrowing = new ContextSuggestionsNarrower.DebugNarrower(5, 2);
        }
        suggested = new JList<>(settings.getSet().getItemsArray());
        scroll = new JScrollPane(suggested);
        suggested.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    apply();
                }
            }
        });
        suggested.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popup = createFrame();
        setCompletionsSet(settings.getSet());
        this.nondelimiter = settings.getSet().getRecommendedDelimiterSet();
        suggested.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (popup.isVisible() && suggested.getSelectedValue() != null && !suggested.getSelectedValue().getDescription().isEmpty()) {
                    showHelp();
                } else {
                    removeHelp();
                }
            }
        });
        suggested.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    apply();

                }
                if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    ende();
                }
            }
        });
        popup.add(scroll);
        statusLabel = new JLabel("...");
        popup.add(statusLabel, BorderLayout.SOUTH);

        caretListenerToRemove = new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent caretEvent) {
                KeywordBasedCodeCompletion.this.proceed(caretEvent);
            }
        };
        source.addCaretListener(caretListenerToRemove);
        keyListenerToRemove = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                proceedArrow(keyEvent);
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_SPACE && keyEvent.getModifiersEx() == InputEvent.CTRL_DOWN_MASK ||
                        keyEvent.getKeyCode() == KeyEvent.VK_INSERT && keyEvent.getModifiersEx() == InputEvent.ALT_DOWN_MASK
                ) {
                    if (futureLocation == null) {
                        calcCompletionPosition();
                    }
                    popup.setVisible(true);
                    if (suggested.isShowing() && suggested.getSelectedValue() != null &&
                            !suggested.getSelectedValue().getDescription().isEmpty()) {
                        showHelp();
                    }
                }
                proceedArrow(keyEvent);
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                proceedArrow(keyEvent);
            }
        };
        source.addKeyListener(keyListenerToRemove);
        focusListenerToRemove = new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                popup.setFocusableWindowState(false);
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        };
        source.addFocusListener(focusListenerToRemove);
    }

    private void showHelp() {
        removeHelp();
        if (settings.isShowHelp()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    help = new JFrame();
                    help.setFocusableWindowState(false);
                    help.setUndecorated(true);
                    help.setSize(400, 200);
                    help.setAlwaysOnTop(true);
                    JTextArea tt =
                            new JTextArea(suggested.getSelectedValue().getKey() + "\n" + suggested.getSelectedValue().getDescription());
                    tt.setLineWrap(true);
                    help.add(new JScrollPane(tt));
                    help.setLocation(popup.getLocationOnScreen().x, popup.getLocationOnScreen().y + popup.getHeight());
                    help.setVisible(true);
                }
            });
        }
    }

    private JFrame createFrame() {
        JFrame f = new JFrame() {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(b);
                if (b) {
                    debugln("shown");
                    if (futureLocation != null) {
                        popup.setLocation(futureLocation);
                    }
                } else {
                    debugln("hidden");
                    removeHelp();
                }
            }
        };
        f.setFocusableWindowState(false);
        f.setUndecorated(true);
        deductSize(f);
        f.setAlwaysOnTop(true);
        f.setLayout(new BorderLayout());
        return f;
    }

    public void setCompletionsSet(CompletionItem.CompletionItemSet set) {
        setKeywords(set.getItemsArray());
        this.nondelimiter = set.getRecommendedDelimiterSet();
    }

    private void proceedArrow(final KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER && popup.isVisible()) {
            apply();
            keyEvent.consume();
        }
        if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
            ende();
        }
        if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN || keyEvent.getKeyCode() == KeyEvent.VK_UP ||
                keyEvent.getKeyCode() == KeyEvent.VK_PAGE_DOWN ||
                keyEvent.getKeyCode() == KeyEvent.VK_PAGE_UP) {
            if (popup.isVisible()) {
                keyEvent.consume();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        popup.setFocusableWindowState(true);
                        suggested.requestFocus();
                        if (suggested.getSelectedValue() == null && suggested.getModel().getSize() > 0) {
                            consumeKeyUpAndDown();
                        }
                    }

                    private void consumeKeyUpAndDown() {
                        if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                            suggested.setSelectedIndex(0);
                        }
                        if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
                            suggested.setSelectedIndex(suggested.getModel().getSize() - 1);
                        }
                    }
                });
            }
        }
    }

    private void ende() {
        popup.setFocusableWindowState(false);
        popup.setVisible(false);
    }

    private void proceed(CaretEvent caretEvent) {
        proceed(caretEvent, null);
    }

    private void proceed(CaretEvent caretEvent, DocumentEvent documentEvent) {
        try {
            debugln("");
            int caretpos = calcCompletionPosition();
            if (caretpos > 0) {
                String lastLetter = source.getText().substring(caretpos - 1, caretpos);
                debugln(lastLetter);
                String word = getLastWord(caretpos);
                filter(word, caretpos);
                if (popup.isVisible()) {
                    popup.setLocation(futureLocation);
                }
                statusLabel.setText(" " + suggested.getModel().getSize() + " for " + word);
            } else {
                ende();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ende();
        }
    }

    private String getLastWord(int caretpos) {
        StringBuilder lastWord = new StringBuilder();
        for (int x = caretpos; x > 0; x--) {
            String letter = source.getText().substring(x - 1, x);
            if (!nondelimiter.matcher(letter).matches()) {
                break;
            }
            lastWord.append(letter);
        }
        String word = lastWord.reverse().toString();
        debugln(word);
        return word;
    }

    private void filter(String word, int caretpos) {
        CompletionItem[] keywordsMod = keywords;
        if (beforeFilteringNarrowing != null) {
            keywordsMod = beforeFilteringNarrowing.narrowSuggestions(
                    word, keywordsMod,
                    getBeforeLines(beforeFilteringNarrowing.getBeforeContextLinesCount(), caretpos, word, source.getText()),
                    getAfterLines(beforeFilteringNarrowing.getAfterContextLinesCount(), caretpos, source.getText()),
                    settings.isCaseSensitive()
            );
        }
        if (!settings.isCaseSensitive()) {
            word = word.toLowerCase();
        }
        final String finalword = word;
        String[] letters = finalword.split("");
        Pattern vagueMatch = null;
        switch (settings.getOp()) {
            case SPARSE:
                vagueMatch =
                        Pattern.compile(".*" + Arrays.stream(letters).map(a -> Pattern.quote(a)).collect(Collectors.joining("+.*")) + ".*");
                break;
            case MAYHEM:
                vagueMatch = Pattern.compile(
                        ".*" + Arrays.stream(letters).map(a -> "[" + Pattern.quote(finalword) + "]").collect(Collectors.joining("+.*")) +
                                ".*"
                );
                break;
            default:
                break;

        }
        List<CompletionItem> filtered = new ArrayList<>(keywords.length);
        for (CompletionItem item : keywordsMod) {
            String itemKey = item.getSearchable();
            if (!settings.isCaseSensitive()) {
                itemKey = itemKey.toLowerCase();
            }
            switch (settings.getOp()) {
                case STARTS:
                    if (itemKey.startsWith(finalword)) {
                        filtered.add(item);
                    }
                    break;
                case CONTAINS:
                    if (itemKey.contains(finalword)) {
                        filtered.add(item);
                    }
                    break;
                case SPARSE:
                case MAYHEM:
                    if (vagueMatch.matcher(itemKey).matches()) {
                        filtered.add(item);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown switch: " + settings.getOp());
            }
        }
        CompletionItem[] rr = filtered.toArray(new CompletionItem[0]);
        if (afterFilteringNarrowing != null) {
            rr = afterFilteringNarrowing.narrowSuggestions(
                    word, rr, getBeforeLines(afterFilteringNarrowing.getBeforeContextLinesCount(), caretpos, word, source.getText()),
                    getAfterLines(afterFilteringNarrowing.getAfterContextLinesCount(), caretpos, source.getText()),
                    settings.isCaseSensitive()
            );
        }
        setKeywordsImpl(rr);
    }

    static String[] getAfterLines(int afterContextLinesCount, int caretpos, String text) {
        StringBuilder sb = new StringBuilder();
        int nwLinesCount = 0;
        for (int x = caretpos; x < text.length(); x++) {
            char charAtPos = text.charAt(x);
            if (charAtPos == '\n') {
                nwLinesCount++;
            }
            if (nwLinesCount > afterContextLinesCount) {
                break;
            }
            sb.append(charAtPos);
        }
        return sb.toString().split("\n");
    }

    static String[] getBeforeLines(int beforeContextLinesCount, int caretpos, String word, String text) {
        caretpos--;
        if (caretpos >= text.length()) {
            caretpos = text.length() - 1;
        }
        StringBuilder sb = new StringBuilder();
        int nwLinesCount = 0;
        for (int x = caretpos - word.length(); x >= 0; x--) {
            char charAtPos = text.charAt(x);
            if (charAtPos == '\n') {
                nwLinesCount++;
            }
            if (nwLinesCount > beforeContextLinesCount) {
                break;
            }
            sb.insert(0, charAtPos);
        }
        return sb.toString().split("\n", -1);
    }

    private int calcCompletionPosition() {
        if (!source.isShowing()) {
            return 0;
        }
        int caretpos = source.getCaretPosition();
        Point coord = source.getLocationOnScreen();
        futureLocation = coord;
        try {
            int row = source.getLineOfOffset(caretpos);
            int column = caretpos - source.getLineStartOffset(row);
            debugln("pos " + caretpos + " row " + row + " column " + column);
            Point magic = source.getCaret().getMagicCaretPosition();
            if (magic != null) {
                futureLocation = new Point(coord.x + magic.x, coord.y + magic.y + source.getFontMetrics(source.getFont()).getHeight());
            }
            return caretpos;
        } catch (BadLocationException bex) {
            bex.printStackTrace();
            return 0;
        }
    }

    private void debugln(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    private static ListModel createModel(final CompletionItem[] listData) {
        return (ListModel) new AbstractListModel<CompletionItem>() {
            public int getSize() {
                return listData.length;
            }

            public CompletionItem getElementAt(int i) {
                return listData[i];
            }
        };
    }

    private void setKeywords(CompletionItem[] keywords) {
        this.keywords = keywords;
        setKeywordsImpl(keywords);
    }

    private void setKeywordsImpl(CompletionItem[] lkeywords) {
        CompletionItem wasSelected = suggested.getSelectedValue();
        suggested.setModel(createModel(lkeywords));
        suggested.setSelectedValue(wasSelected, true);
        if (suggested.getSelectedValue() == null && suggested.getModel().getSize() > 0) {
            suggested.setSelectedIndex(0);
        }
        deductSize(popup);
    }

    private void deductSize(JFrame ff) {
        int width = 100;
        for (int i = 0; i < suggested.getModel().getSize(); i++) {
            String s = suggested.getModel().getElementAt(i).getKey();
            int w = (int) (suggested.getFontMetrics(suggested.getFont()).getStringBounds(s, suggested.getGraphics()).getWidth());
            w = w + (scroll.getVerticalScrollBar().getWidth() * 2/*it counts as it is not here*/);
            if (w > width) {
                width = w;
            }
        }
        ff.setSize(width, 130);
    }

    private void apply() {
        ende();
        int pos = source.getCaretPosition();
        String lastWord = getLastWord(pos);
        source.replaceRange(suggested.getSelectedValue().getRealReplacement(), pos - lastWord.length(), pos);
        //source.insert(suggested.getSelectedValue().toString(), source.getCaretPosition());
    }

    public void dispose() {
        popup.setVisible(false);
        popup.dispose();
        removeHelp();
        source.removeFocusListener(focusListenerToRemove);
        source.removeKeyListener(keyListenerToRemove);
        source.removeCaretListener(caretListenerToRemove);
    }

    private void removeHelp() {
        if (help != null) {
            help.setVisible(false);
            help.dispose();
            help = null;
        }
    }

    public CompletionSettings getSettings() {
        return settings;
    }

    public ContextSuggestionsNarrower getBeforeFilteringNarrowing() {
        return beforeFilteringNarrowing;
    }

    public ContextSuggestionsNarrower getAfterFilteringNarrowing() {
        return afterFilteringNarrowing;
    }

    public void setBeforeFilteringNarrowing(ContextSuggestionsNarrower beforeFilteringNarrowing) {
        this.beforeFilteringNarrowing = beforeFilteringNarrowing;
    }

    public void setAfterFilteringNarrowing(ContextSuggestionsNarrower afterFilteringNarrowing) {
        this.afterFilteringNarrowing = afterFilteringNarrowing;
    }

}
