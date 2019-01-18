package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    HashMap<DecompilerWrapperInformation, ConfigPanel> configPanelHashMap;

    public PluginConfigurationEditorController(PluginConfigurationEditorView view, PluginManager pluginManager) {
        this.view = view;
        this.pluginManager = pluginManager;
        configPanelHashMap = new HashMap<>();

        updateWrapperList(pluginManager.getWrappers());

//        if (pluginManager.getWrappers().size() <= 1) {
//            pluginManager.createWrapper();
//            updateWrapperList(pluginManager.getWrappers());
//        }


        view.getPluginListPanel().getWrapperJList().addListSelectionListener(listSelectionEvent -> {
            onPluginJListChange();
        });
        view.getPluginListPanel().getAddWrapperButton().addActionListener(actionEvent -> {
            addWrapper();
        });
        view.getPluginTopOptionPanel().getCloneButton().addActionListener(actionEvent -> {

        });
        view.getPluginTopOptionPanel().getRefreshButton().addActionListener(actionEvent -> {

        });
        view.getPluginTopOptionPanel().getDeleteButton().addActionListener(actionEvent -> {
            removeWrapper();
        });
        view.getPluginTopOptionPanel().getOpenWebsiteButton().addActionListener(actionEvent -> {

        });
        view.getOkCancelPanel().getOkButton().addActionListener(actionEvent -> {

        });
        view.getOkCancelPanel().getCancelButton().addActionListener(actionEvent -> {

        });

        view.getPluginListPanel().getWrapperJList().setSelectedIndex(0);
    }

    void onPluginJListChange(){
        if (view.getPluginListPanel().getWrapperJList().getSelectedIndex() == -1) return;
        DecompilerWrapperInformation selectedPlugin = (DecompilerWrapperInformation)view.getPluginListPanel().getWrapperJList().getSelectedValue();
        ConfigPanel configPanel = getOrCreatePluginConfigPanel(selectedPlugin);
        view.switchCard(configPanel, String.valueOf(System.identityHashCode(configPanel)));
    }

    private void addWrapper() {
        DecompilerWrapperInformation wrapperInformation = pluginManager.createWrapper();
        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedValue(wrapperInformation, true);
    }

    private void removeWrapper() {
        JList wrapperJList = view.getPluginListPanel().getWrapperJList();
        DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
        String name = wrapperInformation.toString();
        int dialogResult = JOptionPane.showConfirmDialog(view, "Are you sure you want to remove " +
                name + "?", "Warning", JOptionPane.OK_CANCEL_OPTION);

        if (dialogResult == JOptionPane.OK_OPTION) {
            pluginManager.deleteWrapper(wrapperInformation);
            updateWrapperList(pluginManager.getWrappers());
            if (wrapperJList.getModel().getSize() == 0) {
                view.dispose();
                return;
            }
            view.getPluginListPanel().getWrapperJList().setSelectedIndex(0);
        }
    }

    public void updateWrapperList(List<DecompilerWrapperInformation> wrappers) {
        JList<DecompilerWrapperInformation> wrapperJList = view.getPluginListPanel().getWrapperJList();

        List<DecompilerWrapperInformation> pluginsWithoutJavap = new ArrayList<>(wrappers);
        for (DecompilerWrapperInformation wrapperInformation: pluginsWithoutJavap){
            if (wrapperInformation.getName().equals("javap")){
                pluginsWithoutJavap.remove(wrapperInformation);
                break;
            }
        }
        wrapperJList.setListData(pluginsWithoutJavap.toArray(new DecompilerWrapperInformation[0]));
    }

    private void applyWrapperChange() {

//        // Get data from forms
//        String name = pluginConfigPanel.getNamePanel().getText();
//        String wrapperUrl = pluginConfigPanel.getWrapperUrlPanel().getText();
//        List<String> dependencyURLs = pluginConfigPanel.getDependencyUrlPanel().getStringList();
//        String decompilerUrl = pluginConfigPanel.getDecompilerLabel().getName();
//        String fileLocation = pluginConfigPanel.getDecompilerWrapperInformatio().getFileLocation();
//
//        File f = new File(pluginConfigPanel.getDecompilerWrapperInformatio().getFileLocation());
//        if(f.canWrite()) {
//            DecompilerWrapperInformation oldWrapper = pluginConfigPanel.getDecompilerWrapperInformatio();
//            DecompilerWrapperInformation newWrapper = new DecompilerWrapperInformation(name, wrapperUrl, dependencyURLs, decompilerUrl);
//            newWrapper.setFileLocation(fileLocation);
//            view.dispose();
//            pluginManager.replace(oldWrapper, newWrapper);
//        } else {
//            JOptionPane.showMessageDialog(pluginConfigPanel,
//                    "Your changes could not be saved. Please check your permissions to edit given files.",
//                    "Saving error",
//                    JOptionPane.ERROR_MESSAGE);
//                pluginConfigPanel.repaint();
//
//        }

    }

    public ConfigPanel getOrCreatePluginConfigPanel(DecompilerWrapperInformation vmInfo){
        if (configPanelHashMap.containsKey(vmInfo)){
            return configPanelHashMap.get(vmInfo);
        }
        ConfigPanel configPanel = new ConfigPanel();
        updatePanelInfo(configPanel, vmInfo);
        configPanelHashMap.put(vmInfo, configPanel);
        return configPanel;
    }

    public void updatePanelInfo(ConfigPanel pluginConfigPanel, DecompilerWrapperInformation vmInfo) {
        boolean canWrite = true;

        JLabel label = new JLabel();
        label.setText("Configuration JSON file: " + vmInfo.getFileLocation());
        pluginConfigPanel.addComponent(label);
        if (!canWrite) {
            JLabel access = new JLabel();
            access.setText("You are not able to edit this configuration, because you don't have the right permissions.");
            pluginConfigPanel.addComponent(access);
        }

        TextInputPanel namePanel = new TextInputPanel("Name");
//        namePanel.textField.setText(vmInfo.getName());


        pluginConfigPanel.addComponent(namePanel);
        FileSelectorPanel wrapperUrlPanel = new FileSelectorPanel("Decompiler wrapper URL");
        if (vmInfo.getWrapperURL() != null) {
            wrapperUrlPanel.setText(vmInfo.getWrapperURL().getPath());
        }
        pluginConfigPanel.addComponent(wrapperUrlPanel);
        FileSelectorArrayPanel dependencyUrlPanel = new FileSelectorArrayPanel("Decompiler and dependency jars");
        if (vmInfo.getDependencyURLs() != null) {
            vmInfo.getDependencyURLs().forEach(url -> dependencyUrlPanel.addRow(url.getPath(), false));
        }
        pluginConfigPanel.addComponent(dependencyUrlPanel);
        JButton decompilerUrlLink = new JButton();
        decompilerUrlLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(vmInfo.getDecompilerURL().toString()));
                } catch (IOException | URISyntaxException e1) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e1);
                }
            }
        });
        if (vmInfo.getDecompilerURL() != null) {
            decompilerUrlLink.setToolTipText(vmInfo.getDecompilerURL().toString());
            decompilerUrlLink.setText("Go to decompiler website.");
            //decompilerUrlLink.setName();
            pluginConfigPanel.addComponent(decompilerUrlLink);
        }
//            this.setEnabled(canWrite);
//            List<Component> components = getAllComponents(this);
//            for (Component compon : components) {
//                if (!(compon instanceof JLabel)) {
//                    compon.setEnabled(canWrite);
//                }
//            }
    }
}
