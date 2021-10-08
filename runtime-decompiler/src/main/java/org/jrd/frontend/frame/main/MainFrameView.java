package org.jrd.frontend.frame.main;

import org.jrd.backend.data.Directories;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.VmInfo;
import org.jrd.frontend.frame.about.AboutView;
import org.jrd.frontend.frame.license.LicenseView;
import org.jrd.frontend.frame.settings.SettingsView;
import org.jrd.frontend.utility.ImageButtonFactory;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("Indentation") // indented Swing components greatly help with orientation
public class MainFrameView {

    public static final String FS_VM_COMMAND = "FS VM";
    public static final String REMOTE_VM_ACTION = "remote VM";
    private JFrame mainFrame;
        private JPanel mainPanel;
            private JTabbedPane tabbedPane;
                private JPanel localVmPanel;
                    private JPanel localVmLabelPanel;
                        private JPanel localVmButtonPanel;
                            private JButton localVmRefreshButton;
                    private JScrollPane localVmScrollPane;
                        private JList<VmInfo> localVmList;

                private JPanel remoteVmPanel;
                    private JPanel remoteVmLabelPanel;
                        private JPanel remoteVmButtonPanel;
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
        private JMenu jMenuConfig;
            private JMenuItem jMenuSettings;
            private JMenuItem jMenuPluginEditor;
            private JMenuItem jMenuOverrides;
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
    private Runnable manageOverrides;

    private static final Dimension BUTTON_SIZE = new Dimension(35, 35);
    private static final String WELCOME_CARD = "welcomePanel";
    private static final String DECOMPILER_CARD = "decompilerView";
    @SuppressWarnings("LineLength") // string formatting
    private static final String WELCOME_MESSAGE =
            "Welcome to Java-Runtime-Decompiler, or JRD for short.\n" +
            "\n" +
            "Before using JRD, the Decompiler Agent's path needs to be selected in 'Configure -> Settings'.\n" +
            "It's a built-in project and can usually be found at '" + Directories.getPotentialAgentLocation(false) + "'.\n" +
            "On JDK 9 and higher, the agent is not allowed to attach by default.\n" +
            "You must run the target process with '-Djdk.attach.allowAttachSelf=true'.\n" +
            "\n" +
            "Internal javap disassembling tools are available by default.\n" +
            "You can also download external decompilers/disassemblers via 'mvn clean install -PdownloadPlugins'.\n" +
            "We currently support the following plugins: Fernflower, Procyon, Cfr, Jasm & Jcoder.\n" +
            "These can be easily setup with the 'Import' button in 'Configure -> Plugins', but nothing is stopping you from writing your own.\n" +
            "\n" +
            "JRD allows you to view loaded classes and their decompiled bytecode in either source code form or in a binary buffer.\n" +
            "JRD can be a dangerous program, as it allows you to overwrite classes in a running JVM, which has the potential to break the JVM.\n" +
            "Aside from local running JVMs, JRD can also interact with remote JMX processes and with JARs or class file trees on the filesystem.\n";

    public JFrame getMainFrame() {
        return mainFrame;
    }

    BytecodeDecompilerView getBytecodeDecompilerView() {
        return bytecodeDecompilerView;
    }

    void setHaltAgentListener(ActionListener listener) {
        haltAgentListener = listener;
    }

