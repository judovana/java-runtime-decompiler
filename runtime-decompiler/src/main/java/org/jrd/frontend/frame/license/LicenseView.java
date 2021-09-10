package org.jrd.frontend.frame.license;

import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.MainFrameView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LicenseView extends JDialog {

    JTextArea licenseTextArea;
    JScrollPane scrollPane;

    public LicenseView(MainFrameView mainFrameView) {
        licenseTextArea = new JTextArea();
        licenseTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane = new JScrollPane(licenseTextArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

        InputStream in = getClass().getResourceAsStream("/LICENSE");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            reader.lines().forEach(s -> sb.append(s).append('\n'));
        } catch (IOException e) {
            sb.append("Unable to read LICENSE file.");
            Logger.getLogger().log(Logger.Level.ALL, sb.toString());
        }

        licenseTextArea.setText(sb.toString());
        licenseTextArea.setEditable(false);
        licenseTextArea.setCaretPosition(0);

        this.setTitle("License");
        this.setSize(new Dimension(600, 650));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setVisible(true);
    }

}
