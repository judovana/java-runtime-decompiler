package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import java.awt.*;

public class FileSelectorArrayAddRow extends JPanel {

    private final JButton addButton;
    private static final String PLUS_SIGN_ICON = "/icons/icons8-sum-24.png";

    FileSelectorArrayAddRow(){
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        addButton = new JButton();
        addButton.setPreferredSize(new Dimension(32, 32));
        addButton.setBorderPainted(false);
        ImageIcon icon = new ImageIcon(FileSelectorArrayRow.class.getResource(PLUS_SIGN_ICON));
        addButton.setIcon(icon);

        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(addButton, gbc);
        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(78), gbc);
    }

    public JButton getAddButton() {
        return addButton;
    }
}
