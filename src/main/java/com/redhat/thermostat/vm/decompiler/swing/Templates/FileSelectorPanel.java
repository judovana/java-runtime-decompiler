package com.redhat.thermostat.vm.decompiler.swing.Templates;

import javax.swing.*;
import java.awt.*;

public class FileSelectorPanel extends JPanel {

    public JTextField textField;
    public JLabel jLabel;
    public JButton browseButton;

    public FileSelectorPanel(String label){
        this(label, "Browse");
    }

    public FileSelectorPanel(String label, String ButtonLabel){

        this.textField = new JTextField();
        this.jLabel = new JLabel(label);
        this.browseButton = new JButton(ButtonLabel);

        browseButton.addActionListener(actionEvent -> {
            JFileChooser chooser = new JFileChooser();
            int returnVar = chooser.showOpenDialog(this);
            if (returnVar == JFileChooser.APPROVE_OPTION){
                textField.setText(chooser.getSelectedFile().getPath());
            }
        });

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        this.add(this.jLabel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        this.add(Box.createHorizontalStrut(20), gbc);
        gbc.weightx = 1;
        gbc.gridx = 1;
        this.add(textField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(20), gbc);
        gbc.gridx = 3;
        this.add(browseButton, gbc);
        gbc.gridx = 4;
        this.add(Box.createHorizontalStrut(20), gbc);
        this.setPreferredSize(new Dimension(0,80));
    }
}
