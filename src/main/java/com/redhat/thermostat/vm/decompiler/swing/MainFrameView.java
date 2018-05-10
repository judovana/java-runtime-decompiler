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
    private JLabel centerPanelLabel;

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


        // GUI END


        vmList.setListData(manager.getAllVm().toArray(new VmRef[0]));

        vmList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (vmList.getValueIsAdjusting()){
                    BytecodeDecompilerView processView = new BytecodeDecompilerView();
                    VmDecompilerInformationController controller = new VmDecompilerInformationController(processView, vmList.getSelectedValue(), manager);
                    mainPanel.add(processView.getBytecodeDecompilerPanel(), BorderLayout.CENTER);
                    mainFrame.revalidate();
                }
            }
        });

        mainFrame.setVisible(true);
    }

}
