package org.jrd.frontend.frame.main;

import org.fife.ui.hex.event.HexSearchActionListener;
import org.fife.ui.hex.event.HexSearchDocumentListener;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.hex.swing.HexSearch;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that creates GUI for attached VM.
 */
public class BytecodeDecompilerView {

    private JPanel bytecodeDecompilerPanel;
        private JSplitPane splitPane;
            private JPanel classes;
                private JPanel classesToolBar;
                    private JButton reloadClassesButton;
                    private JTextField classesSortField;
                        private final Color classesSortFieldColor;
                private JPanel classesPanel;
                    private JScrollPane classesScrollPane;
                        private JList<String> filteredClassesJList;
            private JPanel buffersPanel;
                private JPanel buffersToolBar;
                    private JButton overwriteButton;
                    private JComboBox<DecompilerWrapperInformation> pluginComboBox;
                private final JTabbedPane buffers;
                    private JPanel sourceBuffer;
                        private RTextScrollPane bytecodeScrollPane;
                            private RSyntaxTextArea bytecodeSyntaxTextArea;
                        private SearchControlsPanel bytecodeSearchControls;
                    private JPanel binaryBuffer;
                        private JPanel hexControls;
                        private HexEditor hex;
                        private JPanel hexSearchControls;

    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private RewriteActionListener rewriteActionListener;

    private String[] loadedClasses;
    private String lastDecompiledClass = "";

    private SearchContext searchContext;

    private boolean splitPaneFirstResize = true;

    /**
     * Constructor creates the graphics and adds the action listeners.
     *
     * @return BytecodeDecompilerPanel
     */

    public JPanel getBytecodeDecompilerPanel() {
        return bytecodeDecompilerPanel;
    }


