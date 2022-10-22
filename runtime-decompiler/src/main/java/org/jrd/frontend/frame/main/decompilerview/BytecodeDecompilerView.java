package org.jrd.frontend.frame.main.decompilerview;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.DependenciesReader;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.popup.ClassListPopupMenu;
import org.jrd.frontend.frame.main.popup.DiffPopup;
import org.jrd.frontend.frame.main.renderer.ClassListRenderer;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    private JButton diffButton;
    private JButton undoButton;
    private JButton redoButton;
    private JButton insertButton;
    private JButton bytecodeButton;
    private JButton detachButton;
    private JButton initClassButton;
    private JButton overwriteButton;
    private JButton compileButton;
    private JButton compileAndUploadButton;
    private JComboBox<DecompilerWrapper> pluginComboBox;
    private final JTabbedPane buffers;

    private TextWithControls bytecodeBuffer;
    private HexWithControls binary;

    private TextWithControls additionalSrcBuffer;
    private TextWithControls additionalBytecodeBuffer;
    private HexWithControls additionalBinary;

    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private ActionListener initActionListener;
    private DecompilationController.QuickCompiler compileAction;
    private OverwriteActionListener overwriteActionListener;
    private DependenciesReader dependenciesReader;

    private ClassInfo[] loadedClasses;
    private String lastDecompiledClass = "";
    private String lastFqn = "java.lang.Override";

    private boolean splitPaneFirstResize = true;
    private boolean shouldAttach = false;

    private final JFrame mainFrame;
    private JFrame detachedBytecodeFrame;

    private static final Set<Integer> CLASS_LIST_REGISTERED_KEY_CODES =
            Set.of(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_ENTER);
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
        classesSortField.setToolTipText(
                styleTooltip() + "Search for classes using regular expressions.<br/>" +
                        "Look for specific classes or packages using '.*SomeClass.*' or '.*some.package.*'<br/>" +
                        "Don't forget to escape dollar signs '$' of inner classes to '\\$'.<br/>" +
                        "For negation use the negative lookahead '^(?!.*unwanted).*$' syntax." + "</div><html>"
        );
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
        UndoRedoKeyAdapter classesSortFieldKeyAdapter = new UndoRedoKeyAdapter(null);
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
                    new ClassListPopupMenu<>(filteredClassesJList, originallySelected, doShowClassInfo(), getDependenciesReader())
                            .addItem("name(s)", ClassInfo::getName, true).addItem("location(s)", ClassInfo::getLocation, false)
                            .addItem("class loader(s)", ClassInfo::getClassLoader, false).show(filteredClassesJList, e.getX(), e.getY());
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
                final String fqn =
                        JOptionPane.showInputDialog(mainFrameReference, "Enter the fully qualified name of a class to initialize", lastFqn);

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
                Logger.getLogger().log("Compilation started");
                if (isAdditionalBinaryBufferVisible()) {
                    JOptionPane.showMessageDialog(buffers, "Unlike (compile) and upload, compile is onloy for source buffers");
                } else if (isBinaryBufferVisible()) {
                    JOptionPane.showMessageDialog(buffers, "Unlike (compile) and upload, compile is onloy for source buffers");
                } else if (isDecompiledBytecodeBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), false,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), bytecodeBuffer.getTextAsBytes())
                    );
                } else if (isAdditionalDecompiledBytecodeBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), false,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), additionalBytecodeBuffer.getTextAsBytes())
                    );
                } else if (isAdditionalSrcBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), false,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), additionalSrcBuffer.getTextAsBytes())
                    );
                } else {
                    JOptionPane.showMessageDialog(buffers, "nothing selected - " + buffers.getSelectedComponent());
                }
            }
        });

        compileAndUploadButton = ImageButtonFactory.createCompileUploadButton();
        compileAndUploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                GlobalConsole.getConsole().show();
                Logger.getLogger().log("Compilation with upload started");
                if (isAdditionalBinaryBufferVisible()) {
                    compileAction.upload(lastDecompiledClass, additionalBinary.get());
                } else if (isBinaryBufferVisible()) {
                    compileAction.upload(lastDecompiledClass, binary.get());
                } else if (isDecompiledBytecodeBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), true,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), bytecodeBuffer.getTextAsBytes())
                    );
                } else if (isAdditionalDecompiledBytecodeBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), true,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), additionalBytecodeBuffer.getTextAsBytes())
                    );
                } else if (isAdditionalSrcBufferVisible()) {
                    compileAction.run(
                            (DecompilerWrapper) pluginComboBox.getSelectedItem(), true,
                            new IdentifiedSource(new ClassIdentifier(lastDecompiledClass), additionalSrcBuffer.getTextAsBytes())
                    );
                } else {
                    JOptionPane.showMessageDialog(buffers, "nothing selected - " + buffers.getSelectedComponent());
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
                if (isDecompiledBytecodeBufferVisible() || isAdditionalDecompiledBytecodeBufferVisible() ||
                        isAdditionalSrcBufferVisible()) {
                    compileButton.setEnabled(true);
                    insertButton.setEnabled(true);
                } else if (isBinaryBufferVisible() || isAdditionalBinaryBufferVisible()) {
                    compileButton.setEnabled(false);
                    insertButton.setEnabled(false);
                }
            }
        });
        diffButton = new JButton("Df");
        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new DiffPopup(buffers.getComponents(), lastDecompiledClass).show(diffButton, 0, 0);
            }
        });
        undoButton = ImageButtonFactory.createUndoButton();
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (BytecodeDecompilerView.this.isDecompiledBytecodeBufferVisible()) {
                    bytecodeBuffer.undoLastAction();
                }
                if (BytecodeDecompilerView.this.isAdditionalDecompiledBytecodeBufferVisible()) {
                    additionalBytecodeBuffer.undoLastAction();
                }
                if (BytecodeDecompilerView.this.isAdditionalSrcBufferVisible()) {
                    additionalSrcBuffer.undoLastAction();
                } else if (BytecodeDecompilerView.this.isBinaryBufferVisible()) {
                    binary.undo();
                } else if (BytecodeDecompilerView.this.isAdditionalBinaryBufferVisible()) {
                    additionalBinary.undo();
                }
            }
        });

        redoButton = ImageButtonFactory.createRedoButton();
        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (BytecodeDecompilerView.this.isDecompiledBytecodeBufferVisible()) {
                    bytecodeBuffer.redoLastAction();
                }
                if (BytecodeDecompilerView.this.isAdditionalDecompiledBytecodeBufferVisible()) {
                    additionalBytecodeBuffer.redoLastAction();
                }
                if (BytecodeDecompilerView.this.isAdditionalSrcBufferVisible()) {
                    additionalSrcBuffer.redoLastAction();
                } else if (BytecodeDecompilerView.this.isBinaryBufferVisible()) {
                    binary.redo();
                } else if (BytecodeDecompilerView.this.isAdditionalBinaryBufferVisible()) {
                    additionalBinary.redo();
                }
            }
        });

        insertButton = ImageButtonFactory.createEditButton("Insert agent API to current position");
        insertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (BytecodeDecompilerView.this.isDecompiledBytecodeBufferVisible()) {
                    bytecodeBuffer.showApiMenu(new Point(0, 0));
                } else if (BytecodeDecompilerView.this.isAdditionalDecompiledBytecodeBufferVisible()) {
                    additionalBytecodeBuffer.showApiMenu(new Point(0, 0));
                } else if (BytecodeDecompilerView.this.isAdditionalSrcBufferVisible()) {
                    additionalSrcBuffer.showApiMenu(new Point(0, 0));
                } else if (BytecodeDecompilerView.this.isBinaryBufferVisible() ||
                        BytecodeDecompilerView.this.isAdditionalBinaryBufferVisible()) {
                    Logger.getLogger().log(Logger.Level.ALL, "Unable to insert agent API into binary buffer.");
                } else {
                    JOptionPane.showMessageDialog(null, "weird selected component: " + buffers.getSelectedComponent());
                }
            }
        });

        bytecodeButton = new JButton("0");
        bytecodeButton.setBorder(new EmptyBorder(5, 5, 5, 5));

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

        bytecodeBuffer = new TextWithControls("Source buffer");
        binary = new HexWithControls("Binary buffer");
        additionalBytecodeBuffer = new TextWithControls("Additional source buffer");
        additionalBinary = new HexWithControls("Additional binary buffer");
        additionalSrcBuffer = new TextWithControls("Additional source");

        classes = new JPanel();
        classes.setLayout(new BorderLayout());
        classes.setBorder(new EtchedBorder());

        buffersToolBar = new JPanel(new GridBagLayout());
        buffersToolBar.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = PANEL_INSETS;
        gbc.weightx = 0;
        buffersToolBar.add(diffButton, gbc);

        gbc.gridx = 1;
        buffersToolBar.add(undoButton, gbc);

        gbc.gridx = 2;
        buffersToolBar.add(redoButton, gbc);

        gbc.gridx = 3;
        buffersToolBar.add(insertButton, gbc);

        gbc.gridx = 4;
        buffersToolBar.add(bytecodeButton, gbc);

        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0); // prevent double padding when no glue is utilized
        gbc.gridx = 4;
        buffersToolBar.add(Box.createHorizontalGlue(), gbc);

        gbc.insets = PANEL_INSETS;
        gbc.weightx = 0;
        gbc.gridx = 5;
        buffersToolBar.add(detachButton, gbc);
        gbc.gridx = 6;
        buffersToolBar.add(initClassButton, gbc);
        gbc.gridx = 7;
        buffersToolBar.add(overwriteButton, gbc);
        gbc.gridx = 8;
        buffersToolBar.add(compileButton, gbc);
        gbc.gridx = 9;
        buffersToolBar.add(compileAndUploadButton, gbc);
        gbc.gridx = 10;
        buffersToolBar.add(pluginComboBox, gbc);

        classesScrollPane = new JScrollPane(filteredClassesJList);
        classesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        classesPanel.add(classesToolBar, BorderLayout.NORTH);
        classesPanel.add(classesScrollPane, BorderLayout.CENTER);
        classes.add(classesPanel);

        buffers.add(bytecodeBuffer);
        buffers.add(binary);
        buffers.add(additionalBytecodeBuffer);
        buffers.add(additionalBinary);
        buffers.add(additionalSrcBuffer);

        buffersPanel = new JPanel(new BorderLayout());
        buffersPanel.setBorder(new EtchedBorder());
        buffersPanel.add(buffersToolBar, BorderLayout.NORTH);
        buffersPanel.add(buffers, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, classes, buffersPanel);

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

    public DependenciesReader getDependenciesReader() {
        return dependenciesReader;
    }

    public void setDepsProvider(DependenciesReader depsReader) {
        this.dependenciesReader = depsReader;
    }

    private boolean isDecompiledBytecodeBufferVisible() {
        return buffers.getSelectedComponent().equals(bytecodeBuffer);
    }

    private boolean isBinaryBufferVisible() {
        return buffers.getSelectedComponent().equals(binary);
    }

    private boolean isAdditionalBinaryBufferVisible() {
        return buffers.getSelectedComponent().equals(additionalBinary);
    }

    private boolean isAdditionalDecompiledBytecodeBufferVisible() {
        return buffers.getSelectedComponent().equals(additionalBytecodeBuffer);
    }

    private boolean isAdditionalSrcBufferVisible() {
        return buffers.getSelectedComponent().equals(additionalSrcBuffer);
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
    public void reloadTextField(
            String name, String decompiledClass, byte[] source, String additionalDecompiledClass, byte[] additionalSource,
            String additionalSrcClass
    ) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BytecodeDecompilerView.this
                        .setDecompiledClass(name, decompiledClass, source, additionalDecompiledClass, additionalSource, additionalSrcClass);
            }
        });
    }

    private void setDecompiledClass(
            String name, String data, byte[] source, String additionalData, byte[] additionalSource, String additionalSrcData
    ) {
        additionalSrcBuffer.resetSrcArea(additionalSrcData);
        additionalBytecodeBuffer.resetSrcArea(additionalData);
        bytecodeBuffer.resetSrcArea(data);
        int buildJavaPerVersion = Lib.getBuildJavaPerVersion(source);
        Config.getConfig().setBestSourceTarget(Optional.of(buildJavaPerVersion));
        bytecodeButton.setText(buildJavaPerVersion + "");
        bytecodeButton.setToolTipText(
                styleTooltip() + "bytecode java version:" + buildJavaPerVersion + ". Click here to  copy it as source/target<br>" +
                        "force it is: " + Config.getConfig().doOverwriteST()
        );
        ActionListener[] ls = bytecodeButton.getActionListeners();
        for (ActionListener l : ls) {
            bytecodeButton.removeActionListener(l);
        }
        bytecodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                StringSelection selection =
                        new StringSelection(" -source " + buildJavaPerVersion + " -target " + buildJavaPerVersion + " ");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            }
        });
        binary.open(source);
        additionalBinary.open(additionalSource);

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
        bytecodeBuffer.setPopup(ap);
        additionalBytecodeBuffer.setPopup(ap);
        additionalSrcBuffer.setPopup(ap);
    }

    private class OverwriteActionListener implements ActionListener {

        private final DecompilationController.ClassOverwriter worker;

        OverwriteActionListener(DecompilationController.ClassOverwriter worker) {
            this.worker = worker;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (isBinaryBufferVisible() || isDecompiledBytecodeBufferVisible()) {
                worker.overwriteClass(
                        getSelectedDecompiler(), BytecodeDecompilerView.this.lastDecompiledClass,
                        BytecodeDecompilerView.this.bytecodeBuffer.getText(), BytecodeDecompilerView.this.binary.get(),
                        isBinaryBufferVisible()
                );
            } else if (isAdditionalBinaryBufferVisible() || isAdditionalDecompiledBytecodeBufferVisible()) {
                worker.overwriteClass(
                        getSelectedDecompiler(), BytecodeDecompilerView.this.lastDecompiledClass,
                        BytecodeDecompilerView.this.additionalBytecodeBuffer.getText(), BytecodeDecompilerView.this.additionalBinary.get(),
                        isAdditionalBinaryBufferVisible()
                );
            } else if (isAdditionalSrcBufferVisible()) {
                worker.overwriteClass(
                        getSelectedDecompiler(), BytecodeDecompilerView.this.lastDecompiledClass,
                        BytecodeDecompilerView.this.additionalSrcBuffer.getText(), new byte[0], isAdditionalBinaryBufferVisible()
                );
            } else {
                JOptionPane.showMessageDialog(
                        BytecodeDecompilerView.this.buffers,
                        "No tab selected? \n " + BytecodeDecompilerView.this.buffers.getSelectedComponent()
                );
            }

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
