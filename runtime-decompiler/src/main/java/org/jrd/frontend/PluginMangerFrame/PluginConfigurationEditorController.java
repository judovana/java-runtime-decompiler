package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    private HashMap<DecompilerWrapperInformation, ConfigPanel> configPanelHashMap;

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
        if (vmInfo.getFileLocation() != null){
            pluginConfigPanel.getJsonFileURL().setText("Location: " + vmInfo.getFileLocation());
            if(!Files.isWritable(Paths.get(vmInfo.getFileLocation()))){
                pluginConfigPanel.getMessagePanel().setVisible(true);
            }
        }
        if (vmInfo.getName() != null){
            pluginConfigPanel.getNamePanel().getTextField().setText(vmInfo.getName());
        }
        if (vmInfo.getDependencyURLs() != null){
            vmInfo.getDependencyURLs().forEach(url -> {
                pluginConfigPanel.getDependencyUrlPanel().addRow(url.getPath(), false);
            });
        }
        if (vmInfo.getWrapperURL() != null){
            pluginConfigPanel.getWrapperUrlPanel().setText(vmInfo.getWrapperURL().getPath());
        }
    }
}
