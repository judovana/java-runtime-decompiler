package org.jrd.frontend.frame.main;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Directories;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.VmInfo;
import org.jrd.frontend.frame.about.AboutView;
import org.jrd.frontend.frame.hex.StandaloneHex;
import org.jrd.frontend.frame.license.LicenseView;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.popup.JListPopupMenu;
import org.jrd.frontend.frame.main.renderer.VmListRenderer;
import org.jrd.frontend.frame.settings.SettingsView;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("Indentation") // indented Swing components greatly help with orientation
public class MainFrameView {

    public static final String FS_VM_COMMAND = "FS VM";
    public static final String REMOTE_VM_ACTION = "remote VM";
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel localVmPanel;
    private JPanel localVmLabelPanel;
    private JButton forceNewAttach;
    private JPanel localVmButtonPanel;
    private JButton localVmRefreshButton;
    private JScrollPane localVmScrollPane;
    private JList<VmInfo> localVmList;

    private JPanel remoteVmPanel;
    private JPanel remoteVmLabelPanel;
    private JPanel remoteVmButtonPanel;
    private JCheckBox remoteVmDetach;
    private JButton remoteVmAddButton;
    private JButton remoteVmRemoveButton;
    private JScrollPane remoteVmScrollPane;
    private JList<VmInfo> remoteVmList;

    private JPanel fsVmPanel;
    private JPanel fsVmLabelPanel;
    private JPanel fsVmButtonPanel;
    private JButton fsVmAddButton;
    private JButton fsVmRemoveButton;
    private JScrollPane fsVmScrollPane;
    private JList<VmInfo> fsVmList;

    private JPanel centerPanel;
    private JPanel welcomePanel;
    private JTextArea welcomeJTextArea;
    private BytecodeDecompilerView bytecodeDecompilerView;

    private CardLayout cardLayout;

    private JMenuBar menuBar;
    private JMenu jMenuConnect;
    private JMenuItem jMenuItemNewConnection;
    private JMenuItem openEditor;
    private JMenu jMenuConfig;
    private JMenuItem jMenuSettings;
    private JMenuItem jMenuPluginEditor;
    private JMenuItem jMenuOverrides;
    private JMenuItem jMenuAgents;
    private JMenu jMenuHelp;
    private JMenuItem jMenuItemAbout;
    private JMenuItem jMenuItemUsage;
    private JMenuItem jMenuItemLicense;
    private JMenuItem jMenuItemLog;

    private ActionListener vmChangingListener;
    private ActionListener refreshLocalVmsListener;
    private ActionListener newConnectionDialogListener;
    private ActionListener newFsVmDialogListener;
    private ActionListener removeVmDialogListener;
    private ActionListener pluginConfigurationEditorListener;
    private ActionListener haltAgentListener;
    private ActionListener killAllSession;
    private Runnable manageOverrides;
    private Runnable manageAgents;

    private static final Dimension BUTTON_SIZE = new Dimension(35, 35);
    private static final String WELCOME_CARD = "welcomePanel";
    private static final String DECOMPILER_CARD = "decompilerView";
    @SuppressWarnings("LineLength") // string formatting
    private static final String WELCOME_MESSAGE = "Welcome to Java-Runtime-Decompiler, or JRD for short.\n" + "\n" +
            "Before using JRD, the Decompiler Agent's path needs to be selected in 'Configure -> Settings'.\n" +
            "It's a built-in project and can usually be found at '" + Directories.getRelativePotentialAgentLocation() + "'.\n" +
            "On JDK 9 and higher, the agent is not allowed to attach by default.\n" +
            "You must run the target process with '-Djdk.attach.allowAttachSelf=true'.\n" + "\n" +
            "Internal javap disassembling tools are available by default.\n" +
            "You can also download external decompilers/disassemblers via 'mvn clean install -PdownloadPlugins'.\n" +
            "We currently support the following plugins: Fernflower, Procyon, Cfr, Jasm & Jcoder.\n" +
            "These can be easily setup with the 'Import' button in 'Configure -> Plugins', but nothing is stopping you from writing your own.\n" +
            "\n" +
            "JRD allows you to view loaded classes and their decompiled bytecode in either source code form or in a binary buffer.\n" +
            "JRD can be a dangerous program, as it allows you to overwrite classes in a running JVM, which has the potential to break the JVM.\n" +
            "Aside from local running JVMs, JRD can also interact with remote JMX processes and with JARs or class file trees on the filesystem.\n" +
            "\n" +
            "JRD is NOT an IDE. If you need to do a bigger changes, copypaste the code to IDE, and maybe let JRD to get more classes for you.\n" +
            "Then do your changes, and copypaste/upload  through JRD to the target VM\n" +
            "Decompilers are not perfect, JRD offers you to set up source paths(s) so you can edit and compile original code, not decompiled one\n" +
            "Local class path is here more over for completeness, and may be slowing down JRD. But sometimes there are interesting differences between your local build and runtime version\n";

