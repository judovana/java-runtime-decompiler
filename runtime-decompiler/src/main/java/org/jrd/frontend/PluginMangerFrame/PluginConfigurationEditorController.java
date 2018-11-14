package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    private ConfigPanel pluginConfigPanel;
    private ActionListener updateWrapperListsActionListener;


    public void setUpdateWrapperListsActionListener(ActionListener updateWrapperListsActionListner) {
        this.updateWrapperListsActionListener = updateWrapperListsActionListner;
    }

    public PluginConfigurationEditorController(PluginConfigurationEditorView view, PluginManager pluginManager) {
        this.view = view;
        this.pluginManager = pluginManager;
        updateWrapperList(pluginManager.getWrappers());
        if (pluginManager.getWrappers().size() == 0) {
            pluginManager.createWrapper();
            updateWrapperList(pluginManager.getWrappers());
        }
        view.getWrapperJList().setSelectedIndex(0);
        view.switchPlugin();
        updateListeners();
        view.setAddWrapperButtonListener(actionEvent -> {
            addWrapper();
        });
        view.setSwitchPluginListener(actionEvent -> {
            switchPlugin();
        });
    }

    private void addWrapper() {
        DecompilerWrapperInformation wrapperInformation = pluginManager.createWrapper();
        updateWrapperList(pluginManager.getWrappers());
        view.getWrapperJList().setSelectedValue(wrapperInformation, true);
        switchPlugin();
    }

    private void switchPlugin() {
        // Show dialog save/discard/cancel
        view.switchPlugin();
        updateListeners();
    }

    private void updateListeners() {
        pluginConfigPanel = view.getPluginConfigPanel();
        pluginConfigPanel.setOkButtonListener(actionEvent -> {
            applyWrapperChange();
            updateWrapperListsActionListener.actionPerformed(new ActionEvent(this, 0, null));
        });
        pluginConfigPanel.setCancelButtonListener(actionEvent -> {
            view.dispose();
        });
        pluginConfigPanel.setRemoveButtonListener(actionEvent -> {
            removeWrapper();
        });
    }

    private void removeWrapper() {
        DecompilerWrapperInformation wrapperInformation = pluginConfigPanel.getDecompilerWrapperInformatio();
        String name = wrapperInformation.toString();
        int dialogResult = JOptionPane.showConfirmDialog(view, "Are you sure you want to remove " +
                name + "?", "Warning", JOptionPane.OK_CANCEL_OPTION);
        if (dialogResult == JOptionPane.OK_OPTION) {
            pluginManager.deleteWrapper(wrapperInformation);
            if (pluginManager.getWrappers().size() == 0) {
                view.dispose();
                return;
            }
            updateWrapperListsActionListener.actionPerformed(new ActionEvent(this, 1, null));
            view.getWrapperJList().setSelectedIndex(0);
            switchPlugin();
        }
    }

    public void updateWrapperList(List<DecompilerWrapperInformation> wrappers) {
        JList<DecompilerWrapperInformation> wrapperList = view.getWrapperJList();
        wrapperList.setListData(wrappers.toArray(new DecompilerWrapperInformation[0]));
    }

    private void applyWrapperChange() {

        // Get data from forms
        String name = pluginConfigPanel.getNamePanel().getText();
        String wrapperUrl = pluginConfigPanel.getWrapperUrlPanel().getText();
        List<String> dependencyURLs = pluginConfigPanel.getDependencyUrlPanel().getStringList();
        String decompilerUrl = pluginConfigPanel.getDecompilerLabel().getName();
        String fileLocation = pluginConfigPanel.getDecompilerWrapperInformatio().getFileLocation();

        File f = new File(pluginConfigPanel.getDecompilerWrapperInformatio().getFileLocation());
        if(f.canWrite() || f.getName().equals(".json")) {
            DecompilerWrapperInformation oldWrapper = pluginConfigPanel.getDecompilerWrapperInformatio();
            DecompilerWrapperInformation newWrapper = new DecompilerWrapperInformation(name, wrapperUrl, dependencyURLs, decompilerUrl);
            newWrapper.setFileLocation(fileLocation);
            view.dispose();
            pluginManager.replace(oldWrapper, newWrapper);
        } else {
            JOptionPane.showMessageDialog(pluginConfigPanel,
                    "Your changes could not be saved. Please check your permissions to edit given files.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
                pluginConfigPanel.repaint();

        }

    }
}
