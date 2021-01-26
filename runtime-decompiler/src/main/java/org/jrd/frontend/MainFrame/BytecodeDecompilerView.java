package org.jrd.frontend.MainFrame;

import org.fife.ui.hex.swing.HexEditor;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that creates GUI for attached VM.
 */
public class BytecodeDecompilerView {

    private JPanel BytecodeDecompilerPanel;

    private final JTabbedPane srcBin;

    private JSplitPane splitPane;
    private JPanel leftMainPanel;
    private JTextField classesSortField;
    private final Color classesSortFieldColor;
    private JTextField searchCodeField;
    private JComboBox topComboBox;
    private JPanel classesPanel;
    private JPanel rightMainPanel;
    private JPanel rightBin;
    private JScrollPane leftScrollPanel;
    private JList<String> filteredClassesJlist;
    private RTextScrollPane bytecodeScrollPane;
    private RSyntaxTextArea bytecodeSyntaxTextArea;
    private HexEditor hex;
    private JPanel hexControls;
    private String lastDecompiledClass = "";
    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private RewriteActionListener rewriteActionListener;
    private String[] classes;

    private boolean splitPaneFirstResize = true;

    /**
     * Constructor creates the graphics and adds the action listeners.
     *
     * @return BytecodeDecompilerPanel
     */

    public JPanel getBytecodeDecompilerPanel() {
        return BytecodeDecompilerPanel;
    }


    public BytecodeDecompilerView() {

        BytecodeDecompilerPanel = new JPanel(new BorderLayout());

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

        searchCodeField = new JTextField();
        searchCodeField.getDocument().addDocumentListener(new DocumentListener() {
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

        filteredClassesJlist = new JList<>();
        filteredClassesJlist.setFixedCellHeight(20);
        filteredClassesJlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final String name = filteredClassesJlist.getSelectedValue();
                if (name != null || filteredClassesJlist.getSelectedIndex() != -1) {
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
        });

        JButton overwriteButton = new JButton("Overwrite class");
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String name = filteredClassesJlist.getSelectedValue();
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

        topComboBox = new JComboBox<DecompilerWrapperInformation>();
        topComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (filteredClassesJlist.getSelectedIndex() != -1) {
                    ActionEvent event = new ActionEvent(this, 1, filteredClassesJlist.getSelectedValue());
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

        leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new BorderLayout());
        leftMainPanel.setBorder(new EtchedBorder());

        rightMainPanel = new JPanel();
        rightMainPanel.setLayout(new BorderLayout());
        rightMainPanel.setBorder(new EtchedBorder());

        rightBin = new JPanel();
        rightBin.setLayout(new BorderLayout());
        rightBin.setBorder(new EtchedBorder());

        JPanel topButtonPanel = new JPanel();

        topButtonPanel.setLayout(new BorderLayout());
        topButtonPanel.add(topButton, BorderLayout.WEST);
        topButtonPanel.add(overwriteButton);
        topButtonPanel.add(topComboBox, BorderLayout.EAST);

        leftScrollPanel = new JScrollPane(filteredClassesJlist);
        leftScrollPanel.getVerticalScrollBar().setUnitIncrement(20);

        classesPanel.add(leftScrollPanel);
        leftMainPanel.add(classesPanel);

        srcBin = new JTabbedPane();
        rightMainPanel.setName("source buffer");
        rightMainPanel.add(bytecodeScrollPane);
        rightMainPanel.add(searchCodeField, BorderLayout.NORTH);
        rightBin.setName("Binary buffer");
        rightBin.add(hex);
        rightBin.add(hexControls, BorderLayout.NORTH);
        hexControls.setLayout(new GridLayout(1, 2));
        JButton undo = new JButton("Undo");
        hexControls.add(undo);
        JButton redo = new JButton("Redo");
        hexControls.add(redo);
        undo.addActionListener(actionEvent -> hex.undo());
        redo.addActionListener(actionEvent -> hex.redo());

        JPanel hexSearchControls = new JPanel(new GridLayout(1, 4));
        JComboBox hexSearchType = new JComboBox(new String[]{"hex", "int", "text"});
        hexSearchControls.add(hexSearchType);
        JTextField hexSearch = new JTextField("todo: just convert and search in byteBuffer of fife.ui.hex");
        hexSearchControls.add(hexSearch);
        JButton hexPrev = new JButton("prev");
        hexSearchControls.add(hexPrev);
        JButton hexNext = new JButton("next");
        hexSearchControls.add(hexNext);
        rightBin.add(hexSearchControls, BorderLayout.SOUTH);

        srcBin.add(rightMainPanel);
        srcBin.add(rightBin);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftMainPanel, srcBin);

        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (splitPaneFirstResize) {
                    splitPane.setDividerLocation(0.5);
                    splitPaneFirstResize = false;
                }
            }
        });

        BytecodeDecompilerPanel.add(topButtonPanel, BorderLayout.NORTH);
        BytecodeDecompilerPanel.add(splitPane, BorderLayout.CENTER);

        BytecodeDecompilerPanel.setVisible(true);

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
            for (String classe : classes) {
                Matcher m = p.matcher(classe);
                if (m.matches()) {
                    filtered.add(classe);
                }
            }
        }catch(Exception ex){
            classesSortField.setForeground(Color.red);
            classesSortField.repaint();
            for (String classe : classes) {
                if (!classe.contains(filter)) {
                    filtered.add(classe);
                }
            }
        }
        filteredClassesJlist.setListData(filtered.toArray(new String[filtered.size()]));

    }

    /**
     * Sets the unfiltered class list array and invokes an update.
     *
     * @param classesToReload String[] classesToReload.
     */
    public void reloadClassList(String[] classesToReload) {
        classes = classesToReload;
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
            worker.rewriteClass(getSelecteddecompilerWrapperInformation(), BytecodeDecompilerView.this.lastDecompiledClass, BytecodeDecompilerView.this.bytecodeSyntaxTextArea.getText(), BytecodeDecompilerView.this.hex.get(), srcBin.getSelectedIndex());
        }
    }

    public void setRewriteActionListener(VmDecompilerInformationController.ClassRewriter worker) {
        this.rewriteActionListener = new RewriteActionListener(worker);
    }

    /**
     * Creates a warning table in case of error.
     *
     * @param msg message
     */
    public void handleError(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                //JOptionPane.showMessageDialog(getUiComponent().getParent(), msg, " ", JOptionPane.WARNING_MESSAGE);
            }

        });
    }

    public void refreshComboBox(List<DecompilerWrapperInformation> wrappers) {
        topComboBox.removeAllItems();
        wrappers.forEach(decompilerWrapperInformation -> {
            if (!decompilerWrapperInformation.isInvalidWrapper()) {
                topComboBox.addItem(decompilerWrapperInformation);
            }
        });
    }

    public DecompilerWrapperInformation getSelecteddecompilerWrapperInformation() {
        return (DecompilerWrapperInformation) topComboBox.getSelectedItem();
    }

    /**
     * Search string in decompiled code
     */
    private void searchCode() {
        SearchContext context = new SearchContext();
        String match = searchCodeField.getText();
        context.setSearchFor(match);
        context.setWholeWord(false);
        SearchEngine.markAll(bytecodeSyntaxTextArea, context);
        int line = SearchEngine.getNextMatchPos(match, bytecodeSyntaxTextArea.getText(), true, true, false);
        if (line >= 0) {
            bytecodeSyntaxTextArea.setCaretPosition(line);
        }
    }
}
