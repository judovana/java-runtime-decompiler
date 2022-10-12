package org.jrd.frontend.frame.main.decompilerview;

import org.fife.ui.hex.event.HexSearchActionListener;
import org.fife.ui.hex.event.HexSearchDocumentListener;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.hex.swing.HexSearch;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class SearchControlsPanel extends JPanel {
    private final JTextField searchField = new JTextField("");
    private final JButton previousButton = new JButton("Previous");
    private final JButton nextButton = new JButton("Next");
    private final Color originalSearchFieldColor = searchField.getForeground();
    private SearchContext searchContext;

    private final ActionListener wasNotFoundActionListener;
    private RSyntaxTextArea searchableText;

    private SearchControlsPanel(Component optionsComponent, Component owner) {
        super(new GridBagLayout());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    owner.requestFocus(); //TODO, verify if it is correct component
                }
            }
        });
        Timer wasNotFoundTimer = new Timer(250, (ActionEvent e) -> searchField.setForeground(originalSearchFieldColor));
        wasNotFoundTimer.setRepeats(false);
        wasNotFoundActionListener = e -> {
            searchField.setForeground(Color.RED);
            wasNotFoundTimer.start();
        };

        UndoRedoKeyAdapter keyAdapter = new UndoRedoKeyAdapter(this);
        searchField.getDocument().addUndoableEditListener(keyAdapter.getUndoManager());
        searchField.addKeyListener(keyAdapter);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        gbc.gridx = 0;
        gbc.weightx = 1;
        this.add(searchField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.weightx = 0;
        this.add(optionsComponent, gbc);

        Dimension fixedButtonSize = BytecodeDecompilerView.buttonSizeBasedOnTextField(previousButton, searchField);
        previousButton.setPreferredSize(fixedButtonSize);
        nextButton.setPreferredSize(fixedButtonSize);

        gbc.gridx = 2;
        gbc.weightx = 0;
        this.add(previousButton, gbc);

        gbc.gridx = 3;
        this.add(nextButton, gbc);
    }

    public static SearchControlsPanel createHexControls(HexEditor hex) {
        HexSearch hexSearchEngine = new HexSearch(hex);
        JComboBox<HexSearch.HexSearchOptions> hexSearchType = new JComboBox<>(HexSearch.HexSearchOptions.values());
        hexSearchType.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "Set search type:<br/>" + " - " + HexSearch.HexSearchOptions.HEX +
                        ": Space-delimited hexadecimal bytes, e.g. '6A 72 64'.<br/>" + " - " + HexSearch.HexSearchOptions.INT +
                        ": Space-delimited integers, e.g. '106 114 100'.<br/>" + " - " + HexSearch.HexSearchOptions.TEXT +
                        ": Strings, e.g. 'jrd'.</div><html>"
        );
        hexSearchType.setPrototypeDisplayValue(HexSearch.HexSearchOptions.TEXT);

        SearchControlsPanel controls = new SearchControlsPanel(hexSearchType, hex.getTableFocus());

        DocumentListener hexSearchDocumentListener =
                new HexSearchDocumentListener(hexSearchEngine, controls.searchField, hexSearchType, controls.wasNotFoundActionListener);
        controls.searchField.getDocument().addDocumentListener(hexSearchDocumentListener);
        controls.previousButton.addActionListener(
                new HexSearchActionListener(hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.PREV)
        );
        controls.nextButton.addActionListener(
                new HexSearchActionListener(hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.NEXT)
        );
        hexSearchType.addActionListener(event -> hexSearchDocumentListener.changedUpdate(null));

        return controls;
    }

    public static SearchControlsPanel createBytecodeControls(TextWithControls owner) {
        final JCheckBox regexCheckBox = new JCheckBox("Regex");
        regexCheckBox.setIconTextGap(3);
        final JCheckBox caseCheckBox = new JCheckBox("Match case");
        caseCheckBox.setIconTextGap(3);

        JPanel checkboxes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        checkboxes.add(regexCheckBox);
        checkboxes.add(caseCheckBox);

        SearchControlsPanel controls = new SearchControlsPanel(checkboxes, owner);
        controls.setUnderlyingTextView(owner);

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                invokeSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                invokeSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                invokeSearch();
            }

            private void invokeSearch() {
                SwingUtilities.invokeLater(
                        () -> controls.initialSearchBytecode(
                                controls.searchField.getText(), regexCheckBox.isSelected(), caseCheckBox.isSelected()
                        )
                );
            }
        };
        regexCheckBox.addActionListener(actionEvent -> listener.changedUpdate(null));
        caseCheckBox.addActionListener(actionEvent -> listener.changedUpdate(null));

        controls.searchField.getDocument().addDocumentListener(listener);
        controls.previousButton.addActionListener(e -> controls.searchBytecode(false));
        controls.nextButton.addActionListener(e -> controls.searchBytecode(true));
        controls.nextButton.setToolTipText(BytecodeDecompilerView.styleTooltip() + "F3");
        controls.searchField.setToolTipText(BytecodeDecompilerView.styleTooltip() + "ctrl+f");
        return controls;
    }

    private void setUnderlyingTextView(TextWithControls owner) {
        this.searchableText = owner.getSyntaxTextArea();
    }

    public void fireWasNotFoundAction() {
        wasNotFoundActionListener.actionPerformed(null);
    }

    public void focus() {
        searchField.requestFocus();
    }

    void initialSearchBytecode(String query, boolean isRegex, boolean matchesCase) {
        searchContext = new SearchContext();
        searchContext.setSearchFor(query);
        searchContext.setWholeWord(false);
        searchContext.setSearchWrap(true);
        searchContext.setRegularExpression(isRegex);
        searchContext.setMatchCase(matchesCase);
        deselectBytecodeSyntaxArea(); // avoid jumping to next location while typing one char at a time
        searchBytecode(true);
    }

    void searchBytecode(boolean forward) {
        if (searchContext != null) {
            searchContext.setSearchForward(forward);
            SearchResult result = SearchEngine.find(searchableText, searchContext);
            if (!result.wasFound()) {
                this.fireWasNotFoundAction();
            }
        }
    }

    private void deselectBytecodeSyntaxArea() {
        int newDot = searchableText.getSelectionStart();
        searchableText.select(newDot, newDot);
    }

    public void clickNextButton() {
        nextButton.doClick();
    }
}
