package org.jrd.frontend.frame.main;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
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
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.Logger;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.frontend.frame.main.popup.ClassListPopupMenu;
import org.jrd.frontend.frame.main.renderer.ClassListRenderer;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that creates GUI for attached VM.
 */
@SuppressWarnings("Indentation") // indented Swing components greatly help with orientation
public class BytecodeDecompilerView {

    private JPanel bytecodeDecompilerPanel;
        private JSplitPane splitPane;
            private JPanel classes;
                private JPanel classesToolBar;
                    private JButton reloadClassesButton;
                    private JCheckBox showInfoCheckBox;
                    private JTextField classesSortField;
                        private final Color classesSortFieldColor;
                private JPanel classesPanel;
                    private JScrollPane classesScrollPane;
                        private JList<ClassInfo> filteredClassesJList;
                            private ClassListRenderer filteredClassesRenderer;
            private JPanel buffersPanel;
                private JPanel buffersToolBar;
                    private JButton undoButton;
                    private JButton redoButton;
                    private JButton insertButton;
                    private JButton detachButton;
                    private JButton initClassButton;
                    private JButton overwriteButton;
                    private JButton compileButton;
                    private JButton compileAndUploadButton;
                    private JComboBox<DecompilerWrapper> pluginComboBox;
                private final JTabbedPane buffers;
                    private JPanel sourceBuffer;
                        private RTextScrollPane bytecodeScrollPane;
                            private RSyntaxTextArea bytecodeSyntaxTextArea;
                        private SearchControlsPanel bytecodeSearchControls;
                    private JPanel binaryBuffer;
                        private HexEditor hex;
                        private SearchControlsPanel hexSearchControls;

    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private ActionListener initActionListener;
    private DecompilationController.QuickCompiler compileAction;
    private OverwriteActionListener overwriteActionListener;
    private DecompilationController.AgentApiGenerator popup;

    private ClassInfo[] loadedClasses;
    private String lastDecompiledClass = "";
    private String lastFqn = "java.lang.Override";

    private SearchContext searchContext;

    private boolean splitPaneFirstResize = true;
    private boolean shouldAttach = false;

    private final JFrame mainFrame;
    private JFrame detachedBytecodeFrame;

    private static final Set<Integer> CLASS_LIST_REGISTERED_KEY_CODES = Set.of(
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_ENTER
    );
    private static final Insets PANEL_INSETS = new Insets(3, 3, 3, 3);
    private static final String DETACH_BUTTON_TEXT = "Detach";
    private static final String ATTACH_BUTTON_TEXT = "Attach";

    /**
     * Constructor creates the graphics and adds the action listeners.
     *
     * @return BytecodeDecompilerPanel
     */

    public JPanel getBytecodeDecompilerPanel() {
        return bytecodeDecompilerPanel;
    }


    public BytecodeDecompilerView(JFrame mainFrameReference) {
        mainFrame = mainFrameReference;

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

        classesPanel.add(classesSortField, BorderLayout.NORTH);

        filteredClassesRenderer = new ClassListRenderer();

        filteredClassesJList = new JList<>();
        filteredClassesJList.setCellRenderer(filteredClassesRenderer);
        filteredClassesJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        filteredClassesJList.addMouseListener(new MouseAdapter() {
            private int originallySelected = -1;

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    originallySelected = filteredClassesJList.getSelectedIndex(); // should be 1 index only, because of SINGLE_SELECTION
                    filteredClassesJList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                    filteredClassesJList.setSelectedIndex(filteredClassesJList.locationToIndex(e.getPoint()));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    final String name = filteredClassesJList.getSelectedValue().getName();
                    if (name != null || filteredClassesJList.getSelectedIndex() != -1) {
                        bytesWorker(name);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    ClassListPopupMenu.create(filteredClassesJList, originallySelected, doShowClassInfo())
                        .show(filteredClassesJList, e.getX(), e.getY());
                }
            }
        });
        // unfortunately MouseAdapter's mouseDragged() does not get triggered on a JList, hence this 2nd listener
        filteredClassesJList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int indexUnderMouse = filteredClassesJList.locationToIndex(e.getPoint());
                    int minSelectedIndex = filteredClassesJList.getMinSelectionIndex();
                    int maxSelectedIndex = filteredClassesJList.getMaxSelectionIndex();

