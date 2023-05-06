package org.kcc;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class KeywordBasedCodeCompletion {

    private final JTextArea source;

    private Pattern nondelimiter;
    private CompletionItem[] keywords;
    //private final List<JComponent> functional; //buttons/check/radio boxes below completion - ex settings

    private final JFrame popup;
    private JFrame help;
    private final JList<CompletionItem> suggested;
    private final CaretListener caretListenerToRemove;
    private final KeyListener keyListenerToRemove;
    private final FocusListener focusListenerToRemove;
    private boolean debug = true;
    private Point futureLocation;


    public KeywordBasedCodeCompletion(JTextArea source, CompletionItem.CompletionItemSet set) {
        this.source = source;
        suggested = new JList<>(set.getItemsArray());
        popup = createFrame();
        setCompletionsSet(set);
        this.nondelimiter = set.getRecommendedDelimiterSet();
        suggested.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (popup.isVisible() && suggested.getSelectedValue() != null && !suggested.getSelectedValue().getDescription().isEmpty()) {
                    //fixme made disable-able
                    if (help != null) {
                        help.dispose();
                    }
                    help = new JFrame();
                    help.setFocusableWindowState(false);
                    help.setUndecorated(true);
                    help.setSize(400, 200);
                    help.setAlwaysOnTop(true);
                    JTextArea tt = new JTextArea(suggested.getSelectedValue().getDescription());
                    tt.setLineWrap(true);
                    help.add(new JScrollPane(tt));
                    help.setLocation(popup.getLocationOnScreen().x, popup.getLocationOnScreen().y + popup.getHeight());
                    help.setVisible(true);
                } else {
                    if (help != null) {
                        help.dispose();
                        help = null;
                    }
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
                    popup.setFocusableWindowState(false);
                    popup.setVisible(false);
                }
            }
        });

        popup.add(new JScrollPane(suggested));

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
                if (keyEvent.getKeyCode() == KeyEvent.VK_SPACE && keyEvent.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
                    if (futureLocation == null) {
                        calcCompletionPosition();
                    }
                    popup.setVisible(true);
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
                    if (help != null) {
                        help.dispose();
                        help = null;
                    }
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
            popup.setVisible(false);
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
                popup.setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            popup.setVisible(false);
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
        for (CompletionItem item : keywords) {
            //fixme
            //starts, contains, c o n t a i, c o n .. without order
            //case sensitive/not sensitive
            if (item.getKey().startsWith(word)) {
                filtered.add(item);
            }
        }
        setKeywordsImpl(filtered.toArray(new CompletionItem[0]));
    }

    private int calcCompletionPosition() {
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
            if (w > width) {
                width = w;
            }
        }
        ff.setSize(width, 100);
    }

    private void apply() {
        popup.setFocusableWindowState(false);
        popup.setVisible(false);
        int pos = source.getCaretPosition();
        String lastWord = getLastWord(pos);
        source.replaceRange(suggested.getSelectedValue().toString(), pos - lastWord.length(), pos);
        //source.insert(suggested.getSelectedValue().toString(), source.getCaretPosition());
    }

    public void dispose() {
        popup.dispose();
        if (help != null) {
            help.dispose();
            help = null;
        }
        source.removeFocusListener(focusListenerToRemove);
        source.removeKeyListener(keyListenerToRemove);
        source.removeCaretListener(caretListenerToRemove);
    }
}
