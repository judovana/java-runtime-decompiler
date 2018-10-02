package org.jrd.frontend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class LoadingDialog extends JDialog {

    private JLabel infoLabel = new JLabel("Loading classes");
    private JProgressBar progressBar = new JProgressBar();
    private JButton abortButton = new JButton("Abort");

    private ActionListener abortActionListener;

    LoadingDialog() {
        this.setTitle("Connecting");
        this.setSize(new Dimension(256, 144));
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);

        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(infoLabel, BorderLayout.NORTH);
        this.add(progressBar, BorderLayout.CENTER);
        progressBar.setIndeterminate(true);
        this.add(abortButton, BorderLayout.SOUTH);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                abort();
            }
        });

        abortButton.addActionListener(event -> abort());
    }

    private void abort() {
        ActionEvent abortEvent = new ActionEvent(this, 0, null);
        abortActionListener.actionPerformed(abortEvent);
    }

    void setAbortActionListener(ActionListener listener) {
        abortActionListener = listener;
    }
}