                    if (minSelectedIndex < indexUnderMouse && indexUnderMouse < maxSelectedIndex) {
                        filteredClassesJList.removeSelectionInterval(indexUnderMouse, maxSelectedIndex);
                    } else {
                        filteredClassesJList.addSelectionInterval(minSelectedIndex, indexUnderMouse);
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
            }
        });

        filteredClassesJList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (CLASS_LIST_REGISTERED_KEY_CODES.contains(e.getKeyCode())) {
                    final String name = filteredClassesJList.getSelectedValue().getName();
                    if (name != null || filteredClassesJList.getSelectedIndex() != -1) {
                        bytesWorker(name);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    filteredClassesJList.clearSelection();
                }
            }
        });

        detachButton = ImageButtonFactory.createDetachButton();
        detachButton.addActionListener(e -> handleBuffersDetaching());

        initClassButton = ImageButtonFactory.createInitButton();
        initClassButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final String fqn = JOptionPane.showInputDialog(
                        mainFrameReference, "Enter the fully qualified name of a class to initialize", lastFqn
                );

                if (fqn != null) {
                    lastFqn = fqn;
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                ActionEvent event = new ActionEvent(this, 4, fqn);
                                initActionListener.actionPerformed(event);
                            } catch (Throwable t) {
                                Logger.getLogger().log(Logger.Level.ALL, t);
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        });

        overwriteButton = ImageButtonFactory.createOverwriteButton();
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String name = filteredClassesJList.getSelectedValue().getName();
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            ActionEvent event = new ActionEvent(this, 3, name);
                            overwriteActionListener.actionPerformed(event);
                        } catch (Throwable t) {
                            Logger.getLogger().log(Logger.Level.ALL, t);
                        }
                        return null;
                    }
                }.execute();
            }
        });

        compileButton = ImageButtonFactory.createCompileButton();
        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                GlobalConsole.getConsole().show();
                compileAction.run(
                        (DecompilerWrapper) pluginComboBox.getSelectedItem(),
                        false,
                        new IdentifiedSource(new ClassIdentifier(lastDecompiledClass),
                                bytecodeSyntaxTextArea.getText().getBytes(StandardCharsets.UTF_8)));
            }
        });

        compileAndUploadButton = ImageButtonFactory.createCompileUploadButton();
        compileAndUploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                GlobalConsole.getConsole().show();
                if (!isSourceBufferVisible()) {
                    compileAction.upload(lastDecompiledClass, hex.get());
                } else {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(),
                            true,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass),
                                    bytecodeSyntaxTextArea.getText().getBytes(StandardCharsets.UTF_8)));
                }
            }
        });

        reloadClassesButton = ImageButtonFactory.createRefreshButton("Refresh classes");
        reloadClassesButton.addActionListener(e -> classWorker());

        showInfoCheckBox = new JCheckBox("Show detailed class info");
        showInfoCheckBox.addActionListener(event -> handleClassInfoSwitching());

        buffers = new JTabbedPane();
        buffers.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                if (isSourceBufferVisible()) {
                    compileButton.setEnabled(true);
                    insertButton.setEnabled(true);
                } else {
                    compileButton.setEnabled(false);
                    insertButton.setEnabled(false);
                }
            }
        });
        undoButton = ImageButtonFactory.createUndoButton();
        undoButton.addActionListener(actionEvent -> {
            if (isSourceBufferVisible()) {
                bytecodeSyntaxTextArea.undoLastAction();
            } else {
                hex.undo();
            }
        });

        redoButton = ImageButtonFactory.createRedoButton();
        redoButton.addActionListener(actionEvent -> {
            if (isSourceBufferVisible()) {
                bytecodeSyntaxTextArea.redoLastAction();
            } else {
                hex.redo();
            }
        });

        insertButton = ImageButtonFactory.createBookButton("Insert agent API to current position");
        insertButton.addActionListener(actionEvent -> {
            if (isSourceBufferVisible()) {
                showApiMenu(new Point(0, 0));
            } else {
                Logger.getLogger().log(Logger.Level.ALL, "Unable to insert agent API into binary buffer.");
            }
        });

        classesToolBar = new JPanel(new GridBagLayout());
        classesToolBar.setBorder(new EtchedBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = PANEL_INSETS;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.weightx = 0;
        classesToolBar.add(reloadClassesButton, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        classesToolBar.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        classesToolBar.add(showInfoCheckBox, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        classesToolBar.add(classesSortField, gbc);

        pluginComboBox = new JComboBox<DecompilerWrapper>();
        pluginComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (filteredClassesJList.getSelectedIndex() != -1) {
                    ActionEvent event = new ActionEvent(this, 1, filteredClassesJList.getSelectedValue().getName());
                    bytesActionListener.actionPerformed(event);
                }
            }
        });

        bytecodeSyntaxTextArea = new RSyntaxTextArea();
        bytecodeSyntaxTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                        showApiMenu(null);
                    }
                }
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        showApiMenu(null);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        bytecodeSearchControls.focus();
                    }
                }
            }
        });
        bytecodeSyntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        bytecodeSyntaxTextArea.setCodeFoldingEnabled(true);
        bytecodeScrollPane = new RTextScrollPane(bytecodeSyntaxTextArea);
        bytecodeSearchControls = SearchControlsPanel.createBytecodeControls(this);

        hex = new HexEditor();
        hex.addKeyListenerToTable(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        hexSearchControls.focus();
                    }
                }
            }
        });
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
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = PANEL_INSETS;
        gbc.weightx = 0;
        buffersToolBar.add(undoButton, gbc);

        gbc.gridx = 1;
        buffersToolBar.add(redoButton, gbc);

        gbc.gridx = 2;
        buffersToolBar.add(insertButton, gbc);

        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0); // prevent double padding when no glue is utilized
        gbc.gridx = 2;
        buffersToolBar.add(Box.createHorizontalGlue(), gbc);

        gbc.insets = PANEL_INSETS;
        gbc.weightx = 0;
        gbc.gridx = 3;
        buffersToolBar.add(detachButton, gbc);
        gbc.gridx = 4;
        buffersToolBar.add(initClassButton, gbc);
        gbc.gridx = 5;
        buffersToolBar.add(overwriteButton, gbc);
        gbc.gridx = 6;
        buffersToolBar.add(compileButton, gbc);
        gbc.gridx = 7;
        buffersToolBar.add(compileAndUploadButton, gbc);
        gbc.gridx = 8;
        buffersToolBar.add(pluginComboBox, gbc);

        classesScrollPane = new JScrollPane(filteredClassesJList);
        classesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        classesPanel.add(classesToolBar, BorderLayout.NORTH);
        classesPanel.add(classesScrollPane, BorderLayout.CENTER);
        classes.add(classesPanel);

        sourceBuffer.setName("Source buffer");
        sourceBuffer.add(bytecodeScrollPane);
        sourceBuffer.add(bytecodeSearchControls, BorderLayout.SOUTH);
        binaryBuffer.setName("Binary buffer");
        binaryBuffer.add(hex);

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
                    splitPane.setDividerLocation(0.35);
                    splitPaneFirstResize = false;
                }
            }
        });

        bytecodeDecompilerPanel.add(splitPane, BorderLayout.CENTER);

        bytecodeDecompilerPanel.setVisible(true);

    }

    private void handleClassInfoSwitching() {
        classWorker();

        filteredClassesRenderer.setDoShowInfo(doShowClassInfo());

        // invalidate JList cache
        filteredClassesJList.setFixedCellWidth(1);
        filteredClassesJList.setFixedCellWidth(-1);

        filteredClassesJList.ensureIndexIsVisible(filteredClassesJList.getSelectedIndex());

        filteredClassesJList.revalidate();
        filteredClassesJList.repaint();

        updateClassList(); // reinterpret current search
    }

    private static class UndoRedoKeyAdapter extends KeyAdapter {
        private UndoManager undoManager = new UndoManager();

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

    private static final class SearchControlsPanel extends JPanel {
        private final JTextField searchField = new JTextField("");
        private final JButton previousButton = new JButton("Previous");
        private final JButton nextButton = new JButton("Next");
        private final Color originalSearchFieldColor = searchField.getForeground();

        private final ActionListener wasNotFoundActionListener;

        private SearchControlsPanel(Component optionsComponent, Component forFocus) {
            super(new GridBagLayout());

            searchField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        forFocus.requestFocus();
                    }
                }
            });
            Timer wasNotFoundTimer = new Timer(
                    250, (ActionEvent e) -> searchField.setForeground(originalSearchFieldColor)
            );
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
            gbc.insets = new Insets(3, 3, 3, 3);

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

            SearchControlsPanel controls = new SearchControlsPanel(hexSearchType, hex.getTableFocus());

            DocumentListener hexSearchDocumentListener = new HexSearchDocumentListener(
                    hexSearchEngine, controls.searchField, hexSearchType, controls.wasNotFoundActionListener
            );
            controls.searchField.getDocument().addDocumentListener(hexSearchDocumentListener);
            controls.previousButton.addActionListener(new HexSearchActionListener(
                    hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.PREV)
            );
            controls.nextButton.addActionListener(new HexSearchActionListener(
                    hexSearchEngine, controls.searchField, hexSearchType, HexSearchActionListener.Method.NEXT)
            );
            hexSearchType.addActionListener(event -> hexSearchDocumentListener.changedUpdate(null));

            return controls;
        }

        public static SearchControlsPanel createBytecodeControls(BytecodeDecompilerView parent) {
            final JCheckBox regexCheckBox = new JCheckBox("Regex");
            regexCheckBox.setIconTextGap(3);
            final JCheckBox caseCheckBox = new JCheckBox("Match case");
            caseCheckBox.setIconTextGap(3);

            JPanel checkboxes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            checkboxes.add(regexCheckBox);
            checkboxes.add(caseCheckBox);

            SearchControlsPanel controls = new SearchControlsPanel(checkboxes, parent.bytecodeSyntaxTextArea);

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
                    SwingUtilities.invokeLater(() -> parent.initialSearchBytecode(
                                controls.searchField.getText(), regexCheckBox.isSelected(), caseCheckBox.isSelected()
                    ));
                }
            };
            regexCheckBox.addActionListener(actionEvent -> listener.changedUpdate(null));
            caseCheckBox.addActionListener(actionEvent -> listener.changedUpdate(null));

            controls.searchField.getDocument().addDocumentListener(listener);
            controls.previousButton.addActionListener(e -> parent.searchBytecode(false));
            controls.nextButton.addActionListener(e -> parent.searchBytecode(true));

            return controls;
        }

        public void fireWasNotFoundAction() {
            wasNotFoundActionListener.actionPerformed(null);
        }

        public void focus() {
            searchField.requestFocus();
        }
    }

    private boolean isSourceBufferVisible() {
        return buffers.getSelectedComponent().equals(sourceBuffer);
    }

    private void handleBuffersDetaching() {
        if (shouldAttach) {
            shouldAttach = false;
            detachedBytecodeFrame.dispatchEvent(new WindowEvent(detachedBytecodeFrame, WindowEvent.WINDOW_CLOSING));
            return;
        }

        shouldAttach = true;
        detachedBytecodeFrame = new JFrame("Bytecode");

        ImageButtonFactory.flipDetachButton(detachButton, shouldAttach, ATTACH_BUTTON_TEXT);
        splitPane.remove(buffersPanel);
        splitPane.setEnabled(false); // disable slider of the now one-item split pane
        detachedBytecodeFrame.add(buffersPanel);

        detachedBytecodeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        detachedBytecodeFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                mainFrame.setSize(mainFrame.getWidth() + buffersPanel.getWidth(), mainFrame.getHeight());
                ImageButtonFactory.flipDetachButton(detachButton, shouldAttach, DETACH_BUTTON_TEXT);

                splitPane.setEnabled(true);
                splitPane.add(buffersPanel);
                splitPane.setDividerLocation(0.35);
            }
        });

        detachedBytecodeFrame.setSize(buffersPanel.getWidth(), mainFrame.getHeight());
        mainFrame.setSize(mainFrame.getWidth() - buffersPanel.getWidth(), mainFrame.getHeight());
        ScreenFinder.moveWindowNextTo(mainFrame, detachedBytecodeFrame);
        detachedBytecodeFrame.setVisible(true);
    }

    public static String styleTooltip() {
        return "<html>" + "<div style='background:yellow;color:black'>";
    }

    private void updateClassList() {
        List<ClassInfo> filtered = new ArrayList<>();
        String filter = classesSortField.getText().trim();
        if (filter.isEmpty()) {
            filter = ".*";
        }

        try {
            Pattern p = Pattern.compile(filter);
            classesSortField.setForeground(classesSortFieldColor);
            classesSortField.repaint();

            for (ClassInfo clazz : loadedClasses) {
                Matcher m = p.matcher(clazz.getSearchableString(doShowClassInfo()));
                if (m.matches()) {
                    filtered.add(clazz);
                }
            }
        } catch (Exception ex) {
            classesSortField.setForeground(Color.red);
            classesSortField.repaint();

            // regex is invalid => just use .contains()
            for (ClassInfo clazz : loadedClasses) {
                if (!clazz.getSearchableString(doShowClassInfo()).contains(filter)) {
                    filtered.add(clazz);
                }
            }
        }

        ClassInfo originalSelection = filteredClassesJList.getSelectedValue();
        filteredClassesJList.setListData(filtered.toArray(new ClassInfo[0]));
        filteredClassesJList.setSelectedValue(originalSelection, true);

        // setSelectedValue with null or a value that isn't in the list results in the selection being cleared
        if (filteredClassesJList.getSelectedIndex() == -1) {
            classesScrollPane.getVerticalScrollBar().setValue(0);
        }
    }

    /**
     * Sets the unfiltered class list array and invokes an update.
     *
     * @param classesToReload String[] classesToReload.
     */
    public void reloadClassList(ClassInfo[] classesToReload) {
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
        bytecodeSyntaxTextArea.discardAllEdits(); // makes the bytecode upload not undoable
        bytecodeSyntaxTextArea.setCaretPosition(0);

        try {
            hex.open(new ByteArrayInputStream(source));
        } catch (IOException ex) {
            Logger.getLogger().log(ex);
        }
        this.lastDecompiledClass = name;
    }

    public void setClassesActionListener(ActionListener listener) {
        classesActionListener = listener;
    }

    public void setInitActionListener(ActionListener listener) {
        initActionListener = listener;
    }

    public void setCompileListener(DecompilationController.QuickCompiler listener) {
        compileAction = listener;
    }

    public void setBytesActionListener(ActionListener listener) {
        bytesActionListener = listener;
    }

    public void setPopup(DecompilationController.AgentApiGenerator ap) {
        popup = ap;
    }

    private class OverwriteActionListener implements ActionListener {

        private final DecompilationController.ClassOverwriter worker;

        OverwriteActionListener(DecompilationController.ClassOverwriter worker) {
            this.worker = worker;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            worker.overwriteClass(
                    getSelectedDecompiler(),
                    BytecodeDecompilerView.this.lastDecompiledClass,
                    BytecodeDecompilerView.this.bytecodeSyntaxTextArea.getText(),
                    BytecodeDecompilerView.this.hex.get(),
                    !isSourceBufferVisible()
            );
        }
    }

    public void setOverwriteActionListener(DecompilationController.ClassOverwriter worker) {
        this.overwriteActionListener = new OverwriteActionListener(worker);
    }

    public void refreshComboBox(List<DecompilerWrapper> wrappers) {
        pluginComboBox.removeAllItems();
        wrappers.forEach(wrapper -> {
            if (!wrapper.isInvalidWrapper()) {
                pluginComboBox.addItem(wrapper);
            }
        });
    }

    public DecompilerWrapper getSelectedDecompiler() {
        return (DecompilerWrapper) pluginComboBox.getSelectedItem();
    }

    private void initialSearchBytecode(String query, boolean isRegex, boolean matchesCase) {
        searchContext = new SearchContext();

        searchContext.setSearchFor(query);
        searchContext.setWholeWord(false);
        searchContext.setSearchWrap(true);
        searchContext.setRegularExpression(isRegex);
        searchContext.setMatchCase(matchesCase);

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

    private void showApiMenu(Point forcedLocation) {
        Point caretPosition = bytecodeSyntaxTextArea.getCaret().getMagicCaretPosition();
        if (caretPosition == null || forcedLocation != null) {
            caretPosition = forcedLocation;
        }
        if (caretPosition == null) {
            return;
        }
        popup.getFor(bytecodeSyntaxTextArea, forcedLocation == null).show(
                bytecodeSyntaxTextArea,
                caretPosition.x,
                caretPosition.y + bytecodeSyntaxTextArea.getFontMetrics(
                        bytecodeSyntaxTextArea.getFont()).getHeight() // offset to the next row
        );
    }

    public static Dimension buttonSizeBasedOnTextField(JButton originalButton, JTextField referenceTextField) {
        return new Dimension(originalButton.getPreferredSize().width, referenceTextField.getPreferredSize().height);
    }

    private void classWorker() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent event = new ActionEvent(this, 2, null);
                    classesActionListener.actionPerformed(event);
                } catch (Throwable t) {
                    Logger.getLogger().log(Logger.Level.ALL, t);
                }
                return null;
            }
        }.execute();
    }

    private void bytesWorker(String name) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent event = new ActionEvent(this, 1, name);
                    bytesActionListener.actionPerformed(event);
                } catch (Throwable t) {
                    Logger.getLogger().log(Logger.Level.ALL, t);
                }
                return null;
            }
        }.execute();
    }

    public boolean doShowClassInfo() {
        return showInfoCheckBox.isSelected();
    }
}
