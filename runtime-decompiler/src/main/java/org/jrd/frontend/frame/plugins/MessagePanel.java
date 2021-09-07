package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;

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
