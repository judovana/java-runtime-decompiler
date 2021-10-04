package org.jrd.frontend.frame.settings;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.ArchiveManagerOptions;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.Directories;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;
import org.jrd.frontend.frame.main.MainFrameView;
import org.jrd.frontend.frame.plugins.FileSelectorArrayRow;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsView extends JDialog {

    private JPanel mainPanel;
    private SettingsPanel settingsPanel;
    private JPanel okCancelPanel;
    private JButton okButton;
    private JButton cancelButton;

    private final Config config = Config.getConfig();

    public static class SettingsPanel extends JPanel {

        private JTextField agentPathTextField;
        private JLabel agentPathLabel;
        private JButton browseButton;
        private JLabel checkBoxSettings;
        private JCheckBox useHostSystemClassesCheckBox;

        private JFileChooser chooser;

        private JCheckBox useDefaults;
        private JTextField newExtensionsTextField;
        private JLabel nestedJars;
        private JButton addButton;
        private JButton removeButton;
        private DefaultListModel<String> defaultListModel;
        private JList<String> currentExtensionsList;
        private JScrollPane scrollPane;


        SettingsPanel(String initialAgentPath, boolean initialUseHostSystemClasses) {

            this.agentPathTextField = new JTextField();
            this.agentPathTextField.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                    "Select a path to the Decompiler Agent.<br />" +
                    FileSelectorArrayRow.getTextFieldToolTip()
            );
            this.agentPathTextField.setText(initialAgentPath);

            this.agentPathLabel = new JLabel("Decompiler Agent path");
            this.browseButton = new JButton("Browse");

            this.checkBoxSettings = new JLabel("Settings");
            this.useHostSystemClassesCheckBox = new JCheckBox(
                    "Use host system classes during compilation phase of class overwrite",
                    initialUseHostSystemClasses
            );

            chooser = new JFileChooser();
            File dir;
            if (Directories.isPortable()) {
                dir = new File(Directories.getJrdLocation() + File.separator + "libs");
            } else {
                dir = new File(Directories.getJrdLocation() + File.separator +
                        "decompiler_agent" + File.separator + "target");
            }
            chooser.setCurrentDirectory(FileSelectorArrayRow.fallback(dir));

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;

            addAgent(gbc);

            // Nested Jars
            nestedJars = new JLabel("Nested Jars Settings:");
            newExtensionsTextField = new JTextField();
            newExtensionsTextField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    super.keyReleased(e);
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        confirmExtensions();
                    }
                }
            });

            defaultListModel = new DefaultListModel<>();
            currentExtensionsList = new JList<>(defaultListModel);
            currentExtensionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            currentExtensionsList.setLayoutOrientation(JList.VERTICAL);
            currentExtensionsList.setVisibleRowCount(-1);

            scrollPane = new JScrollPane(currentExtensionsList);
            scrollPane.setPreferredSize(new Dimension(0, 200));

            addButton = new JButton("Add");
            addButton.addActionListener(actionEvent -> confirmExtensions());

            removeButton = new JButton("Remove");
            removeButton.addActionListener(actionEvent -> {
                try {
                    defaultListModel.removeElementAt(currentExtensionsList.getSelectedIndex());
                } catch (ArrayIndexOutOfBoundsException e) {
                    Logger.getLogger().log(Logger.Level.DEBUG, "Tried to remove extension out of bounds");
                }
            });

            useDefaults = new JCheckBox("Use default extensions");
            ActionListener a = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    newExtensionsTextField.setEnabled(!useDefaults.isSelected());
                    addButton.setEnabled(!useDefaults.isSelected());
                    removeButton.setEnabled(!useDefaults.isSelected());
                    currentExtensionsList.setEnabled(!useDefaults.isSelected());
                    if (!useDefaults.isSelected() && newExtensionsTextField.getText().isEmpty()) {
                        newExtensionsTextField.setText(ArchiveManagerOptions.DEFAULTS.stream().collect(Collectors.joining(" ")));
                    }
                }
            };
            useDefaults.addActionListener(a);
            useDefaults.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                    "Default extensions are: " + ArchiveManagerOptions.DEFAULTS.stream().collect(Collectors.joining(", ")));

            // Setup
            if (ArchiveManagerOptions.getInstance().areExtensionsEmpty()) {
                useDefaults.setSelected(true);
            } else {
                defaultListModel.addAll(ArchiveManagerOptions.getInstance().getExtensions());
            }
            a.actionPerformed(null);

            addExtensions(gbc);
            this.setPreferredSize(new Dimension(0, 400));
        }

        void addAgent(GridBagConstraints gbc) {
            gbc.insets = new Insets(20, 20, 0, 0);
            gbc.gridx = 1;
            this.add(this.agentPathLabel, gbc);

            gbc.insets = new Insets(5, 20, 0, 0);
            gbc.weightx = 1;
            gbc.gridx = 1;
            gbc.gridwidth = 2;
            this.add(agentPathTextField, gbc);

            gbc.insets = new Insets(0, 20, 0, 20);
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbc.gridx = 3;
            gbc.gridy = 1;
            this.add(browseButton, gbc);

            gbc.insets = new Insets(20, 20, 0, 0);
            gbc.gridx = 1;
            gbc.gridy = 3;
            this.add(checkBoxSettings, gbc);

            gbc.insets = new Insets(5, 20, 0, 0);
            gbc.gridx = 1;
            gbc.gridy = 4;
            this.add(useHostSystemClassesCheckBox, gbc);
        }

        void addExtensions(GridBagConstraints gbc) {
            gbc.insets = new Insets(30, 20, 0, 0);
            gbc.gridy = 5;
            this.add(nestedJars, gbc);

            gbc.insets = new Insets(10, 20, 0, 0);
            gbc.gridy = 6;
            this.add(useDefaults, gbc);

            gbc.insets = new Insets(5, 20, 0, 20);
            gbc.gridy = 7;
            gbc.weighty = 1.0;
            gbc.gridwidth = 3;
            this.add(scrollPane, gbc);

            gbc.insets = new Insets(5, 20, 0, 0);
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridy = 8;
            gbc.gridwidth = 1;
            this.add(newExtensionsTextField, gbc);

            gbc.insets = new Insets(5, 5, 0, 0);
            gbc.weighty = 0;
            gbc.gridx = 2;
            this.add(addButton, gbc);

            gbc.insets = new Insets(5, 5, 0, 20);  //top padding
            gbc.gridx = 3;
            this.add(removeButton, gbc);
        }

        void confirmExtensions() {
            for (String s : newExtensionsTextField.getText().split("\\s")) {
                if ("".equals(s) || "\\s".equals(s)) {
                    Logger.getLogger().log(Logger.Level.DEBUG, "Empty string when adding extension");
                } else {
                    defaultListModel.addElement(s);
                }
            }
            newExtensionsTextField.setText("");
        }
    }

    public SettingsView(MainFrameView mainFrameView) {
        settingsPanel = new SettingsPanel(config.getAgentRawPath(), config.doUseHostSystemClasses());
        settingsPanel.browseButton.addActionListener(actionEvent -> {
            int dialogResult = settingsPanel.chooser.showOpenDialog(settingsPanel);
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                settingsPanel.agentPathTextField.setText(settingsPanel.chooser.getSelectedFile().getPath());
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            applySettings();
            dispose();
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> dispose());
        cancelButton.setPreferredSize(new Dimension(90, 30));

        okCancelPanel = new JPanel(new GridBagLayout());
        okCancelPanel.setBorder(new EtchedBorder());
        okCancelPanel.setPreferredSize(new Dimension(0, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.weightx = 1;
        okCancelPanel.add(Box.createHorizontalGlue(), gbc);

        gbc.weightx = 0;
        gbc.gridx = 1;
        okCancelPanel.add(okButton, gbc);

        gbc.gridx = 2;
        okCancelPanel.add(Box.createHorizontalStrut(15), gbc);

        gbc.gridx = 3;
        okCancelPanel.add(cancelButton, gbc);

        gbc.gridx = 4;
        okCancelPanel.add(Box.createHorizontalStrut(20), gbc);

        mainPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(settingsPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        mainPanel.add(okCancelPanel, gbc);

        this.setTitle("Settings");
        this.setSize(new Dimension(800, 500));
        this.setMinimumSize(new Dimension(250, 500));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);
        this.setVisible(true);
    }

    private void applySettings() {
        config.setAgentPath(settingsPanel.agentPathTextField.getText());
        config.setUseHostSystemClasses(settingsPanel.useHostSystemClassesCheckBox.isSelected());

        try {
            config.saveConfigFile();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }

        List<String> extensions;
        if (settingsPanel.useDefaults.isSelected()) {
            extensions = new ArrayList<>();
        } else {
            extensions = Collections.list(settingsPanel.defaultListModel.elements());
        }
        ArchiveManagerOptions.getInstance().setExtension(extensions);
    }
}
