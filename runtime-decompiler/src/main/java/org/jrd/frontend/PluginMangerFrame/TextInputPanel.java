package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import java.awt.*;

public class TextInputPanel extends JPanel {

    public JTextField textField;
    public JLabel jLabel;

    /**
     * GridbagLayout JPanel with label and textField.
     *
     * @param label
     */
    TextInputPanel(String label) {

        this.textField = new JTextField();
        textField.setPreferredSize(new Dimension(0, 32));
        this.jLabel = new JLabel(label);

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.gridx = 0;
        this.add(jLabel, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 1;
        this.add(textField, gbc);

        this.setPreferredSize(new Dimension(0, 80));
    }

    public String getText() {
        return textField.getText();
    }
}
