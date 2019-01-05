package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PluginListPanel extends JPanel{

    private JList wrapperJList;
    private JButton addWrapperButton;

    PluginListPanel(){
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(180, 0));

        wrapperJList = new JList<>();
        wrapperJList.setFixedCellHeight(32);
        wrapperJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        wrapperJList.addListSelectionListener(listSelectionEvent -> {
//            if (wrapperJList.getValueIsAdjusting()) {
//                if (wrapperJList.getSelectedValue() != null)
//                    switchPluginListener.actionPerformed(new ActionEvent(this, 0, null));
//            }
//        });


        addWrapperButton = new JButton("New");
        addWrapperButton.setPreferredSize(new Dimension(0,28));
//        addWrapperButton.addActionListener(actionEvent -> {
//            addWrapperButtonListener.actionPerformed(actionEvent);
//        });

        this.add(wrapperJList, BorderLayout.CENTER);
        this.add(addWrapperButton, BorderLayout.NORTH);
    }

}
