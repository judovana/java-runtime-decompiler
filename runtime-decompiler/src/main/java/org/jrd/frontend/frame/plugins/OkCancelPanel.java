package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Panel with three buttons "Validate", "OK" and "Cancel"
 */
public class OkCancelPanel extends JPanel {

    private final JButton okButton;
    private final JButton cancelButton;
    private final JButton validateButton;

    OkCancelPanel() {
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(0, 50));
        setBorder(new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.shadow")));

        okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(90, 28));
        cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(90, 28));
        validateButton = new JButton("Validate");
        validateButton.setPreferredSize(new Dimension(90, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(validateButton, gbc);
        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 3;
        this.add(okButton, gbc);
        gbc.gridx = 4;
        this.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 5;
        this.add(cancelButton, gbc);
        gbc.gridx = 6;
        this.add(Box.createHorizontalStrut(20), gbc);
    }

    public JButton getValidateButton() {
        return validateButton;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
