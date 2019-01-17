package org.jrd.frontend.PluginMangerFrame;
import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import java.awt.*;

public class PluginConfigurationEditorView extends JDialog {

    private final JPanel configAndOptionPanel;
    private final JScrollPane scrollPane;
    private final CardLayout cardLayout;
    private final PluginListPanel pluginListPanel;
    private final PluginTopOptionPanel pluginTopOptionPanel;
    private final JPanel centerPanel;
    private final JPanel cardConfigPanel;
    private final OkCancelPanel okCancelPanel;

    /**
     * Modal window for editing configuration files for decompilers.
     *
     * @param mainFrameView main window
     */
    public PluginConfigurationEditorView(MainFrameView mainFrameView) {
        this.setLayout(new BorderLayout());
        this.setTitle("Plugin configuration");
        this.setSize(new Dimension(960, 540));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.RED);
        okCancelPanel = new OkCancelPanel();
        pluginListPanel = new PluginListPanel();
        pluginTopOptionPanel = new PluginTopOptionPanel();
        configAndOptionPanel = new JPanel(new BorderLayout());
        cardLayout = new CardLayout();
        cardConfigPanel = new JPanel(cardLayout);
        scrollPane = new JScrollPane(cardConfigPanel);

        centerPanel.add(pluginListPanel, BorderLayout.WEST);
        configAndOptionPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(configAndOptionPanel, BorderLayout.CENTER);
        configAndOptionPanel.add(pluginTopOptionPanel, BorderLayout.NORTH);

        this.add(centerPanel, BorderLayout.CENTER);
        this.add(okCancelPanel, BorderLayout.SOUTH);
    }

    public JPanel getConfigAndOptionPanel() {
        return configAndOptionPanel;
    }

    public JPanel getCenterPanel() {
        return centerPanel;
    }

    public OkCancelPanel getOkCancelPanel() {
        return okCancelPanel;
    }
}