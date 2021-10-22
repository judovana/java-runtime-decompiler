package org.jrd.frontend.frame.main;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoadingDialog extends JDialog {

    private JProgressBar progressBar = new JProgressBar();
    private JButton abortButton = new JButton("Abort");

    private ActionListener abortActionListener;

    public LoadingDialog(String title) {
        this.setTitle("Connecting");
        this.setSize(new Dimension(256, 144));
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);

        JLabel infoLabel = new JLabel(title);
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

    public void setAbortActionListener(ActionListener listener) {
        abortActionListener = listener;
    }
}
