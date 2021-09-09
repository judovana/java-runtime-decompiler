package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;

public class ConfigPanel extends JPanel {

    private final JLabel jsonFileUrl;
    private final MessagePanel messagePanel;
    private final TextInputPanel namePanel;
    private final FileSelectorPanel wrapperUrlPanel;
    private final FileSelectorArrayPanel dependencyUrlPanel;

    public ConfigPanel() {
        this.setLayout(new GridBagLayout());

        jsonFileUrl = new JLabel();
        messagePanel = new MessagePanel("<html><b>Info:</b> You don't have permissions to save this configuration! " +
                "You can clone it and save the copy.</html>");
        messagePanel.setVisible(false);
        namePanel = new TextInputPanel("Name");
        wrapperUrlPanel = new FileSelectorPanel("Decompiler wrapper");
        dependencyUrlPanel = new FileSelectorArrayPanel("Decompiler dependency jars");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1;
        this.add(jsonFileUrl, gbc);
        gbc.gridy = 1;
        this.add(messagePanel, gbc);
        gbc.gridy = 2;
        this.add(namePanel, gbc);
        gbc.gridy = 3;
        this.add(wrapperUrlPanel, gbc);
        gbc.gridy = 4;
        this.add(dependencyUrlPanel, gbc);
        gbc.gridy = 99999;
        gbc.weighty = 1;
        this.add(Box.createVerticalGlue(), gbc);
        //sets gbc for addComponent method
        gbc.gridy = 0;
        gbc.weighty = 0;
    }

    public JLabel getJsonFileUrl() {
        return jsonFileUrl;
    }

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public TextInputPanel getNamePanel() {
        return namePanel;
    }

    public FileSelectorPanel getWrapperUrlPanel() {
        return wrapperUrlPanel;
    }

    public FileSelectorArrayPanel getDependencyUrlPanel() {
        return dependencyUrlPanel;
    }
}
