package com.redhat.thermostat.vm.decompiler.swing.Templates;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class ConfigPanel extends JPanel {

    JButton okButton;
    JButton cancelButton;
    JPanel okCancelPanel;
    JPanel configureOKCancelPanel;
    GridBagConstraints gbc;

    public ConfigPanel(){

        okButton = new JButton("OK");
        // Action listener
        okButton.setPreferredSize(new Dimension(90,30));

        cancelButton = new JButton("Cancel");
        // Action listener
        cancelButton.setPreferredSize(new Dimension(90,30));

        okCancelPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridy = 0;
        gbc.weightx = 1;
        okCancelPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        okCancelPanel.add(okButton, gbc);
        gbc.gridx = 2;
        okCancelPanel.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 3;
        okCancelPanel.add(cancelButton, gbc);
        gbc.gridx = 4;
        okCancelPanel.add(Box.createHorizontalStrut(20), gbc);

        configureOKCancelPanel = new JPanel(new GridBagLayout());
        configureOKCancelPanel.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        configureOKCancelPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.gridx = 1;
        configureOKCancelPanel.add(okCancelPanel, gbc);
        configureOKCancelPanel.setPreferredSize(new Dimension(0,60));

        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        // addComponent inserts here
        gbc.gridy = 99999;
        gbc.weighty = 1;
        this.add(Box.createVerticalGlue(),gbc);
        gbc.gridy = 100000;
        gbc.weighty = 0;
        this.add(configureOKCancelPanel, gbc);
        gbc.gridy = 0;
    }

    public void addComponent(Component component){
        this.add(component, gbc);
        gbc.gridy++;
    }
}