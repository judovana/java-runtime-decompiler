/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmInfo;
import com.redhat.thermostat.vm.decompiler.data.VmManager;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
                        private JList remoteVmList;
        private JPanel centerPanel;
            private JPanel welcomePanel;
                private JTextArea welcomeJTextArea;
            private BytecodeDecompilerView bytecodeDecompilerView;

    private JMenuBar menuBar;
    private JMenu jMenuConnect;
    private JMenuItem jMenuItemNewConnection;
    private JMenu jMenuConfig;
    private JMenuItem jMenuItemConfigure;
    private JMenu jMenuHelp;
    private JMenuItem jMenuItemAbout;
    private JMenuItem jMenuItemUsage;
    private JMenuItem jMenuItemLicense;

    private JDialog configureFrame;

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public MainFrameView(VmManager manager){

        // mainFrame, mainPanel, westPanel, localVmPanel. localVmList, localVmScrollPane, localVmLabelPanel
        localVmList = new JList<VmInfo>();
        localVmList.setFixedCellHeight(80);
        localVmList.setCellRenderer(new VmListRenderer());
        localVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        remoteConnectionButton.setMargin( new Insets(5, 9, 5, 9) );
        // remoteConnectionButton End

        remoteVmLabelPanel = new JPanel(new BorderLayout());
        remoteVmLabelPanel.add(new JLabel("    Remote Processes", SwingConstants.CENTER), BorderLayout.CENTER);
        remoteVmLabelPanel.add(remoteConnectionButton, BorderLayout.EAST);
        // remoteVmLabelPanel end

        remoteVmList = new JList<VmInfo>();
        remoteVmList.setFixedCellHeight(80);
        remoteVmList.setCellRenderer(new VmListRenderer());
        remoteVmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        westPanel.setPreferredSize(new Dimension(400,0));
        //westPanel End

        // centerPanel, welcomePanel
        welcomeJTextArea = new JTextArea(20,40);
        welcomeJTextArea.setText("Welcome to Java-Runtime-Decompiler\n" +
                "\n" +
                "To start click on one of the VMs on the left panel.");
        welcomeJTextArea.setFont(new Font(welcomeJTextArea.getFont().getFontName(), welcomeJTextArea.getFont().getStyle(), 20));
        welcomeJTextArea.setLineWrap(true);
        welcomeJTextArea.setWrapStyleWord(true);
        welcomeJTextArea.setEditable(false);
        welcomePanel = new JPanel(new GridBagLayout());
        welcomeJTextArea.setBackground(welcomePanel.getBackground());
        welcomePanel.add(welcomeJTextArea);
        // welcomePanel End

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(welcomePanel, BorderLayout.CENTER);
        // centerPanel End


        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(westPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        // mainPanel end

        //menuBar, jMenuConnect, jMenuConfig, jMenuHelp
        jMenuConnect = new JMenu("Connect");
        jMenuItemNewConnection = new JMenuItem("New Connection");
        jMenuConnect.add(jMenuItemNewConnection);
        // jMenuConnect end

        jMenuConfig = new JMenu("Config");
        jMenuItemConfigure = new JMenuItem("Configure");
        jMenuItemConfigure.addActionListener(actionEvent -> {
            configureFrame = new ConfigureView(this);
        });
        jMenuConfig.add(jMenuItemConfigure);
        // jMenuConfig end

        jMenuHelp = new JMenu("Help");
        jMenuItemAbout = new JMenuItem("About");
        jMenuItemUsage = new JMenuItem("Usage");
        jMenuItemLicense = new JMenuItem("License");
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
        mainFrame.setSize(1280,720);
        mainFrame.setMinimumSize(new Dimension(700,340));
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
        // mainFrame End

        //Fill list with vms
        localVmList.setListData(manager.getAllVm().toArray(new VmInfo[0]));

        //localVmList Listener
        localVmList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (localVmList.getValueIsAdjusting()){
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                if (bytecodeDecompilerView != null){
                                    bytecodeDecompilerView.haltServer();
                                }
                                if (localVmList.getSelectedValue() == null){
                                    centerPanel.removeAll();
                                    centerPanel.add(welcomePanel, BorderLayout.CENTER);
                                    mainFrame.repaint();
                                } else {
                                    bytecodeDecompilerView = new BytecodeDecompilerView();
                                    centerPanel.removeAll();
                                    mainFrame.revalidate();
                                    mainFrame.repaint();
                                    centerPanel.add(bytecodeDecompilerView.getBytecodeDecompilerPanel(), BorderLayout.CENTER);
                                    VmDecompilerInformationController controller = new VmDecompilerInformationController(bytecodeDecompilerView, localVmList.getSelectedValue(), manager);
                                    mainFrame.revalidate();
                                }
                            } catch (Throwable t) {
                                // log exception
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        });

        // Tell server to shutdown before exiting
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (bytecodeDecompilerView != null){
                    bytecodeDecompilerView.haltServer();
                }
            }
        });
    }
}
