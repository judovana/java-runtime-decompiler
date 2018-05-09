/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmManager;

import java.awt.*;
import java.util.ArrayList;
import workers.VmRef;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author pmikova
 */
public class MainFrameView {


    private JFrame mainFrame;
    private JPanel mainPanel;
    private JPanel eastPanel;
    private JList<VmRef> vmList;

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

//        String[] vmListDefault = {"VMs haven't been", "initialized yet", "please wait..."};
        vmList = new JList<VmRef>();
        vmList.setFixedCellHeight(20);
        vmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(vmList, BorderLayout.CENTER);
        eastPanel.setBorder(new EtchedBorder());
        eastPanel.setPreferredSize(new Dimension(400,720));

        mainPanel.add(eastPanel, BorderLayout.WEST);


        mainFrame.setVisible(true);

        //end

        vmList.setListData(manager.getAllVm().toArray(new VmRef[0]));
        vmList.setSelectedIndex(0);
        BytecodeDecompilerView leftView = new BytecodeDecompilerView();
        VmDecompilerInformationController controller = new VmDecompilerInformationController(leftView, vmList.getSelectedValue(), manager);

        vmList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (vmList.getValueIsAdjusting()){

                }
            }
        });

        mainPanel.add(leftView.getGuiMainFrame(), BorderLayout.CENTER);

        // listener na kazdy item listu Vmek, po kliknuti identifikuje vm a:
//        BytecodeDecompilerView leftView = new BytecodeDecompilerView();
//        VmDecompilerInformationController controller  = new VmDecompilerInformationController(leftView, ref, manager);
                // tohle uz by melo vytvorit teoreticky i ten view a musi se zajistit, ze se ten view zobrazi az pak (fakt netusim jak se to dela)

        //
    
    }
    
}
