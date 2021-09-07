package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginTopOptionPanel extends JPanel {

    private final JButton cloneButton;
    private final JButton deleteButton;
    private final JButton openWebsiteButton;
    private final JButton importButton;

    public PluginTopOptionPanel() {
        final int height = 28;
        final int buttonWidth = 84;
        Dimension buttonSize = new Dimension(buttonWidth, height);

        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(0, height));

        cloneButton = new JButton("Clone");
        cloneButton.setPreferredSize(buttonSize);
        deleteButton = new JButton("Delete");
        deleteButton.setPreferredSize(buttonSize);
        openWebsiteButton = new JButton("Website");
        openWebsiteButton.setPreferredSize(buttonSize);
        importButton = new JButton("Import");
        importButton.setPreferredSize(buttonSize);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.gridx = 0;
        this.add(cloneButton, gbc);
        gbc.gridx = 1;
        this.add(deleteButton, gbc);
        gbc.gridx = 2;
        this.add(openWebsiteButton, gbc);
        gbc.gridx = 3;
        this.add(importButton, gbc);
        gbc.gridx = 4;
        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
    }

    public JButton getCloneButton() {
        return cloneButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JButton getOpenWebsiteButton() {
        return openWebsiteButton;
    }

    public JButton getImportButton() {
        return importButton;
    }
}
