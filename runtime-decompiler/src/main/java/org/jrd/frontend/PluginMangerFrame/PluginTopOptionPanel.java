package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import java.awt.*;

public class PluginTopOptionPanel extends JPanel {

    private JButton cloneButton;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton openWebsiteButton;

    private JLabel validStatusLabel;

    PluginTopOptionPanel(){
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(0,28));
        this.setBackground(SystemColor.menu);


        cloneButton = new JButton("Clone");
        cloneButton.setPreferredSize(new Dimension(84,28));
        refreshButton = new JButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(84,28));
        deleteButton = new JButton("Delete");
        deleteButton.setPreferredSize(new Dimension(84,28));
        openWebsiteButton = new JButton("Website");
        openWebsiteButton.setPreferredSize(new Dimension(84,28));

        validStatusLabel = new JLabel("Valid: False");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.gridx = 0;
        this.add(cloneButton, gbc);
        gbc.gridx = 1;
        this.add(refreshButton, gbc);
        gbc.gridx = 2;
        this.add(deleteButton, gbc);
        gbc.gridx = 3;
        this.add(openWebsiteButton, gbc);
        gbc.gridx = 4;
        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 5;
        this.add(validStatusLabel, gbc);
    }

}
