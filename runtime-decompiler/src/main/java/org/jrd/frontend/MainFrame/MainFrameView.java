package org.jrd.frontend.MainFrame;

import org.jrd.backend.data.VmInfo;
import org.jrd.frontend.ConfigureFrame.ConfigureView;
import org.jrd.frontend.LicenseFrame.LicenseView;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;

public class MainFrameView {

    private JFrame mainFrame;
    private JPanel mainPanel;
    private JPanel westPanel;
    private JPanel localVmPanel;
    private JScrollPane localVmScrollPane;
    private JPanel localVmLabelPanel;
    private JList<VmInfo> localVmList;
    private JPanel remoteVmPanel;
    private JPanel remoteVmLabelPanel;
    private JButton remoteConnectionButton;
    private JScrollPane remoteVmScrollPane;
    private JList<VmInfo> remoteVmList;
    private CardLayout cardLayout;
    private JPanel centerPanel;
    private JPanel welcomePanel;
    private JTextArea welcomeJTextArea;
    private BytecodeDecompilerView bytecodeDecompilerView;

    private JMenuBar menuBar;
    private JMenu jMenuConnect;
    private JMenuItem jMenuItemNewConnection;
    private JMenu jMenuConfig;
    private JMenuItem jMenuItemConfigure;
    private JMenuItem jMenuPluginEditor;
    private JMenu jMenuHelp;
    private JMenuItem jMenuItemAbout;
    private JMenuItem jMenuItemUsage;
    private JMenuItem jMenuItemLicense;

    private JDialog configureDialog;
    private JDialog licenseDialog;

    private ActionListener vmChangingListener;
    private ActionListener newConnectionDialogListener;
    private ActionListener pluginConfigurationEditorListener;

    private ActionListener haltAgentListener;

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

