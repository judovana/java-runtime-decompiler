package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.decompiling.DecompilerWrapperInformation;
import com.redhat.thermostat.vm.decompiler.swing.Templates.ConfigPanel;
import com.redhat.thermostat.vm.decompiler.swing.Templates.FileSelectorArrayPanel;
import com.redhat.thermostat.vm.decompiler.swing.Templates.FileSelectorPanel;
import com.redhat.thermostat.vm.decompiler.swing.Templates.TextInputPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class PluginConfigurationEditorView extends JDialog {

    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private ConfigPanel pluginConfigPanel;
    private JList<DecompilerWrapperInformation> wrapperList;
    private JSplitPane splitPane;

    private TextInputPanel namePanel;
    private TextInputPanel classNamePanel;
    private FileSelectorPanel wrapperUrlPanel;
    private FileSelectorArrayPanel dependencyUrlPanel;

    PluginConfigurationEditorView(MainFrameView mainFrameView) {
        mainPanel = new JPanel(new BorderLayout());

        splitPane = new JSplitPane();
        splitPane.setDividerLocation(200);
        mainPanel.add(splitPane);

        wrapperList = new JList<>();
        wrapperList.addListSelectionListener(listSelectionEvent -> {
            if (wrapperList.getValueIsAdjusting()){
                switchPlugin();
            }
        });

        pluginConfigPanel = new ConfigPanel();

        leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(wrapperList);

        splitPane.setLeftComponent(leftPanel);

        rightPanel = new JPanel(new BorderLayout());
        splitPane.setRightComponent(rightPanel);

        rightPanel.add(pluginConfigPanel, BorderLayout.CENTER);

        this.setTitle("Plugin configuration");
        this.setSize(new Dimension(960, 540));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
    }

    void updateWrapperList(List<DecompilerWrapperInformation> wrappers){
        wrapperList.clearSelection();
        wrapperList.setListData(wrappers.toArray(new DecompilerWrapperInformation[0]));
        wrapperList.setSelectedIndex(0);
        switchPlugin();
    }


    void switchPlugin(){
        // Show dialog save/discard/cancel
        pluginConfigPanel = new ConfigPanel();
        DecompilerWrapperInformation selectedWrapper = wrapperList.getSelectedValue();
        namePanel = new TextInputPanel("Name");
        namePanel.textField.setText(selectedWrapper.getName());
        pluginConfigPanel.addComponent(namePanel);
        classNamePanel = new TextInputPanel("Fully Qualified name");
        classNamePanel.textField.setText(selectedWrapper.getFullyQualifiedClassName());
        pluginConfigPanel.addComponent(classNamePanel);
        wrapperUrlPanel = new FileSelectorPanel("Decompiler Wrapper URL");
        if (selectedWrapper.getWrapperURL() != null){
            wrapperUrlPanel.textField.setText(selectedWrapper.getWrapperURL().getPath());
        }
        pluginConfigPanel.addComponent(wrapperUrlPanel);
        dependencyUrlPanel = new FileSelectorArrayPanel("DependencyArray");
        if (selectedWrapper.getDependencyURLs() != null){
            selectedWrapper.getDependencyURLs().forEach(url -> dependencyUrlPanel.addUrl(url));
        }
        pluginConfigPanel.addComponent(dependencyUrlPanel);

        rightPanel.removeAll();
        rightPanel.add(pluginConfigPanel, BorderLayout.CENTER);
        rightPanel.revalidate();
    }

}