    void setVmChanging(ActionListener listener) {
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
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    ActionEvent event = new ActionEvent(localVmList, 0, null);
                    vmChangingListener.actionPerformed(event);
                }
            }
        });
        //localVmList End

        localVmRefreshButton = ImageButtonFactory.createRefreshButton("Refresh local VMs");
        localVmRefreshButton.addActionListener(actionEvent -> {
            refreshLocalVmsListener.actionPerformed(actionEvent);
        });
        // make text fit
        localVmRefreshButton.setMargin(new Insets(0, 0, 0, 0));

        localVmButtonPanel = new JPanel();
        localVmButtonPanel.add(localVmRefreshButton);

        localVmScrollPane = new JScrollPane(localVmList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // localVmScrollPane End
        localVmLabelPanel = new JPanel(new BorderLayout());
        localVmLabelPanel.add(new JLabel("Local Processes", SwingConstants.CENTER), BorderLayout.CENTER);
        localVmLabelPanel.add(localVmButtonPanel, BorderLayout.EAST);

        // localVmLabelPanel End
        localVmPanel = new JPanel(new BorderLayout());
        localVmPanel.setName("Local VMs");
        localVmPanel.add(localVmLabelPanel, BorderLayout.NORTH);
        localVmPanel.add(localVmScrollPane, BorderLayout.CENTER);
        // localVmPanel End

        // remoteVmPanel, remoteVmScrollPane, remoteVmLabelPanel, remoteConnectionButton
        remoteVmAddButton = ImageButtonFactory.createAddButton();
        remoteVmAddButton.addActionListener(actionEvent -> {
            newConnectionDialogListener.actionPerformed(actionEvent);
        });

        remoteVmRemoveButton = ImageButtonFactory.createRemoveButton();
        remoteVmRemoveButton.addActionListener(actionEvent -> {
            ActionEvent event = new ActionEvent(remoteVmList, 0, REMOTE_VM_ACTION);
            removeVmDialogListener.actionPerformed(event);
        });

        remoteVmButtonPanel = new JPanel();
        remoteVmButtonPanel.add(remoteVmRemoveButton);
        remoteVmButtonPanel.add(remoteVmAddButton);

        // remoteConnectionButton End
        remoteVmLabelPanel = new JPanel(new BorderLayout());
        remoteVmLabelPanel.add(new JLabel("Remote Processes", SwingConstants.CENTER), BorderLayout.CENTER);
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
                ActionEvent event = new ActionEvent(remoteVmList, 0, null);
                vmChangingListener.actionPerformed(event);
            }
        });
        // remoteVmList
        remoteVmScrollPane = new JScrollPane(remoteVmList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // remoteVmScrollPane end

        remoteVmPanel = new JPanel(new BorderLayout());
        remoteVmPanel.setName("Remote VMs");
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
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    ActionEvent event = new ActionEvent(fsVmList, 0, null);
                    vmChangingListener.actionPerformed(event);
                }
            }
        });
        fsVmScrollPane = new JScrollPane(fsVmList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

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
        fsVmLabelPanel.add(
                new JLabel("Local Filesystem Classpath Elements", SwingConstants.CENTER),
                BorderLayout.CENTER
        );
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
        welcomePanel = new JPanel(new GridBagLayout());
        welcomeJTextArea.setBackground(new Color(welcomePanel.getBackground().getRGB()));
        welcomePanel.add(welcomeJTextArea);
        // welcomePanel End

        mainFrame = new JFrame();
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //todo disconect pernament agents
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
        // jMenuConnect end

        jMenuConfig = new JMenu("Configure");
        jMenuSettings = new JMenuItem("Settings");
        jMenuSettings.addActionListener(actionEvent -> {
            new SettingsView(this);
        });
        jMenuConfig.add(jMenuSettings);
        jMenuOverrides = new JMenuItem("Overrides");
        jMenuOverrides.addActionListener(actionEvent -> {
            manageOverrides.run();
        });
        jMenuConfig.add(jMenuOverrides);
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
        mainFrame.setSize(1366, 768);
        mainFrame.setMinimumSize(new Dimension(700, 340));
        mainFrame.setLayout(new BorderLayout());
        ScreenFinder.centerWindowsToCurrentScreen(mainFrame);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
        // mainFrame End


        // Tell server to shut down before exiting
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendHaltRequest();
            }
        });
    }

    void clearLocalListSelection() {
        localVmList.clearSelection();
    }

    void clearRemoteListSelection() {
        remoteVmList.clearSelection();
    }

    /**
     * Switches centerPanel between decompiler view and welcome view.
     *
     * @param isVmSelected True - Decompiler view
     *                     / False - Welcome view
     */
    void switchPanel(boolean isVmSelected) {
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

    void setNewConnectionDialogListener(ActionListener listener) {
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

    void setLocalVmList(VmInfo[] vmInfos) {
        setVmList(localVmList, vmInfos);
    }

    void setRemoteVmList(VmInfo[] vmInfos) {
        setVmList(remoteVmList, vmInfos);
    }

    void setFsVmList(VmInfo[] vmInfos) {
        setVmList(fsVmList, vmInfos);
    }

    void setManageOverrides(Runnable action) {
        this.manageOverrides = action;
    }

    private void setVmList(JList<VmInfo> vmList, VmInfo[] vmInfos) {
        VmInfo selectedValue = vmList.getSelectedValue();
        vmList.setListData(vmInfos);
        if (selectedValue != null) { // if !vmList.contains(selectedValue), e.g. after removal, selection gets cleared
            vmList.setSelectedValue(selectedValue, true);
        }
    }

    protected void switchTabsToRemoteVms() {
        this.tabbedPane.setSelectedComponent(remoteVmPanel);
    }
}
