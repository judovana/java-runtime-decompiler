package com.redhat.thermostat.vm.decompiler.swing.Templates;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class FileSelectorArrayPanel extends JPanel {

    public JList jlist;
    public JLabel jLabel;
    GridBagConstraints gbc;


    public FileSelectorArrayPanel(String label){

        this.jlist = new JList();
        this.jLabel = new JLabel(label);
        this.setBackground(Color.RED);

        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(Box.createHorizontalStrut(20));

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        this.add(jLabel, gbc);

        gbc.gridx  = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
        gbc.weighty = 1;
        this.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
        this.setPreferredSize(new Dimension(0,80));
    }

    public void addUrl(URL url){
        gbc.weightx = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        this.add(Box.createHorizontalStrut(20), gbc);
        gbc.gridx = 1;
        this.add(new JButton(), gbc);
    }
}