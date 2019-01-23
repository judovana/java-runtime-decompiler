package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Panel with two buttons "OK" and "Cancel"
 */
public class OkCancelPanel extends JPanel{

    private final JButton okButton;
    private final JButton cancelButton;

    OkCancelPanel(){
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(0, 50));
        setBorder(new MatteBorder(1,0,0,0, UIManager.getColor("Separator.shadow")));
        GridBagConstraints gbc;

        okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(90, 28));
        cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(90, 28));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        this.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(okButton, gbc);
        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 3;
        this.add(cancelButton, gbc);
        gbc.gridx = 4;
        this.add(Box.createHorizontalStrut(20), gbc);
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
