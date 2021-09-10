package org.jrd.frontend.frame.plugins;

import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.frontend.frame.main.MainFrameView;

import javax.swing.*;
import java.awt.*;

public class PluginConfigurationEditorView extends JDialog {

    private final JPanel configAndOptionPanel;
    private final JScrollPane scrollPane;
    private final CardLayout cardLayoutForConfigPanels;
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
        this.setTitle("Configure Plugins");
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
        cardLayoutForConfigPanels = new CardLayout();
        cardConfigPanel = new JPanel(cardLayoutForConfigPanels);
        scrollPane = new JScrollPane(cardConfigPanel);

        centerPanel.add(pluginListPanel, BorderLayout.WEST);
        configAndOptionPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(configAndOptionPanel, BorderLayout.CENTER);
        configAndOptionPanel.add(pluginTopOptionPanel, BorderLayout.NORTH);

        this.add(centerPanel, BorderLayout.CENTER);
        this.add(okCancelPanel, BorderLayout.SOUTH);
    }

    public PluginListPanel getPluginListPanel() {
        return pluginListPanel;
    }

    public PluginTopOptionPanel getPluginTopOptionPanel() {
        return pluginTopOptionPanel;
    }

    public OkCancelPanel getOkCancelPanel() {
        return okCancelPanel;
    }

    public void switchCard(JPanel jPanel, String id) {
        boolean isNew = true;
        for (Component component : cardConfigPanel.getComponents()) {
            if (jPanel == component) {
                isNew = false;
                break;
            }
        }
        if (isNew) {
            cardConfigPanel.add(jPanel, id);
        }
        cardLayoutForConfigPanels.show(cardConfigPanel, id);
    }

    public void clearConfigPanel() {
        assert cardConfigPanel.getComponentCount() == 1;

        cardConfigPanel.removeAll();
        cardConfigPanel.revalidate();
        cardConfigPanel.repaint();
    }

    DecompilerWrapper getSelectedWrapper() {
        return (DecompilerWrapper) getPluginListPanel().getWrapperJList().getSelectedValue();
    }
}
