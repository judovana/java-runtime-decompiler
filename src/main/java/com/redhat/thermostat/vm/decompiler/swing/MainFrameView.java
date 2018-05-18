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
                        private JScrollPane remoteVmScrollPane;
                            private JList remoteVmList;
        private JPanel centerPanel;
            private JPanel welcomePanel;
                private JTextArea welcomeJTextArea;
            private BytecodeDecompilerView bytecodeDecompilerView;

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
        localVmLabelPanel.setBorder(new EtchedBorder());
        localVmLabelPanel.add(new JLabel("Local Processes", SwingConstants.CENTER));
        // localVmLabelPanel End


        localVmPanel = new JPanel(new BorderLayout());
        localVmPanel.add(localVmLabelPanel, BorderLayout.NORTH);
        localVmPanel.add(localVmScrollPane, BorderLayout.CENTER);
        // localVmPanel End

        // remoteVmPanel, remoteVmScrollPane, remoteVmLabelPanel
        remoteVmLabelPanel = new JPanel();
        remoteVmLabelPanel.setBorder(new EtchedBorder());
        remoteVmLabelPanel.add(new JLabel("Remote Processes", SwingConstants.CENTER));
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
                "To start click on one of the VMs on the left panel.\n" +
                "\n" +
                "Make sure you have set \"THERMOSTAT_DECOMPILER_AGENT_JAR\" environment variable\n" +
                "to absolute path of decompiler agent.");
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


        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setTitle("Runtime-Decompiler");
        mainFrame.setSize(1280,720);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);
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
                                    VmDecompilerInformationController controller = new VmDecompilerInformationController(bytecodeDecompilerView, localVmList.getSelectedValue(), manager);
                                    centerPanel.removeAll();
                                    centerPanel.add(bytecodeDecompilerView.getBytecodeDecompilerPanel(), BorderLayout.CENTER);
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
