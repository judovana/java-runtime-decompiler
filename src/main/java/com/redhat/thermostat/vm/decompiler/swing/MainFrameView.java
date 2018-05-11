/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmManager;

import java.awt.*;

import workers.VmRef;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author rmanak,pmikova
 */
public class MainFrameView {


    private JFrame mainFrame;

    private JPanel mainPanel;

    private JPanel eastPanel;
    private JScrollPane vmlistScrollPane;
    private JList<VmRef> vmList;

    private JPanel processlistLabelPanel;
    private JPanel centerPanel;
    private JTextArea welcomeJTextArea;
    private JPanel welcomePanel;

    private BytecodeDecompilerView bytecodeDecompilerView;

    public MainFrameView(VmManager manager){


        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setTitle("Runtime-Decompiler");
        mainFrame.setSize(1280,720);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainPanel.setLayout(new BorderLayout());

        vmList = new JList<VmRef>();
        vmList.setFixedCellHeight(20);
        vmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vmlistScrollPane = new JScrollPane(vmList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        processlistLabelPanel = new JPanel(new GridBagLayout());
        processlistLabelPanel.add(new JLabel("Process List"));
        eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(processlistLabelPanel, BorderLayout.NORTH);
        eastPanel.add(vmlistScrollPane, BorderLayout.CENTER);
        eastPanel.setBorder(new EtchedBorder());
        eastPanel.setPreferredSize(new Dimension(400,720));
        mainPanel.add(eastPanel, BorderLayout.WEST);

        centerPanel = new JPanel(new BorderLayout());
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        welcomePanel = new JPanel(new GridBagLayout());
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
        welcomeJTextArea.setBackground(welcomePanel.getBackground());
        welcomePanel.add(welcomeJTextArea);
        centerPanel.add(welcomePanel, BorderLayout.CENTER);



        // GUI END


        vmList.setListData(manager.getAllVm().toArray(new VmRef[0]));

        vmList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (vmList.getValueIsAdjusting()){
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                if (vmList.getSelectedValue() == null){
                                    centerPanel.removeAll();
                                    centerPanel.add(welcomePanel, BorderLayout.CENTER);
                                    mainFrame.repaint();
                                } else {
                                    BytecodeDecompilerView processView = new BytecodeDecompilerView();
                                    VmDecompilerInformationController controller = new VmDecompilerInformationController(processView, vmList.getSelectedValue(), manager);
                                    centerPanel.removeAll();
                                    centerPanel.add(processView.getBytecodeDecompilerPanel(), BorderLayout.CENTER);
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

        mainFrame.setVisible(true);
    }

}
