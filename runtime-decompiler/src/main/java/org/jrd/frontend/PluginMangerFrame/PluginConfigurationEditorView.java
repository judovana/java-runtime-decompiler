package org.jrd.frontend.PluginMangerFrame;
import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import java.awt.*;

public class PluginConfigurationEditorView extends JDialog {

    JPanel configAndOptionPanel;
    JScrollPane scrollPane;
    CardLayout cardLayout;
    JPanel centerpanel;
    JPanel cardConfigPanel;
    OkCancelPanel okCancelPanel;


    /**
     * Modal window for editing configuration files for decompilers.
     *
     * @param mainFrameView main window
     */
    public PluginConfigurationEditorView(MainFrameView mainFrameView) {
        this.setLayout(new BorderLayout());

        okCancelPanel = new OkCancelPanel();
        centerpanel = new JPanel(new BorderLayout());
        centerpanel.add(new PluginListPanel(), BorderLayout.WEST);
        configAndOptionPanel = new JPanel(new BorderLayout());
        configAndOptionPanel.add(new PluginTopOptionPanel(), BorderLayout.NORTH);
        cardLayout = new CardLayout();
        cardConfigPanel = new JPanel(cardLayout);
        scrollPane = new JScrollPane(cardConfigPanel);
        cardLayout.show(cardConfigPanel, "UUID");
        configAndOptionPanel.add(scrollPane, BorderLayout.CENTER);
        centerpanel.add(configAndOptionPanel, BorderLayout.CENTER);
        centerpanel.setBackground(Color.RED);
        this.add(centerpanel, BorderLayout.CENTER);
        this.add(okCancelPanel, BorderLayout.SOUTH);


        this.setTitle("Plugin configuration");
        this.setSize(new Dimension(960, 540));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
    }
}