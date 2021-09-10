package org.jrd.frontend.frame.plugins;

import org.jrd.backend.core.Logger;

import javax.swing.*;
import java.awt.*;

public class FileSelectorArrayAddRow extends JPanel {

    private JButton addButton;
    private static final String PLUS_SIGN_ICON = "/icons/icons8-sum-24.png";

    FileSelectorArrayAddRow() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        try {
            ImageIcon icon = new ImageIcon(FileSelectorArrayRow.class.getResource(PLUS_SIGN_ICON));
            addButton = new JButton(icon);
        } catch (NullPointerException e) {
            addButton = new JButton("+");
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("File " + PLUS_SIGN_ICON + " not found. Falling back to String version.", e));
        }
        addButton.setPreferredSize(new Dimension(32, 32));
        addButton.setBorderPainted(false);

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
