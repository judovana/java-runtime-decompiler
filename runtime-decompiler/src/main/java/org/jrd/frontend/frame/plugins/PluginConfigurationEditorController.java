package org.jrd.frontend.frame.plugins;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Directories;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.ExpandableUrl;
import org.jrd.backend.decompiling.ImportUtils;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    private Map<DecompilerWrapper, ConfigPanel> configPanelHashMap;

    private ActionListener pluginsConfiguredListener;

    public PluginConfigurationEditorController(PluginConfigurationEditorView view, PluginManager pluginManager) {
        this.view = view;
        this.pluginManager = pluginManager;
        configPanelHashMap = new HashMap<>();

        view.getPluginListPanel().getWrapperJList().addListSelectionListener(selectionEvent -> onPluginJListChange());
        view.getPluginListPanel().getAddWrapperButton().addActionListener(actionEvent -> addWrapper());
        view.getPluginTopOptionPanel().getCloneButton().addActionListener(actionEvent -> {
            DecompilerWrapper wrapper = view.getSelectedWrapper();
            DecompilerWrapper clonedWrapper = cloneWrapper(wrapper);

            updateWrapperList(pluginManager.getWrappers());
            view.getPluginListPanel().getWrapperJList().setSelectedValue(clonedWrapper, true);
        });
        view.getPluginTopOptionPanel().getDeleteButton().addActionListener(actionEvent -> {
            DecompilerWrapper wrapper = view.getSelectedWrapper();
            removeWrapper(wrapper);
        });
        view.getPluginTopOptionPanel().getOpenWebsiteButton().addActionListener(actionEvent -> openDownloadUrl());
        view.getPluginTopOptionPanel().getImportButton().addActionListener(actionEvent -> openImportDialog());
        view.getOkCancelPanel().getOkButton().addActionListener(actionEvent -> {
            for (DecompilerWrapper wrapper: configPanelHashMap.keySet()) {
                applyWrapperChange(wrapper);
            }
            view.dispose();
            if (pluginsConfiguredListener != null) {
                pluginsConfiguredListener.actionPerformed(new ActionEvent(this, 0, null));
            }
        });
        view.getOkCancelPanel().getCancelButton().addActionListener(actionEvent -> view.dispose());
        view.getOkCancelPanel().getValidateButton().addActionListener(event -> validateWrapper());

        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedIndex(0);
    }

    private void openImportDialog() {
        List<URL> availableDecompilers = ImportUtils.getWrappersFromClasspath();
        List<String> availableDecompilerNames = new ArrayList<>();

        for (URL url : availableDecompilers) {
            try {
                URL javaWrapperComplement = new URL(ImportUtils.flipWrapperExtension(url.toString()));
                if (javaWrapperComplement.openStream() != null) {
                    availableDecompilerNames.add(url.toString().substring(url.toString().lastIndexOf("/") + 1));
                }
            } catch (IOException e) {
                Logger.getLogger().log(Logger.Level.ALL, e);
            }
        }

        Object selected = JOptionPane.showInputDialog(this.view,
                "Which decompiler would you like to import?\n" +
                        "The selected option will be copied to " + Directories.getPluginDirectory() + ".",
                "Import decompiler files",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableDecompilerNames.toArray(),
                availableDecompilerNames.toArray()[0]);

        if (selected != null) { // null if the user cancels
            URL selectedUrl = availableDecompilers.get(availableDecompilerNames.indexOf(selected.toString()));
            String selectedFilename = ImportUtils.filenameFromUrl(selectedUrl);

            if (new File(Directories.getPluginDirectory() + File.separator + selectedFilename).exists()) {
                if (confirmWrapperOverwrite() != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            ImportUtils.importOnePlugin(selectedUrl, selectedFilename);

            configPanelHashMap.clear();
            pluginManager.loadConfigs();
            List<DecompilerWrapper> newWrappers = pluginManager.getWrappers();
            updateWrapperList(newWrappers);

            for (int i = 0; i < newWrappers.size(); i++) {
                if (selected.toString().contains(newWrappers.get(i).getFullyQualifiedClassName())) {
                    view.getPluginListPanel().getWrapperJList().setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private int confirmWrapperOverwrite() {
        String[] options = {"Yes", "No"};
        return JOptionPane.showOptionDialog(this.view,
                "An identical file already exists in " + Directories.getPluginDirectory() + ".\n" +
                        "Do you want to continue and overwrite the file?",
                "Confirmation",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
    }

    void onPluginJListChange() {
        if (view.getPluginListPanel().getWrapperJList().getSelectedIndex() == -1) {
            return;
        }

        DecompilerWrapper selectedPlugin = view.getSelectedWrapper();
        ConfigPanel configPanel = getOrCreatePluginConfigPanel(selectedPlugin);

        toggleWebsiteButton(selectedPlugin);

        view.switchCard(configPanel, String.valueOf(System.identityHashCode(configPanel)));
    }

    private void toggleWebsiteButton(DecompilerWrapper plugin) {
        if (plugin.getDecompilerDownloadUrl() != null) {
            view.getPluginTopOptionPanel().getOpenWebsiteButton().setEnabled(true);
            view.getPluginTopOptionPanel().getOpenWebsiteButton().setToolTipText(null);
        } else {
            view.getPluginTopOptionPanel().getOpenWebsiteButton().setEnabled(false);
            view.getPluginTopOptionPanel().getOpenWebsiteButton().setToolTipText("<html>" +
                    "Button disabled by default on user-created plugins.<br />" +
                    "You can manually set the download URL in this plugin's JSON file to enable this button." +
                    "</html>");
        }
    }


    public void openDownloadUrl() {
        DecompilerWrapper wrapper = view.getSelectedWrapper();

        if (wrapper.getDecompilerDownloadUrl() != null) {
            try {
                URI downloadUri = wrapper.getDecompilerDownloadUrl().toURI();
                java.awt.Desktop.getDesktop().browse(downloadUri);
            } catch (IOException | URISyntaxException e) {
                Logger.getLogger().log(Logger.Level.ALL, e);
            } catch (UnsupportedOperationException e) {
                JOptionPane.showMessageDialog(
                        view,
                        "Website could not be opened automatically. Go to: " +
                            wrapper.getDecompilerDownloadUrl().toString());
            }
        }
    }

    private void addWrapper() {
        DecompilerWrapper wrapper = pluginManager.createWrapper();
        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedValue(wrapper, true);
    }

    private void removeWrapper(DecompilerWrapper wrapper) {
        if (wrapper == null) {
            Logger.getLogger().log(Logger.Level.DEBUG, "Attempted delete operation with no plugin wrapper selected.");
            return;
        }

        String name = wrapper.toString();
        int dialogResult = JOptionPane.showConfirmDialog(view, "Are you sure you want to remove " +
                name + "?", "Warning", JOptionPane.OK_CANCEL_OPTION);
        if (dialogResult == JOptionPane.CANCEL_OPTION) {
            return;
        }

        pluginManager.deleteWrapper(wrapper);
        configPanelHashMap.remove(wrapper);
        updateWrapperList(pluginManager.getWrappers());

        JList wrapperJList = view.getPluginListPanel().getWrapperJList();
        if (wrapperJList.getModel().getSize() == 0) {
            view.clearConfigPanel();
            return;
        }
        wrapperJList.setSelectedIndex(0);
    }

    private void validateWrapper() {
        if (view.getPluginListPanel().getWrapperJList().getSelectedIndex() == -1) {
            return;
        }

        String result = pluginManager.validatePlugin(getDataFromPanel(view.getSelectedWrapper()));

        if (result != null) {
            JOptionPane.showMessageDialog(view,
                    "Validation failed: " + result,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    view, "This plugin is valid.", "Success", JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public void updateWrapperList(List<DecompilerWrapper> wrappers) {
        JList<DecompilerWrapper> wrapperJList = view.getPluginListPanel().getWrapperJList();

        List<DecompilerWrapper> pluginsWithoutJavap = new ArrayList<>(wrappers.size());
        for (DecompilerWrapper wrapper : wrappers) {
            if (!wrapper.isJavap() && !wrapper.isJavapVerbose()) {
                pluginsWithoutJavap.add(wrapper);
            }
        }

        wrapperJList.setListData(pluginsWithoutJavap.toArray(new DecompilerWrapper[0]));
    }

    private DecompilerWrapper cloneWrapper(DecompilerWrapper wrapper) {
        DecompilerWrapper clonedWrapper = getDataFromPanel(wrapper);
        pluginManager.setLocationForNewWrapper(clonedWrapper);
        clonedWrapper.setDecompilerDownloadUrl(wrapper.getDecompilerDownloadUrl().toString());
        try {
            pluginManager.saveWrapper(clonedWrapper);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view,
                    "Cloned decompiler configuration could not be saved.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
        }
        pluginManager.addWrapper(clonedWrapper);
        return clonedWrapper;
    }

    private void applyWrapperChange(DecompilerWrapper oldWrapper) {
        File f = new File(oldWrapper.getFileLocation());
        if (!f.canWrite()) {
            return;
        }
        DecompilerWrapper newWrapper = getDataFromPanel(oldWrapper);
        newWrapper.setFileLocation(oldWrapper.getFileLocation());
        if (oldWrapper.getDecompilerDownloadUrl() == null) {
            newWrapper.setDecompilerDownloadUrl("");
        } else {
            newWrapper.setDecompilerDownloadUrl(oldWrapper.getDecompilerDownloadUrl().toString());
        }
        try {
            pluginManager.replace(oldWrapper, newWrapper);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view,
                    "Your changes could not be saved. Please check your permissions to edit given files.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public DecompilerWrapper getDataFromPanel(DecompilerWrapper wrapper) {
        ConfigPanel configPanel = configPanelHashMap.get(wrapper);
        DecompilerWrapper newWrapper = new DecompilerWrapper();

        newWrapper.setName(configPanel.getNamePanel().getTextField().getText());
        newWrapper.setWrapperUrlFromPath(configPanel.getWrapperUrlPanel().getText());
        newWrapper.setDependencyUrlsFromPath(configPanel.getDependencyUrlPanel().getStringList());
        return newWrapper;
    }

    public ConfigPanel getOrCreatePluginConfigPanel(DecompilerWrapper vmInfo) {
        if (configPanelHashMap.containsKey(vmInfo)) {
            return configPanelHashMap.get(vmInfo);
        }
        ConfigPanel configPanel = new ConfigPanel();
        updatePanelInfo(configPanel, vmInfo);
        configPanelHashMap.put(vmInfo, configPanel);
        return configPanel;
    }

    public void updatePanelInfo(ConfigPanel pluginConfigPanel, DecompilerWrapper vmInfo) {
        if (vmInfo.getFileLocation() != null) {
            pluginConfigPanel.getJsonFileUrl().setText("Location: " + vmInfo.getFileLocation());
            if (!Files.isWritable(Paths.get(vmInfo.getFileLocation()))) {
                pluginConfigPanel.getMessagePanel().setVisible(true);
            }
        }
        if (vmInfo.getName() != null) {
            pluginConfigPanel.getNamePanel().getTextField().setText(vmInfo.getName());
        }

        List<ExpandableUrl> dependencyUrls = vmInfo.getDependencyUrls();
        if (dependencyUrls != null) {
            dependencyUrls.forEach(url -> pluginConfigPanel.getDependencyUrlPanel().addRow(url.getRawPath(), false));
        }
        if (vmInfo.getWrapperUrl() != null) {
            pluginConfigPanel.getWrapperUrlPanel().setText(vmInfo.getWrapperUrl().getRawPath());
        }
    }

    public void setPluginsConfiguredListener(ActionListener pluginsConfiguredListener) {
        this.pluginsConfiguredListener = pluginsConfiguredListener;
    }
}