    public MainFrameView() {

        bytecodeDecompilerView = new BytecodeDecompilerView();

        /**
         * Custom JList that disables selection with mouse drag.
         */
        class UndragableJList extends JList {
            @Override
            protected void processMouseMotionEvent(MouseEvent e) {
                if (MouseEvent.MOUSE_DRAGGED != e.getID()) {
                    super.processMouseMotionEvent(e);
                }
            }
        }

        // mainFrame, mainPanel, westPanel, localVmPanel. localVmList, localVmScrollPane, localVmLabelPanel
        localVmList = new UndragableJList();
        localVmList.setName("localVmList");
        localVmList.setFixedCellHeight(80);
        localVmList.setCellRenderer(new VmListRenderer());
        localVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //localVmList Listener
        localVmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                ActionEvent event = new ActionEvent(localVmList, 0, null);
                vmChangingListener.actionPerformed(event);
            }
        });
        //localVmList End

        localVmScrollPane = new JScrollPane(localVmList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // localVmScrollPane End

        localVmLabelPanel = new JPanel();
        localVmLabelPanel.add(new JLabel("Local Processes", SwingConstants.CENTER));
        // localVmLabelPanel End


        localVmPanel = new JPanel(new BorderLayout());
        localVmPanel.add(localVmLabelPanel, BorderLayout.NORTH);
        localVmPanel.add(localVmScrollPane, BorderLayout.CENTER);
        // localVmPanel End

        // remoteVmPanel, remoteVmScrollPane, remoteVmLabelPanel, remoteConnectionButton
        remoteConnectionButton = new JButton("+");
        remoteConnectionButton.addActionListener(actionEvent -> {
            newConnectionDialogListener.actionPerformed(actionEvent);
        });
        remoteConnectionButton.setMargin(new Insets(5, 9, 5, 9));
        // remoteConnectionButton End

        remoteVmLabelPanel = new JPanel(new BorderLayout());
        remoteVmLabelPanel.add(new JLabel("    Remote Processes", SwingConstants.CENTER), BorderLayout.CENTER);
        remoteVmLabelPanel.add(remoteConnectionButton, BorderLayout.EAST);
        // remoteVmLabelPanel end

        remoteVmList = new UndragableJList();
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
        remoteVmPanel.add(remoteVmLabelPanel, BorderLayout.NORTH);
        remoteVmPanel.add(remoteVmScrollPane, BorderLayout.CENTER);
        // remoteVmPanel End

        westPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        westPanel.setBorder(new EtchedBorder());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        westPanel.add(localVmPanel, gbc);
        gbc.weighty = 0.75;
        gbc.gridy = 1;
        westPanel.add(remoteVmPanel, gbc);
        westPanel.setPreferredSize(new Dimension(400, 0));
        //westPanel End

        // centerPanel, welcomePanel
        welcomeJTextArea = new JTextArea(9, 40);
        welcomeJTextArea.setText("Welcome to Java-Runtime-Decompiler\n" +
                "\n" +
                "Before using the app, the Decompiler Agent's path needs to be selected in 'Config → Configure'.\n" +
                "It's a built-in project and can usually be found at './decompiler_agent/target/decompiler-agent-*.jar'.\n" +
                "\n" +
                "Internal javap decompiling tools are available by default.\n" +
                "You can also download an external decompiler, e.g. via 'mvn clean install -PdownloadPlugins', and set it up in 'Config → Plugin configuration'.\n" +
                "Currently supported decompilers are: Fernflower, Procyon.\n");
        welcomeJTextArea.setFont(new Font(welcomeJTextArea.getFont().getFontName(), welcomeJTextArea.getFont().getStyle(), 20));
        welcomeJTextArea.setLineWrap(true);
        welcomeJTextArea.setWrapStyleWord(true);
        welcomeJTextArea.setEditable(false);
        welcomePanel = new JPanel(new GridBagLayout());
        welcomeJTextArea.setBackground(new Color(welcomePanel.getBackground().getRGB()));
        welcomePanel.add(welcomeJTextArea);
        // welcomePanel End

        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        centerPanel.add(welcomePanel, "welcomePanel");
        centerPanel.add(bytecodeDecompilerView.getBytecodeDecompilerPanel(), "decompilerView");
        // centerPanel End


        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(westPanel, BorderLayout.WEST);
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

        jMenuConfig = new JMenu("Config");
        jMenuItemConfigure = new JMenuItem("Configure");
        jMenuItemConfigure.addActionListener(actionEvent -> {
            configureDialog = new ConfigureView(this);
        });
        jMenuConfig.add(jMenuItemConfigure);
        jMenuPluginEditor = new JMenuItem("Plugin configuration");
        jMenuPluginEditor.addActionListener(actionEvent -> {
            pluginConfigurationEditorListener.actionPerformed(actionEvent);
        });
        jMenuConfig.add(jMenuPluginEditor);
        // jMenuConfig end

        jMenuHelp = new JMenu("Help");
        jMenuItemAbout = new JMenuItem("About");
        jMenuItemUsage = new JMenuItem("Usage");
        jMenuItemLicense = new JMenuItem("License");
        jMenuItemLicense.addActionListener(actionEvent -> {
            licenseDialog = new LicenseView(this);
        });
        jMenuHelp.add(jMenuItemAbout);
        jMenuHelp.add(jMenuItemUsage);
        jMenuHelp.add(jMenuItemLicense);
        // jMenuHelp end

        menuBar = new JMenuBar();
        menuBar.add(jMenuConnect);
        menuBar.add(jMenuConfig);
        menuBar.add(jMenuHelp);
        // menuBar end


        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setTitle("Runtime-Decompiler");
        mainFrame.setSize(1280, 720);
        mainFrame.setMinimumSize(new Dimension(700, 340));
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
        // mainFrame End


        // Tell server to shutdown before exiting
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
            cardLayout.show(centerPanel, "decompilerView");
        } else {
            cardLayout.show(centerPanel, "welcomePanel");
        }
    }

    private void sendHaltRequest() {
        ActionEvent event = new ActionEvent(this, 0, null);
        if (null != haltAgentListener) {
            haltAgentListener.actionPerformed(event);
        } else {
            System.exit(0);
        }

    }

    void setCreateNewConnectionDialogListener(ActionListener listener) {
        this.newConnectionDialogListener = listener;
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

    private void setVmList(JList<VmInfo> vmList, VmInfo[] vmInfos) {
        VmInfo selectedValue = vmList.getSelectedValue();
        vmList.setListData(vmInfos);
        if (selectedValue != null) {
            vmList.setSelectedValue(selectedValue, true);
        }
    }
}
