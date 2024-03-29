package org.jrd.frontend.frame.plugins;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class MessagePanel extends JPanel {

    private final JLabel jLabel;

    MessagePanel(String message) {
        this.setLayout(new BorderLayout());
        this.setBackground(Color.decode("#14b7e0"));
        this.setPreferredSize(new Dimension(0, 40));

        jLabel = new JLabel();
        jLabel.setText(message);
        this.add(jLabel);
    }
}
