package org.kcc;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
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
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordBasedCodeCompletion {

    private final JTextArea source;
    private final CompletionSettings settings;

    private Pattern nondelimiter;
    private CompletionItem[] keywords;
    //private final List<JComponent> functional; //buttons/check/radio boxes below completion - ex settings

    private final JFrame popup;
    private JFrame help;
    private final JList<CompletionItem> suggested;
    private final JScrollPane scroll;
    private final CaretListener caretListenerToRemove;
    private final KeyListener keyListenerToRemove;
    private final FocusListener focusListenerToRemove;
    private boolean debug = true;
    private Point futureLocation;


    public KeywordBasedCodeCompletion(JTextArea source, CompletionSettings settings) {
        this.source = source;
        this.settings = settings;
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
                    //fixme made disable-able
                    showHelp();
                } else {
                    deHelp();
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
                if ((keyEvent.getKeyCode() == KeyEvent.VK_SPACE && keyEvent.getModifiersEx() == InputEvent.CTRL_DOWN_MASK)
                        ||
                        (keyEvent.getKeyCode() == KeyEvent.VK_INSERT && keyEvent.getModifiersEx() == InputEvent.ALT_DOWN_MASK)
                ) {
                    if (futureLocation == null) {
                        calcCompletionPosition();
                    }
                    popup.setVisible(true);
                    if (suggested.isShowing() && suggested.getSelectedValue() != null && !suggested.getSelectedValue().getDescription().isEmpty()) {
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
        focusListenerToRemove = (new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                popup.setFocusableWindowState(false);
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });
        source.addFocusListener(focusListenerToRemove);
    }

    private void showHelp() {
        deHelp();
        help = new JFrame();
        help.setFocusableWindowState(false);
        help.setUndecorated(true);
        help.setSize(400, 200);
        help.setAlwaysOnTop(true);
        JTextArea tt = new JTextArea(suggested.getSelectedValue().getKey() + "\n" + suggested.getSelectedValue().getDescription());
        tt.setLineWrap(true);
        help.add(new JScrollPane(tt));
        help.setLocation(popup.getLocationOnScreen().x, popup.getLocationOnScreen().y + popup.getHeight());
        help.setVisible(true);
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
                    deHelp();
                }
            }
        };
        f.setFocusableWindowState(false);
        f.setUndecorated(true);
        deductSize(f);
        f.setAlwaysOnTop(true);
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
        if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN || keyEvent.getKeyCode() == KeyEvent.VK_UP) {
            if (popup.isVisible()) {
                keyEvent.consume();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        popup.setFocusableWindowState(true);
                        suggested.requestFocus();
                        if (suggested.getSelectedValue() == null && suggested.getModel().getSize() > 0) {
                            if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                                suggested.setSelectedIndex(0);
                            }
                            if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
                                suggested.setSelectedIndex(suggested.getModel().getSize() - 1);
                            }
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

    private void proceed(DocumentEvent documentEvent) {
        proceed(null, documentEvent);
    }

    private void proceed(CaretEvent caretEvent, DocumentEvent documentEvent) {
        try {
            debugln("");
            int caretpos = calcCompletionPosition();
            if (caretpos > 0) {
                String lastLetter = source.getText().substring(caretpos - 1, caretpos);
                debugln(lastLetter);
                String word = getLastWord(caretpos);
                filter(word);
                if (popup.isVisible()) {
                    popup.setLocation(futureLocation);
                }
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

    private void filter(String word) {
        List<CompletionItem> filtered = new ArrayList<>(keywords.length);
        if (!settings.isCaseSensitive()) {
            word = word.toLowerCase();
        }
        final String finalword = word;
        String[] letters = finalword.split("");
        Pattern vagueMatch = null;
        switch (settings.getOp()) {
            case SPARSE:
                vagueMatch = Pattern.compile(".*" + Arrays.stream(letters).map(a -> Pattern.quote(a)).collect(Collectors.joining("+.*")) + ".*");
                break;
            case MAYHEM:
                vagueMatch = Pattern.compile(".*" + Arrays.stream(letters).map(a -> "[" + Pattern.quote(finalword) + "]").collect(Collectors.joining("+.*")) + ".*");
                break;
        }
        for (CompletionItem item : keywords) {
            //fixme
            //starts, contains, c o n t a i, c o n .. without order
            //case sensitive/not sensitive
            String itemKey = item.getKey();
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
            }
        }
        setKeywordsImpl(filtered.toArray(new CompletionItem[0]));
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
        return ((ListModel) (new AbstractListModel<CompletionItem>() {
            public int getSize() {
                return listData.length;
            }

            public CompletionItem getElementAt(int i) {
                return listData[i];
            }
        }));
    }

    private static ListModel createModel(final List<CompletionItem> listData) {
        return ((ListModel) (new AbstractListModel<CompletionItem>() {
            public int getSize() {
                return listData.size();
            }

            public CompletionItem getElementAt(int i) {
                return listData.get(i);
            }
        }));
    }

    private void setKeywords(CompletionItem[] keywords) {
        this.keywords = keywords;
        setKeywordsImpl(keywords);
    }

    private void setKeywordsImpl(CompletionItem[] keywords) {
        CompletionItem wasSelected = suggested.getSelectedValue();
        suggested.setModel(createModel(keywords));
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
        ff.setSize(width, 100);
    }

    private void apply() {
        ende();
        int pos = source.getCaretPosition();
        String lastWord = getLastWord(pos);
        source.replaceRange(suggested.getSelectedValue().toString(), pos - lastWord.length(), pos);
        //source.insert(suggested.getSelectedValue().toString(), source.getCaretPosition());
    }

    public void dispose() {
        popup.setVisible(false);
        popup.dispose();
        deHelp();
        source.removeFocusListener(focusListenerToRemove);
        source.removeKeyListener(keyListenerToRemove);
        source.removeCaretListener(caretListenerToRemove);
    }

    private void deHelp() {
        if (help != null) {
            help.setVisible(false);
            help.dispose();
            help = null;
        }
    }

    public CompletionSettings getSettings() {
        return settings;
    }
}
