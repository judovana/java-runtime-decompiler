package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.decompiling.DecompilerWrapperInformation;
import com.redhat.thermostat.vm.decompiler.swing.Templates.ConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class PluginConfigurationEditorView extends JDialog {

    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private ConfigPanel pluginConfigPanel;

    private ActionListener switchPluginListener;
    private ActionListener addWrapperButtonListener;

    private JList<DecompilerWrapperInformation> wrapperJList;

    private JButton addWrapperButton;
    private JSplitPane splitPane;

    /**
     * Modal window for editing configuration files for decompilers.
     *
     * @param mainFrameView main window
     */
    PluginConfigurationEditorView(MainFrameView mainFrameView) {

        wrapperJList = new JList<>();
        wrapperJList.setFixedCellHeight(32);
        wrapperJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wrapperJList.addListSelectionListener(listSelectionEvent -> {
            if (wrapperJList.getValueIsAdjusting()) {
                if (wrapperJList.getSelectedValue() != null)
                    switchPluginListener.actionPerformed(new ActionEvent(this, 0, null));
            }
        });

        leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(wrapperJList, BorderLayout.CENTER);
        addWrapperButton = new JButton("New");
        addWrapperButton.addActionListener(actionEvent -> {
            addWrapperButtonListener.actionPerformed(actionEvent);
        });
        leftPanel.add(addWrapperButton, BorderLayout.SOUTH);

        rightPanel = new JPanel(new BorderLayout());

        splitPane = new JSplitPane();
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(200);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitPane);


        this.setTitle("Plugin configuration");
        this.setSize(new Dimension(960, 540));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
    }

    public void setSwitchPluginListener(ActionListener switchPluginListener) {
        this.switchPluginListener = switchPluginListener;
    }

    public void setAddWrapperButtonListener(ActionListener addWrapperButtonListener) {
        this.addWrapperButtonListener = addWrapperButtonListener;
    }

    public void switchPlugin() {
        // Show dialog save/discard/cancel
        pluginConfigPanel = new ConfigPanel(wrapperJList.getSelectedValue());

        rightPanel.removeAll();
        rightPanel.add(pluginConfigPanel, BorderLayout.CENTER);
        rightPanel.revalidate();
    }

    public ConfigPanel getPluginConfigPanel() {
        return pluginConfigPanel;
    }

    public JList<DecompilerWrapperInformation> getWrapperJList() {
        return wrapperJList;
    }
}