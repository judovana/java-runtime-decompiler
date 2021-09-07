package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;

public class TextInputPanel extends JPanel {

    private final JTextField textField;
    private final JLabel jLabel;

    TextInputPanel(String label) {
        this();
        jLabel.setText(label);
    }

    TextInputPanel() {
        this.setPreferredSize(new Dimension(0, 80));
        this.setLayout(new GridBagLayout());

        textField = new JTextField();
        textField.setPreferredSize(new Dimension(0, 32));
        jLabel = new JLabel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(jLabel, gbc);
        gbc.gridy = 1;
        gbc.weightx = 1;
        this.add(textField, gbc);
    }

    public JTextField getTextField() {
        return textField;
    }
}