    public JFrame getMainFrame() {
        return mainFrame;
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "I miss package and subpackage visibility. Highly connected classes")
    public BytecodeDecompilerView getBytecodeDecompilerView() {
        return bytecodeDecompilerView;
    }

    public void setHaltAgentListener(ActionListener listener) {
        haltAgentListener = listener;
    }

    public void setKillAllSessionListener(ActionListener listener) {
        killAllSession = listener;
    }

    public void setVmChanging(ActionListener listener) {
        vmChangingListener = listener;
    }

    /**
     * Custom JList that disables selection with mouse drag.
     */
    private static class UndraggableJList extends JList<VmInfo> {
        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (MouseEvent.MOUSE_DRAGGED != e.getID()) {
                super.processMouseMotionEvent(e);
            }
        }

        @Override
        public String toString() {
            return getName() + " - size " + getModel().getSize();
        }
    }

    private void setImageIcon() {
        final String iconPath = "/icons/main-icon.png";
        URL imageResource = getClass().getResource(iconPath);
        if (imageResource == null) {
            Logger.getLogger().log(Logger.Level.ALL, "Resource \"" + iconPath + "\" not found");
            return;
        }
        Image originalImage = new ImageIcon(imageResource).getImage();
        int originalHeight = originalImage.getHeight(null);
        int resolution = 32;
        if (originalHeight < resolution) {
            Logger.getLogger().log(Logger.Level.ALL, "Resource \"" + iconPath + "\" contains malformed data");
            return;
        }
        List<Image> images = new ArrayList<>();
        images.add(originalImage);
        for (; resolution < originalHeight; resolution *= 2) {
            images.add(originalImage.getScaledInstance(resolution, resolution, Image.SCALE_SMOOTH));
        }
        mainFrame.setIconImages(images);
    }

    public MainFrameView() {

        // mainFrame, mainPanel, westPanel, localVmPanel. localVmList, localVmScrollPane, localVmLabelPanel
        localVmList = new UndraggableJList();
        localVmList.setName("localVmList");
        localVmList.setFixedCellHeight(80);
        localVmList.setCellRenderer(new VmListRenderer());
        localVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //localVmList Listener
        localVmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    ActionEvent event = new ActionEvent(localVmList, 0, null);
                    vmChangingListener.actionPerformed(event);
                } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    new JListPopupMenu<>(localVmList, true, bytecodeDecompilerView.getDependenciesReader())
                            .addItem("name(s)", VmInfo::getVmName, true)
                            .addItem("PID(s)", vmInfo -> String.valueOf(vmInfo.getVmPid()), false)
                            .show(localVmList, mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });

        localVmRefreshButton = ImageButtonFactory.createRefreshButton("Refresh local VMs");
        localVmRefreshButton.addActionListener(actionEvent -> {
            refreshLocalVmsListener.actionPerformed(actionEvent);
        });
        // make text fit
        localVmRefreshButton.setMargin(new Insets(0, 0, 0, 0));

        localVmButtonPanel = new JPanel();
        localVmButtonPanel.add(localVmRefreshButton);

        localVmScrollPane = new JScrollPane(
                localVmList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        // localVmScrollPane End
        localVmLabelPanel = new JPanel(new BorderLayout());
        localVmLabelPanel.add(new JLabel("Local Processes", SwingConstants.CENTER), BorderLayout.CENTER);
        localVmLabelPanel.add(localVmButtonPanel, BorderLayout.EAST);
        // localVmLabelPanel End

        forceNewAttach = new JButton("Force new manual attach");
        forceNewAttach.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "By default, each agent is attached as" +
                        " permanent, single instance for safety and performance reasons.<br>" +
                        "If you need different PID connection you are on yor own." +
                        " You can later connect to such agent through remote vm, by its port"
        );
        forceNewAttach.addActionListener(a -> NewAgentDialog.show(localVmList));
        // newAgentPanel End
        localVmPanel = new JPanel(new BorderLayout());
        localVmPanel.setName("Local VMs");
        localVmPanel.add(localVmLabelPanel, BorderLayout.NORTH);
        localVmPanel.add(localVmScrollPane, BorderLayout.CENTER);
        localVmPanel.add(forceNewAttach, BorderLayout.SOUTH);
        // localVmPanel End

        // remoteVmPanel, remoteVmScrollPane, remoteVmLabelPanel, remoteConnectionButton
        remoteVmAddButton = ImageButtonFactory.createAddButton();
        remoteVmAddButton.addActionListener(actionEvent -> {
            newConnectionDialogListener.actionPerformed(actionEvent);
        });

        remoteVmRemoveButton = ImageButtonFactory.createRemoveButton();
        remoteVmRemoveButton.addActionListener(actionEvent -> {
            ActionEvent event = new ActionEvent(remoteVmList, 0, REMOTE_VM_ACTION, remoteVmDetach.isSelected() ? 1 : 0);
            removeVmDialogListener.actionPerformed(event);
        });

        remoteVmButtonPanel = new JPanel();
        remoteVmButtonPanel.add(remoteVmRemoveButton);
        remoteVmButtonPanel.add(remoteVmAddButton);

        // remoteConnectionButton End
        remoteVmLabelPanel = new JPanel(new BorderLayout());
        remoteVmDetach = new JCheckBox("Kill agent on removal", false);
        remoteVmLabelPanel.add(remoteVmDetach);
        remoteVmLabelPanel.add(remoteVmButtonPanel, BorderLayout.EAST);
        // remoteVmLabelPanel end
        remoteVmList = new UndraggableJList();
        remoteVmList.setName("remoteVmList");
        remoteVmList.setFixedCellHeight(80);
        remoteVmList.setCellRenderer(new VmListRenderer());
        remoteVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        remoteVmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    ActionEvent event = new ActionEvent(remoteVmList, 0, null);
                    vmChangingListener.actionPerformed(event);
                } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    new JListPopupMenu<>(remoteVmList, true, bytecodeDecompilerView.getDependenciesReader()).addItem(
                            "address(es)",
                            vmInfo -> vmInfo.getVmDecompilerStatus().getHostname() + ":" + vmInfo.getVmDecompilerStatus().getListenPort(),
                            true
                    ).show(remoteVmList, mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        // remoteVmList
        remoteVmScrollPane = new JScrollPane(
                remoteVmList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        // remoteVmScrollPane end

        remoteVmPanel = new JPanel(new BorderLayout());
        remoteVmPanel.setName("Remote VMs with attached agent");
        remoteVmPanel.add(remoteVmLabelPanel, BorderLayout.NORTH);
        remoteVmPanel.add(remoteVmScrollPane, BorderLayout.CENTER);
        // remoteVmPanel End

        fsVmList = new UndraggableJList();
        fsVmList.setName("localFsVmList");
        fsVmList.setFixedCellHeight(80);
        fsVmList.setCellRenderer(new VmListRenderer());
        fsVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fsVmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    ActionEvent event = new ActionEvent(fsVmList, 0, null);
                    vmChangingListener.actionPerformed(event);
                } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    new JListPopupMenu<>(fsVmList, true, bytecodeDecompilerView.getDependenciesReader())
                            .addItem("name(s)", VmInfo::getVmName, false).addItem("classpath(s)", VmInfo::getCpString, true)
                            .addItem("ID(s)", VmInfo::getVmId, false).show(fsVmList, mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        fsVmScrollPane =
                new JScrollPane(fsVmList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fsVmAddButton = ImageButtonFactory.createAddButton();
        fsVmAddButton.addActionListener(actionEvent -> newFsVmDialogListener.actionPerformed(actionEvent));

        fsVmRemoveButton = ImageButtonFactory.createRemoveButton();
        fsVmRemoveButton.addActionListener(actionEvent -> {
            ActionEvent event = new ActionEvent(fsVmList, 0, FS_VM_COMMAND);
            removeVmDialogListener.actionPerformed(event);
        });

        fsVmButtonPanel = new JPanel();
        fsVmButtonPanel.add(fsVmRemoveButton);
        fsVmButtonPanel.add(fsVmAddButton);

        fsVmLabelPanel = new JPanel(new BorderLayout());
        fsVmLabelPanel.add(fsVmButtonPanel, BorderLayout.EAST);
        fsVmLabelPanel.add(new JLabel("Local Filesystem Classpath Elements", SwingConstants.CENTER), BorderLayout.CENTER);
        fsVmPanel = new JPanel(new BorderLayout());
        fsVmPanel.setName("Local FS");
        fsVmPanel.add(fsVmLabelPanel, BorderLayout.NORTH);
        fsVmPanel.add(fsVmScrollPane, BorderLayout.CENTER);
        // localFsPanel End

        // copy preferred height size from a panel with buttons
        localVmLabelPanel.setPreferredSize(fsVmLabelPanel.getPreferredSize());

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EtchedBorder());
        tabbedPane.add(localVmPanel);
        tabbedPane.add(remoteVmPanel);
        tabbedPane.add(fsVmPanel);
        tabbedPane.setPreferredSize(new Dimension(400, 0));
        //westPanel End

        // centerPanel, welcomePanel
        welcomeJTextArea = new JTextArea((int) WELCOME_MESSAGE.chars().filter(ch -> ch == '\n').count(), 50);
        welcomeJTextArea.setText(WELCOME_MESSAGE);
        welcomeJTextArea.setFont(new Font(Font.SANS_SERIF, welcomeJTextArea.getFont().getStyle(), 20));
        welcomeJTextArea.setLineWrap(true);
        welcomeJTextArea.setWrapStyleWord(true);
        welcomeJTextArea.setEditable(false);
        welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBorder(BorderFactory.createLineBorder(welcomePanel.getBackground(), 50));
        welcomeJTextArea.setBackground(new Color(welcomePanel.getBackground().getRGB()));
        JScrollPane welcomeTextScroll = new JScrollPane(welcomeJTextArea);
        welcomeTextScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        welcomeTextScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        welcomePanel.add(welcomeTextScroll);
        // welcomePanel End

        mainFrame = new JFrame();
        setImageIcon();
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //TODO disconnect permanent agents
                //check fs vm overrides
                for (int i = 0; i < fsVmList.getModel().getSize(); i++) {
                    VmInfo vm = fsVmList.getModel().getElementAt(i);
                    if (DecompilationController.warnOnOvveridesOfFsVm(vm, mainFrame)) {
                        return;
                    }
                }
                mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                mainFrame.dispose();
            }
        });
        bytecodeDecompilerView = new BytecodeDecompilerView(mainFrame);

        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        centerPanel.add(welcomePanel, WELCOME_CARD);
        centerPanel.add(bytecodeDecompilerView.getBytecodeDecompilerPanel(), DECOMPILER_CARD);
        // centerPanel End

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        // mainPanel end

        //menuBar, jMenuConnect, jMenuConfig, jMenuHelp
        jMenuConnect = new JMenu("Connect");
        jMenuItemNewConnection = new JMenuItem("New Connection");
        jMenuItemNewConnection.addActionListener(actionEvent -> {
            newConnectionDialogListener.actionPerformed(actionEvent);
        });
        jMenuConnect.add(jMenuItemNewConnection);
        openEditor = new JMenuItem("Open Hex/Text notes");
        openEditor.addActionListener(a -> {
            StandaloneHex hexview = null;
            try {
                hexview = new StandaloneHex(new ArrayList<>(), true, getBytecodeDecompilerView().getCompletionHelper());
                hexview.setVisible(true);
            } catch (IOException ex) {
                Logger.getLogger().log(ex);
            }
        });
        jMenuConnect.add(openEditor);
        // jMenuConnect end

        jMenuConfig = new JMenu("Configure");
        jMenuSettings = new JMenuItem("Settings");
        jMenuSettings.addActionListener(actionEvent -> {
            new SettingsView(this.getMainFrame());
        });
        jMenuConfig.add(jMenuSettings);
        jMenuOverrides = new JMenuItem("Overrides");
        jMenuOverrides.addActionListener(actionEvent -> {
            manageOverrides.run();
        });
        jMenuConfig.add(jMenuOverrides);
        jMenuAgents = new JMenuItem("Agents");
        jMenuAgents.addActionListener(actionEvent -> {
            manageAgents.run();
        });
        jMenuConfig.add(jMenuAgents);
        jMenuPluginEditor = new JMenuItem("Plugins");
        jMenuPluginEditor.addActionListener(actionEvent -> {
            pluginConfigurationEditorListener.actionPerformed(actionEvent);
        });
        jMenuConfig.add(jMenuPluginEditor);
        // jMenuConfig end

        jMenuHelp = new JMenu("Help");
        jMenuItemAbout = new JMenuItem("About");
        jMenuItemAbout.addActionListener(actionEvent -> {
            new AboutView(this);
        });
        jMenuItemUsage = new JMenuItem("Usage");
        jMenuItemUsage.addActionListener(actionEvent -> {
            clearLocalListSelection();
            clearRemoteListSelection();
            switchPanel(false);
        });
        jMenuItemLicense = new JMenuItem("License");
        jMenuItemLicense.addActionListener(actionEvent -> {
            new LicenseView(this);
        });

        jMenuItemLog = new JMenuItem("Log");
        jMenuItemLog.addActionListener(actionEvent -> {
            GlobalConsole.getConsole().show();
        });
        jMenuHelp.add(jMenuItemAbout);
        jMenuHelp.add(jMenuItemUsage);
        jMenuHelp.add(jMenuItemLicense);
        jMenuHelp.add(jMenuItemLog);
        // jMenuHelp end

        menuBar = new JMenuBar();
        menuBar.add(jMenuConnect);
        menuBar.add(jMenuConfig);
        menuBar.add(jMenuHelp);
        // menuBar end

        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.setTitle(MetadataProperties.getInstance().getName());
        mainFrame.setMinimumSize(new Dimension(500, 340));
        mainFrame.setPreferredSize(new Dimension(900, 600));
        mainFrame.setSize(1400, 768);
        mainFrame.setLayout(new BorderLayout());
        ScreenFinder.centerWindowToCurrentScreen(mainFrame);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
        // mainFrame End

        // Tell server to shut down before exiting
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                killAllSession.actionPerformed(null);
            }
        });
    }

    public void clearLocalListSelection() {
        localVmList.clearSelection();
    }

    public void clearRemoteListSelection() {
        remoteVmList.clearSelection();
    }

    /**
     * Switches centerPanel between decompiler view and welcome view.
     *
     * @param isVmSelected True - Decompiler view
     *                     / False - Welcome view
     */
    public void switchPanel(boolean isVmSelected) {
        if (isVmSelected) {
            cardLayout.show(centerPanel, DECOMPILER_CARD);
        } else {
            cardLayout.show(centerPanel, WELCOME_CARD);
        }
    }

    private void sendHaltRequest() {
        ActionEvent event = new ActionEvent(this, 0, null);
        if (null != haltAgentListener) {
            haltAgentListener.actionPerformed(event);
        }
    }

    public void setRefreshLocalVmsListener(ActionListener listener) {
        this.refreshLocalVmsListener = listener;
    }

    public void setNewConnectionDialogListener(ActionListener listener) {
        this.newConnectionDialogListener = listener;
    }

    public void setNewFsVmDialogListener(ActionListener newFsVmDialogListener) {
        this.newFsVmDialogListener = newFsVmDialogListener;
    }

    public void setRemoveVmDialogListener(ActionListener removeVmDialogListener) {
        this.removeVmDialogListener = removeVmDialogListener;
    }

    public void setPluginConfigurationEditorListener(ActionListener pluginConfigurationEditorListener) {
        this.pluginConfigurationEditorListener = pluginConfigurationEditorListener;
    }

    public void setLocalVmList(VmInfo[] vmInfos) {
        setVmList(localVmList, vmInfos);
    }

    public void setRemoteVmList(VmInfo[] vmInfos) {
        setVmList(remoteVmList, vmInfos);
    }

    public void setFsVmList(VmInfo[] vmInfos) {
        setVmList(fsVmList, vmInfos);
    }

    public void setManageOverrides(Runnable action) {
        this.manageOverrides = action;
    }

    public void setManageAgents(Runnable action) {
        this.manageAgents = action;
    }

    private void setVmList(JList<VmInfo> vmList, VmInfo[] vmInfos) {
        VmInfo selectedValue = vmList.getSelectedValue();
        vmList.setListData(vmInfos);
        if (selectedValue != null) { // if !vmList.contains(selectedValue), e.g. after removal, selection gets cleared
            vmList.setSelectedValue(selectedValue, true);
        }
    }

    public void switchTabsToRemoteVms() {
        this.tabbedPane.setSelectedComponent(remoteVmPanel);
    }
}
