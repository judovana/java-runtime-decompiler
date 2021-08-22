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
import org.jrd.backend.core.OutputController;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
        private JPanel topButtonPanel;
            private JComboBox<DecompilerWrapperInformation> pluginComboBox;
        private JSplitPane splitPane;
            private JPanel classes;
                private JTextField classesSortField;
                    private final Color classesSortFieldColor;
                private JPanel classesPanel;
                    private JScrollPane classesScrollPane;
                        private JList<String> filteredClassesJList;

            private final JTabbedPane buffers;
                private JPanel sourceBuffer;
                    private JTextField bytecodeSearchField;
                    private RTextScrollPane bytecodeScrollPane;
                        private RSyntaxTextArea bytecodeSyntaxTextArea;
                private JPanel binaryBuffer;
                    private JPanel hexControls;
                    private HexEditor hex;
                    private JPanel hexSearchControls;

    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private RewriteActionListener rewriteActionListener;

    private String[] loadedClasses;
    private String lastDecompiledClass = "";

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
        classesSortField.setToolTipText(styleTooltip() + "Use regular expression; eg com.*. <br/>Dont forget to escape $ as \\$ otherwise it is end of line.<br/>Nothing shown? Try.*pkg.* or .*SomeClass.*<br/>Search java pattern for help<br/>For negation use ^(?!.*bar).*$  for or use | note, that or and negation do not like each other. Escape character is \\" +
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

        bytecodeSearchField = new JTextField();
        bytecodeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                searchCode();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                searchCode();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                searchCode();
            }
        });

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

        JButton overwriteButton = new JButton("Overwrite class");
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

        JButton topButton = new JButton("Refresh loaded classes list");
        topButton.addActionListener(new ActionListener() {
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

        topButtonPanel = new JPanel();

        topButtonPanel.setLayout(new BorderLayout());
        topButtonPanel.add(topButton, BorderLayout.WEST);
        topButtonPanel.add(overwriteButton);
        topButtonPanel.add(pluginComboBox, BorderLayout.EAST);

        classesScrollPane = new JScrollPane(filteredClassesJList);
        classesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        classesPanel.add(classesScrollPane);
        classes.add(classesPanel);

        buffers = new JTabbedPane();
        sourceBuffer.setName("Source buffer");
        sourceBuffer.add(bytecodeScrollPane);
        sourceBuffer.add(bytecodeSearchField, BorderLayout.NORTH);
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

        hexSearchControls = new JPanel(new GridLayout(1, 4));
        HexSearch hexSearchEngine = new HexSearch(hex);
        JComboBox<HexSearch.HexSearchOptions> hexSearchType = new JComboBox<HexSearch.HexSearchOptions>(HexSearch.HexSearchOptions.values());
        hexSearchControls.add(hexSearchType);
        JTextField hexSearch = new JTextField("");
        hexSearch.getDocument().addDocumentListener(new HexSearchDocumentListener(hexSearchEngine, hexSearch, hexSearchType));
        hexSearchControls.add(hexSearch);
        JButton hexPrev = new JButton("Previous");
        hexPrev.addActionListener(new HexSearchActionListener(hexSearchEngine, hexSearch, hexSearchType, HexSearchActionListener.Method.PREV));
        hexSearchControls.add(hexPrev);
        JButton hexNext = new JButton("Next");
        hexNext.addActionListener(new HexSearchActionListener(hexSearchEngine, hexSearch, hexSearchType, HexSearchActionListener.Method.NEXT));
        hexSearchControls.add(hexNext);
        binaryBuffer.add(hexSearchControls, BorderLayout.SOUTH);

        buffers.add(sourceBuffer);
        buffers.add(binaryBuffer);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                classes, buffers);

        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (splitPaneFirstResize) {
                    splitPane.setDividerLocation(0.5);
                    splitPaneFirstResize = false;
                }
            }
        });

        bytecodeDecompilerPanel.add(topButtonPanel, BorderLayout.NORTH);
        bytecodeDecompilerPanel.add(splitPane, BorderLayout.CENTER);

        bytecodeDecompilerPanel.setVisible(true);

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

    /**
     * Search string in decompiled code
     */
    private void searchCode() {
        SearchContext context = new SearchContext();
        String match = bytecodeSearchField.getText();
        context.setSearchFor(match);
        context.setWholeWord(false);
        SearchEngine.markAll(bytecodeSyntaxTextArea, context);
        int line = SearchEngine.getNextMatchPos(match, bytecodeSyntaxTextArea.getText(), true, true, false);
        if (line >= 0) {
            bytecodeSyntaxTextArea.setCaretPosition(line);
        }
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
