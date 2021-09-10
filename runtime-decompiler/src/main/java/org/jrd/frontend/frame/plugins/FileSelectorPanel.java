package org.jrd.frontend.frame.plugins;

import org.jrd.backend.data.Directories;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileSelectorPanel extends JPanel {

    private JTextField textField;
    private JLabel jLabel;
    private JButton browseButton;
    private JFileChooser chooser;

    FileSelectorPanel(String label) {
        this(label, "Browse");
    }

    FileSelectorPanel(String label, String buttonLabel) {

        this.textField = new JTextField();
        textField.setPreferredSize(new Dimension(0, 32));
        textField.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                "Select a path to the decompiler wrapper .java file.<br />" +
                FileSelectorArrayRow.getTextFieldToolTip()
        );

        this.jLabel = new JLabel(label);
        this.browseButton = new JButton(buttonLabel);

        this.chooser = new JFileChooser();
        File dir = new File(Directories.getPluginDirectory());
        chooser.setCurrentDirectory(FileSelectorArrayRow.fallback(dir));

        browseButton.addActionListener(actionEvent -> {
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