    public BytecodeDecompilerView() {

        bytecodeDecompilerPanel = new JPanel(new BorderLayout());

        classesPanel = new JPanel(new BorderLayout());

        classesSortField = new JTextField(".*");
        classesSortFieldColor = classesSortField.getForeground();
        classesSortField.setToolTipText(styleTooltip() + "Search for classes using regular expressions.<br/>" +
                "Look for specific classes or packages using '.*SomeClass.*' or '.*some.package.*'<br/>" +
                "Don't forget to escape dollar signs '$' of inner classes to '\\$'.<br/>" +
                "For negation use the negative lookahead '^(?!.*unwanted).*$' syntax." +
                "</div><html>");
        classesSortField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                updateClassList();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                updateClassList();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                updateClassList();
            }
        });
        UndoRedoKeyAdapter classesSortFieldKeyAdapter = new UndoRedoKeyAdapter();
        classesSortField.getDocument().addUndoableEditListener(classesSortFieldKeyAdapter.getUndoManager());
        classesSortField.addKeyListener(classesSortFieldKeyAdapter);

        bytecodeSearchControls = SearchControlsPanel.createBytecodeControls(this);

        classesPanel.add(classesSortField, BorderLayout.NORTH);

        filteredClassesJList = new JList<>();
        filteredClassesJList.setFixedCellHeight(20);
        filteredClassesJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final String name = filteredClassesJList.getSelectedValue();
                if (name != null || filteredClassesJList.getSelectedIndex() != -1) {
                    classWorker(name);
                }
            }
        });

        filteredClassesJList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    final String name = filteredClassesJList.getSelectedValue();
                    if (name != null || filteredClassesJList.getSelectedIndex() != -1) {
                        classWorker(name);
                    }
                }
            }
        });

        overwriteButton = new JButton("Overwrite class");
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String name = filteredClassesJList.getSelectedValue();
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            ActionEvent event = new ActionEvent(this, 3, name);
                            rewriteActionListener.actionPerformed(event);
                        } catch (Throwable t) {
                            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, t);
                        }
                        return null;
                    }
                }.execute();
            }
        });
        overwriteButton.setPreferredSize(buttonSizeBasedOnTextField(overwriteButton, classesSortField));

        reloadClassesButton = new JButton("Reload classes");
        reloadClassesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            ActionEvent event = new ActionEvent(this, 2, null);


                            classesActionListener.actionPerformed(event);
                        } catch (Throwable t) {
                            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, t);
                        }
                        return null;
                    }
                }.execute();
            }
        });
        reloadClassesButton.setPreferredSize(buttonSizeBasedOnTextField(reloadClassesButton, classesSortField));

        classesToolBar = new JPanel(new GridBagLayout());
        classesToolBar.setBorder(new EtchedBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.weightx = 0;
        classesToolBar.add(reloadClassesButton, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        classesToolBar.add(classesSortField, gbc);

        pluginComboBox = new JComboBox<DecompilerWrapperInformation>();
        pluginComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (filteredClassesJList.getSelectedIndex() != -1) {
                    ActionEvent event = new ActionEvent(this, 1, filteredClassesJList.getSelectedValue());
                    bytesActionListener.actionPerformed(event);
                }
            }
        });

        bytecodeSyntaxTextArea = new RSyntaxTextArea();
        bytecodeSyntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        bytecodeSyntaxTextArea.setCodeFoldingEnabled(true);
        bytecodeScrollPane = new RTextScrollPane(bytecodeSyntaxTextArea);

        hex = new HexEditor();
        hexControls = new JPanel();

        classes = new JPanel();
        classes.setLayout(new BorderLayout());
        classes.setBorder(new EtchedBorder());

        sourceBuffer = new JPanel();
        sourceBuffer.setLayout(new BorderLayout());
        sourceBuffer.setBorder(new EtchedBorder());

        binaryBuffer = new JPanel();
        binaryBuffer.setLayout(new BorderLayout());
        binaryBuffer.setBorder(new EtchedBorder());

        buffersToolBar = new JPanel(new GridBagLayout());
        buffersToolBar.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);

        gbc.weightx = 1;
        buffersToolBar.add(Box.createHorizontalGlue(), gbc);

        gbc.weightx = 0;
        gbc.gridx = 1;
        buffersToolBar.add(overwriteButton, gbc);

        gbc.gridx = 2;
        buffersToolBar.add(pluginComboBox, gbc);

        classesScrollPane = new JScrollPane(filteredClassesJList);
        classesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        classesPanel.add(classesToolBar, BorderLayout.NORTH);
        classesPanel.add(classesScrollPane, BorderLayout.CENTER);
        classes.add(classesPanel);

        buffers = new JTabbedPane();
        sourceBuffer.setName("Source buffer");
        sourceBuffer.add(bytecodeScrollPane);
        sourceBuffer.add(bytecodeSearchControls, BorderLayout.SOUTH);
        binaryBuffer.setName("Binary buffer");
        binaryBuffer.add(hex);
        binaryBuffer.add(hexControls, BorderLayout.NORTH);
        hexControls.setLayout(new GridLayout(1, 2));
        JButton undo = new JButton("Undo");
        hexControls.add(undo);
        JButton redo = new JButton("Redo");
        hexControls.add(redo);
        undo.addActionListener(actionEvent -> hex.undo());
        redo.addActionListener(actionEvent -> hex.redo());

        hexSearchControls = SearchControlsPanel.createHexControls(hex);

        binaryBuffer.add(hexSearchControls, BorderLayout.SOUTH);

        buffers.add(sourceBuffer);
        buffers.add(binaryBuffer);

        buffersPanel = new JPanel(new BorderLayout());
        buffersPanel.setBorder(new EtchedBorder());
        buffersPanel.add(buffersToolBar, BorderLayout.NORTH);
        buffersPanel.add(buffers, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                classes, buffersPanel);

        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (splitPaneFirstResize) {
                    splitPane.setDividerLocation(0.5);
                    splitPaneFirstResize = false;
                }
            }
        });

        bytecodeDecompilerPanel.add(splitPane, BorderLayout.CENTER);

        bytecodeDecompilerPanel.setVisible(true);

    }

    private static class UndoRedoKeyAdapter extends KeyAdapter {
        UndoManager undoManager = new UndoManager();

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                if (e.getKeyCode() == KeyEvent.VK_Z && undoManager.canUndo()) {
                    undoManager.undo();
                } else if (e.getKeyCode() == KeyEvent.VK_Y && undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        }

        public UndoManager getUndoManager() {
            return undoManager;
        }
    }

    private static class SearchControlsPanel extends JPanel {
        private final JTextField searchField = new JTextField("");
        private final JButton previousButton = new JButton("Previous");
        private final JButton nextButton = new JButton("Next");
        private final Color originalSearchFieldColor = searchField.getForeground();

        private final ActionListener wasNotFoundActionListener;

        private SearchControlsPanel(Component optionsComponent) {
            super(new GridBagLayout());

            Timer wasNotFoundTimer = new Timer(250, (ActionEvent e) -> searchField.setForeground(originalSearchFieldColor));
            wasNotFoundTimer.setRepeats(false);
            wasNotFoundActionListener = e -> {
                searchField.setForeground(Color.RED);
                wasNotFoundTimer.start();
            };

            UndoRedoKeyAdapter keyAdapter = new UndoRedoKeyAdapter();
            searchField.getDocument().addUndoableEditListener(keyAdapter.getUndoManager());
            searchField.addKeyListener(keyAdapter);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(3,3,3,3);

            gbc.gridx = 0;
            gbc.weightx = 1;
            this.add(searchField, gbc);

            gbc.gridwidth = 1;
            gbc.gridx = 1;
            gbc.weightx = 0;
            this.add(optionsComponent, gbc);

            Dimension fixedButtonSize = buttonSizeBasedOnTextField(previousButton, searchField);
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
            hexSearchType.setToolTipText(styleTooltip() +
                    "Set search type:<br/>" +
                    " - " + HexSearch.HexSearchOptions.HEX + ": Space-delimited hexadecimal bytes, e.g. '6A 72 64'.<br/>" +
                    " - " + HexSearch.HexSearchOptions.INT + ": Space-delimited integers, e.g. '106 114 100'.<br/>" +
                    " - " + HexSearch.HexSearchOptions.TEXT + ": Strings, e.g. 'jrd'.</div><html>"
            );
            hexSearchType.setPrototypeDisplayValue(HexSearch.HexSearchOptions.TEXT);

            SearchControlsPanel controls = new SearchControlsPanel(hexSearchType);

            DocumentListener hexSearchDocumentListener = new HexSearchDocumentListener(hexSearchEngine, controls.searchField, hexSearchType, controls.wasNotFoundActionListener);
            controls.searchField.getDocument().addDocumentListener(hexSearchDocumentListener);
            controls.previousButton.addActionListener(new HexSearchActionListener(hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.PREV));
            controls.nextButton.addActionListener(new HexSearchActionListener(hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.NEXT));
            hexSearchType.addActionListener(event -> hexSearchDocumentListener.changedUpdate(null));

            return controls;
        }

        public static SearchControlsPanel createBytecodeControls(BytecodeDecompilerView parent) {
            final JCheckBox regexCheckBox = new JCheckBox("Regex");
            regexCheckBox.setIconTextGap(3);

            SearchControlsPanel controls = new SearchControlsPanel(regexCheckBox);

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
                    SwingUtilities.invokeLater(() -> parent.initialSearchBytecode(controls.searchField.getText(), regexCheckBox.isSelected()));
                }
            };
            regexCheckBox.addActionListener(actionEvent -> listener.changedUpdate(null));

            controls.searchField.getDocument().addDocumentListener(listener);
            controls.previousButton.addActionListener(e -> parent.searchBytecode(false));
            controls.nextButton.addActionListener(e -> parent.searchBytecode(true));

            return controls;
        }

        public void fireWasNotFoundAction() {
            wasNotFoundActionListener.actionPerformed(null);
        }
    }

    public static String styleTooltip() {
        return "<html>" + "<div style='background:yellow;color:black'>";
    }

    private void updateClassList() {
        ArrayList<String> filtered = new ArrayList<>();
        String filter = classesSortField.getText().trim();
        if (filter.isEmpty()) {
            filter = ".*";
        }
        try {
            Pattern p = Pattern.compile(filter);
            classesSortField.setForeground(classesSortFieldColor);
            classesSortField.repaint();
            for (String clazz : loadedClasses) {
                Matcher m = p.matcher(clazz);
                if (m.matches()) {
                    filtered.add(clazz);
                }
            }
        }catch(Exception ex){
            classesSortField.setForeground(Color.red);
            classesSortField.repaint();
            for (String clazz : loadedClasses) {
                if (!clazz.contains(filter)) {
                    filtered.add(clazz);
                }
            }
        }
        filteredClassesJList.setListData(filtered.toArray(new String[filtered.size()]));

    }

    /**
     * Sets the unfiltered class list array and invokes an update.
     *
     * @param classesToReload String[] classesToReload.
     */
    public void reloadClassList(String[] classesToReload) {
        loadedClasses = Arrays.copyOf(classesToReload, classesToReload.length);
        SwingUtilities.invokeLater(() -> updateClassList());
    }

    /**
     * Sets the decompiled code into JTextArea
     *
     * @param decompiledClass String of source code of decompiler class
     */
    public void reloadTextField(String name, String decompiledClass, byte[] source) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BytecodeDecompilerView.this.setDecompiledClass(name, decompiledClass, source);
            }
        });
    }

    private void setDecompiledClass(String name, String data, byte[] source) {
        bytecodeSyntaxTextArea.setText(data);
        bytecodeSyntaxTextArea.setCaretPosition(0);

        try {
            hex.open(new ByteArrayInputStream(source));
        } catch (IOException ex) {
            OutputController.getLogger().log(ex);
        }
        this.lastDecompiledClass = name;
    }

    public void setClassesActionListener(ActionListener listener) {
        classesActionListener = listener;
    }


    public void setBytesActionListener(ActionListener listener) {
        bytesActionListener = listener;
    }

    private class RewriteActionListener implements ActionListener {

        private final VmDecompilerInformationController.ClassRewriter worker;

        public RewriteActionListener(VmDecompilerInformationController.ClassRewriter worker) {
            this.worker = worker;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            worker.rewriteClass(getSelectedDecompilerWrapperInformation(), BytecodeDecompilerView.this.lastDecompiledClass, BytecodeDecompilerView.this.bytecodeSyntaxTextArea.getText(), BytecodeDecompilerView.this.hex.get(), buffers.getSelectedIndex());
        }
    }

    public void setRewriteActionListener(VmDecompilerInformationController.ClassRewriter worker) {
        this.rewriteActionListener = new RewriteActionListener(worker);
    }

    public void refreshComboBox(List<DecompilerWrapperInformation> wrappers) {
        pluginComboBox.removeAllItems();
        wrappers.forEach(decompilerWrapperInformation -> {
            if (!decompilerWrapperInformation.isInvalidWrapper()) {
                pluginComboBox.addItem(decompilerWrapperInformation);
            }
        });
    }

    public DecompilerWrapperInformation getSelectedDecompilerWrapperInformation() {
        return (DecompilerWrapperInformation) pluginComboBox.getSelectedItem();
    }

    private void initialSearchBytecode(String query, boolean isRegex) {
        searchContext = new SearchContext();

        searchContext.setSearchFor(query);
        searchContext.setWholeWord(false);
        searchContext.setSearchWrap(true);
        searchContext.setRegularExpression(isRegex);

        deselectBytecodeSyntaxArea(); // avoid jumping to next location while typing one char at a time
        searchBytecode(true);
    }

    private void searchBytecode(boolean forward) {
        searchContext.setSearchForward(forward);
        SearchResult result = SearchEngine.find(bytecodeSyntaxTextArea, searchContext);

        if (!result.wasFound()) {
            bytecodeSearchControls.fireWasNotFoundAction();
        }
    }

    private void deselectBytecodeSyntaxArea() {
        int newDot = bytecodeSyntaxTextArea.getSelectionStart();
        bytecodeSyntaxTextArea.select(newDot, newDot);
    }

    public static Dimension buttonSizeBasedOnTextField(JButton originalButton, JTextField referenceTextField) {
        return new Dimension(originalButton.getPreferredSize().width, referenceTextField.getPreferredSize().height);
    }

    private void classWorker(String name) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent event = new ActionEvent(this, 1, name);
                    bytesActionListener.actionPerformed(event);
                } catch (Throwable t) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, t);
                }
                return null;
            }
        }.execute();
    }
}
