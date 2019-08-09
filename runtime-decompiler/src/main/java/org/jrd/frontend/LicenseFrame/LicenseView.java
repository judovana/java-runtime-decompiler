package org.jrd.frontend.LicenseFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class LicenseView extends JDialog {

    JEditorPane licensePane;
    JScrollPane scrollPane;

    public LicenseView(MainFrameView mainFrameView){
        licensePane = new JEditorPane();
        licensePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane = new JScrollPane(licensePane);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

        URL url = getClass().getResource("/LICENSE");
        try {
            licensePane.setPage(url);
        } catch (IOException e) {
            licensePane.setContentType("text/html");
            licensePane.setText("<html>License text not found.</html>");
        }

        licensePane.setEditable(false);
        licensePane.setCaretPosition(0);
        licensePane.addHyperlinkListener(event -> {
            if(event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(event.getURL().toURI());
                } catch (IOException | URISyntaxException e0) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e0);
                } catch (UnsupportedOperationException e1){
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Unable to open link." + this.getWidth()));
                }
            }
        });

        this.setTitle("License");
        this.setSize(new Dimension(600,650));
        this.setMinimumSize(new Dimension(300,330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setVisible(true);
    }

}
