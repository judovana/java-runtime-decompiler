package org.jrd.frontend.Templates;

import javax.swing.*;
import java.awt.*;

public class FileSelectorPanel extends JPanel {

    private JTextField textField;
    private JLabel jLabel;
    private JButton browseButton;

    FileSelectorPanel(String label) {
        this(label, "Browse");
    }

    FileSelectorPanel(String label, String ButtonLabel) {

        this.textField = new JTextField();
        textField.setPreferredSize(new Dimension(0, 32));
        this.jLabel = new JLabel(label);
        this.browseButton = new JButton(ButtonLabel);

        browseButton.addActionListener(actionEvent -> {
            JFileChooser chooser = new JFileChooser();
            int returnVar = chooser.showOpenDialog(this);
            if (returnVar == JFileChooser.APPROVE_OPTION) {
                textField.setText(chooser.getSelectedFile().getPath());
            }
        });

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.gridx = 0;
        this.add(this.jLabel, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 1;
        this.add(textField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(Box.createHorizontalStrut(20), gbc);
        gbc.gridx = 2;
        this.add(browseButton, gbc);
        this.setPreferredSize(new Dimension(0, 80));
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }
}
