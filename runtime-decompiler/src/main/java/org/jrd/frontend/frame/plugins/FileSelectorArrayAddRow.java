package org.jrd.frontend.frame.plugins;

import org.jrd.frontend.utility.ImageButtonFactory;

import javax.swing.*;
import java.awt.*;

public class FileSelectorArrayAddRow extends JPanel {

    private JButton addButton;

    FileSelectorArrayAddRow(int rightMargin) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        addButton = ImageButtonFactory.createAddButton();

        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);

        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(addButton, gbc);

        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(rightMargin), gbc);
    }

    public JButton getAddButton() {
        return addButton;
    }
}
