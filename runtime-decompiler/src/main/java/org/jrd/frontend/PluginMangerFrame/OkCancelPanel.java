package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel with two buttons "OK" and "Cancel"
 */
public class OkCancelPanel extends JPanel{

    private JButton okButton;
    private JButton cancelButton;

    private ActionListener okActionListener;

    private ActionListener cancelActionListener;

    OkCancelPanel(){
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(0, 50));
        setBorder(new MatteBorder(1,0,0,0, SystemColor.menu));
        GridBagConstraints gbc;

        okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> this.okActionListener.actionPerformed(actionEvent));
        okButton.setPreferredSize(new Dimension(90, 30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> this.cancelActionListener.actionPerformed(actionEvent));
        cancelButton.setPreferredSize(new Dimension(90, 30));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridy = 0;
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

    public void setOkActionListener(ActionListener okActionListener) {
        this.okActionListener = okActionListener;
    }

    public void setCancelActionListener(ActionListener cancelActionListener) {
        this.cancelActionListener = cancelActionListener;
    }
}
