package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    private HashMap<DecompilerWrapperInformation, ConfigPanel> configPanelHashMap;

    private ActionListener pluginsConfiguredListener;

    public PluginConfigurationEditorController(PluginConfigurationEditorView view, PluginManager pluginManager) {
        this.view = view;
        this.pluginManager = pluginManager;
        configPanelHashMap = new HashMap<>();

        view.getPluginListPanel().getWrapperJList().addListSelectionListener(listSelectionEvent -> {
            onPluginJListChange();
        });
        view.getPluginListPanel().getAddWrapperButton().addActionListener(actionEvent -> {
            addWrapper();
        });
        view.getPluginTopOptionPanel().getCloneButton().addActionListener(actionEvent -> {
            JList wrapperJList = view.getPluginListPanel().getWrapperJList();
            DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
            DecompilerWrapperInformation clonedWrapper = cloneWrapper(wrapperInformation);
            updateWrapperList(pluginManager.getWrappers());
            view.getPluginListPanel().getWrapperJList().setSelectedValue(clonedWrapper, true);
        });
        view.getPluginTopOptionPanel().getRefreshButton().addActionListener(actionEvent -> {

        });
        view.getPluginTopOptionPanel().getDeleteButton().addActionListener(actionEvent -> {
            JList wrapperJList = view.getPluginListPanel().getWrapperJList();
            DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
            removeWrapper(wrapperInformation);
        });
        view.getPluginTopOptionPanel().getOpenWebsiteButton().addActionListener(actionEvent -> {
            openDecompilerDownloadURL();
        });
        view.getOkCancelPanel().getOkButton().addActionListener(actionEvent -> {
            for (DecompilerWrapperInformation wrapperInformation: configPanelHashMap.keySet()){
                applyWrapperChange(wrapperInformation);
            }
            view.dispose();
            if (pluginsConfiguredListener != null){
                pluginsConfiguredListener.actionPerformed(new ActionEvent(this, 0 , null));
            }
        });
        view.getOkCancelPanel().getCancelButton().addActionListener(actionEvent -> {
            view.dispose();
        });

        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedIndex(0);
    }

    void onPluginJListChange(){
        if (view.getPluginListPanel().getWrapperJList().getSelectedIndex() == -1) return;
        DecompilerWrapperInformation selectedPlugin = (DecompilerWrapperInformation)view.getPluginListPanel().getWrapperJList().getSelectedValue();
        ConfigPanel configPanel = getOrCreatePluginConfigPanel(selectedPlugin);
        view.switchCard(configPanel, String.valueOf(System.identityHashCode(configPanel)));
    }

    public void openDecompilerDownloadURL(){
        JList wrapperJList = view.getPluginListPanel().getWrapperJList();
        DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
        if (wrapperInformation.getDecompilerDownloadURL() != null) {
            try {
                URI downloadURI = wrapperInformation.getDecompilerDownloadURL().toURI();
                java.awt.Desktop.getDesktop().browse(downloadURI);
            } catch (IOException | URISyntaxException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }
    }

    private void addWrapper() {
        DecompilerWrapperInformation wrapperInformation = pluginManager.createWrapper();
        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedValue(wrapperInformation, true);
    }

    private void removeWrapper(DecompilerWrapperInformation wrapperInformation) {
        JList wrapperJList = view.getPluginListPanel().getWrapperJList();
        String name = wrapperInformation.toString();
        int dialogResult = JOptionPane.showConfirmDialog(view, "Are you sure you want to remove " +
                name + "?", "Warning", JOptionPane.OK_CANCEL_OPTION);

        if (dialogResult == JOptionPane.CANCEL_OPTION) {
            return;
        }
        pluginManager.deleteWrapper(wrapperInformation);
        configPanelHashMap.remove(wrapperInformation);
        updateWrapperList(pluginManager.getWrappers());
        if (wrapperJList.getModel().getSize() == 0) {
            view.dispose();
            return;
        }
        wrapperJList.setSelectedIndex(0);
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

    private DecompilerWrapperInformation cloneWrapper(DecompilerWrapperInformation wrapperInformation){
        DecompilerWrapperInformation clonedWrapper = getDataFromPanel(wrapperInformation);
        pluginManager.setLocationForNewWrapper(clonedWrapper);
        clonedWrapper.setDecompilerDownloadURL(wrapperInformation.getDecompilerDownloadURL().toString());
        try {
            pluginManager.saveWrapper(clonedWrapper);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view,
                    "Cloned decompiler configuration could not be saved.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
        }
        pluginManager.getWrappers().add(clonedWrapper);
        return clonedWrapper;
    }

    private void applyWrapperChange(DecompilerWrapperInformation oldWrapper) {
        File f = new File(oldWrapper.getFileLocation());
        if (!f.canWrite()){
            return;
        }
        DecompilerWrapperInformation newWrapper = getDataFromPanel(oldWrapper);
        newWrapper.setFileLocation(oldWrapper.getFileLocation());
        newWrapper.setDecompilerDownloadURL(oldWrapper.getDecompilerDownloadURL().toString());
        try {
            pluginManager.replace(oldWrapper, newWrapper);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view,
                    "Your changes could not be saved. Please check your permissions to edit given files.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public DecompilerWrapperInformation getDataFromPanel(DecompilerWrapperInformation wrapperInformation){
        ConfigPanel configPanel = configPanelHashMap.get(wrapperInformation);
        DecompilerWrapperInformation newWrapper = new DecompilerWrapperInformation();

        newWrapper.setName(configPanel.getNamePanel().getTextField().getText());
        newWrapper.setWrapperURL(configPanel.getWrapperUrlPanel().getText());
        newWrapper.setDependencyURLs(configPanel.getDependencyUrlPanel().getStringList());
        return newWrapper;
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

    public void setPluginsConfiguredListener(ActionListener pluginsConfiguredListener) {
        this.pluginsConfiguredListener = pluginsConfiguredListener;
    }
}
