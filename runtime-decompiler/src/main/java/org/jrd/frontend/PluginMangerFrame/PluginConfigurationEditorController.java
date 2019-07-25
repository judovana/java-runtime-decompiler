package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Directories;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.PluginMangerFrame.embedded.Directory;
import org.jrd.frontend.PluginMangerFrame.embedded.Listable;
import org.jrd.frontend.PluginMangerFrame.embedded.Zip;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import static org.jrd.backend.decompiling.PluginManager.createUserPluginDir;
import static org.jrd.backend.decompiling.PluginManager.flipWrapperExtension;

public class PluginConfigurationEditorController {

    private PluginManager pluginManager;
    private PluginConfigurationEditorView view;
    private HashMap<DecompilerWrapperInformation, ConfigPanel> configPanelHashMap;

    private ActionListener pluginsConfiguredListener;

    public PluginConfigurationEditorController(PluginConfigurationEditorView view, PluginManager pluginManager) {
        this.view = view;
        this.pluginManager = pluginManager;
        configPanelHashMap = new HashMap<>();

        view.getPluginListPanel().getWrapperJList().addListSelectionListener(listSelectionEvent -> onPluginJListChange());
        view.getPluginListPanel().getAddWrapperButton().addActionListener(actionEvent -> addWrapper());
        view.getPluginTopOptionPanel().getCloneButton().addActionListener(actionEvent -> {
            JList wrapperJList = view.getPluginListPanel().getWrapperJList();
            DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
            DecompilerWrapperInformation clonedWrapper = cloneWrapper(wrapperInformation);
            updateWrapperList(pluginManager.getWrappers());
            view.getPluginListPanel().getWrapperJList().setSelectedValue(clonedWrapper, true);
        });
        view.getPluginTopOptionPanel().getDeleteButton().addActionListener(actionEvent -> {
            JList wrapperJList = view.getPluginListPanel().getWrapperJList();
            DecompilerWrapperInformation wrapperInformation = (DecompilerWrapperInformation) wrapperJList.getSelectedValue();
            removeWrapper(wrapperInformation);
        });
        view.getPluginTopOptionPanel().getOpenWebsiteButton().addActionListener(actionEvent -> openDecompilerDownloadURL());
        view.getPluginTopOptionPanel().getImportButton().addActionListener(actionEvent -> openImportDialog());
        view.getOkCancelPanel().getOkButton().addActionListener(actionEvent -> {
            for (DecompilerWrapperInformation wrapperInformation: configPanelHashMap.keySet()){
                applyWrapperChange(wrapperInformation);
            }
            view.dispose();
            if (pluginsConfiguredListener != null){
                pluginsConfiguredListener.actionPerformed(new ActionEvent(this, 0 , null));
            }
        });
        view.getOkCancelPanel().getCancelButton().addActionListener(actionEvent -> view.dispose());
        view.getOkCancelPanel().getValidateButton().addActionListener(actionEvent -> {
            if(view.getPluginListPanel().getWrapperJList().getSelectedIndex() == -1) return;
            String result = pluginManager.validatePlugin(getDataFromPanel((DecompilerWrapperInformation) view.getPluginListPanel().getWrapperJList().getSelectedValue()));
            if (result != null){
                JOptionPane.showMessageDialog(view,
                        "Validation failed: " + result,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }else {
                JOptionPane.showMessageDialog(view,"This plugin is valid.","Success", JOptionPane.INFORMATION_MESSAGE);
            }

        });

        updateWrapperList(pluginManager.getWrappers());
        view.getPluginListPanel().getWrapperJList().setSelectedIndex(0);
    }

    private List<URL> getWrappersFromClasspath(){
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        List<URL> jsonFiles = new ArrayList<>();

        for(String entry : classpathEntries) {
            try {
                Listable listable;
                if(entry.endsWith(".jar")){
                    listable = new Zip(new File(entry));
                } else {
                    listable = new Directory(new File(entry));
                }
                jsonFiles.addAll(listable.listChildren());

            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }
        return jsonFiles;
    }

    private void openImportDialog(){
        List<URL> availableDecompilers = getWrappersFromClasspath();
        ArrayList<String> availableDecompilerNames = new ArrayList<>();

        for(URL url : availableDecompilers){
            try {
                URL javaWrapperComplement = new URL(flipWrapperExtension(url.toString()));
                if (javaWrapperComplement.openStream() != null){
                    availableDecompilerNames.add(url.toString().substring(url.toString().lastIndexOf("/") + 1));
                }
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }

        Object selected = JOptionPane.showInputDialog(null,
                "Which decompiler would you like to import?",
                "Import decompiler",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableDecompilerNames.toArray(),
                availableDecompilerNames.toArray()[0]);

        if(selected != null){ // null if the user cancels
            URL selectedURL = availableDecompilers.get(availableDecompilerNames.indexOf(selected.toString()));
            String selectedFilename = selectedURL.toString().substring(selectedURL.toString().lastIndexOf("/") + 1);

            try {
                copyWrappers(selectedURL, selectedFilename);
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }

        pluginManager.loadConfigs();
        updateWrapperList(pluginManager.getWrappers());
    }

    private void copyWrappers(URL wrapperURL, String wrapperFilename) throws IOException {
        InputStream is = null;
        OutputStream os = null;

        createUserPluginDir();

        try {
            is = wrapperURL.openStream();
            os = new FileOutputStream(Directories.getPluginDirectory() + File.separator + wrapperFilename);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }

        URL javaComplement = new URL(flipWrapperExtension(wrapperURL.toString()));
        String javaComplementName = flipWrapperExtension(wrapperFilename);
        try {
            is = javaComplement.openStream();
            os = new FileOutputStream(Directories.getPluginDirectory() + File.separator + javaComplementName);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
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
            } catch (UnsupportedOperationException e){
                JOptionPane.showMessageDialog(view, "Website could not be opened automatically. Go to: "  + wrapperInformation.getDecompilerDownloadURL().toString());
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
        for (int x = 0; x< pluginsWithoutJavap.size() ; x++) {
            DecompilerWrapperInformation wrapperInformation = pluginsWithoutJavap.get(x);
            if (wrapperInformation.getName().equals(DecompilerWrapperInformation.JAVAP_NAME)
                    || wrapperInformation.getName().equals(DecompilerWrapperInformation.JAVAP_VERBOSE_NAME)) {
                pluginsWithoutJavap.remove(wrapperInformation);
                x--;
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
        newWrapper.setWrapperURLFromPath(configPanel.getWrapperUrlPanel().getText());
        newWrapper.setDependencyURLsFromPath(configPanel.getDependencyUrlPanel().getStringList());
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
            vmInfo.getDependencyURLs().forEach(url -> pluginConfigPanel.getDependencyUrlPanel().addRow(url.getRawPath(), false));
        }
        if (vmInfo.getWrapperURL() != null){
            pluginConfigPanel.getWrapperUrlPanel().setText(vmInfo.getWrapperURL().getRawPath());
        }
    }

    public void setPluginsConfiguredListener(ActionListener pluginsConfiguredListener) {
        this.pluginsConfiguredListener = pluginsConfiguredListener;
    }
}
