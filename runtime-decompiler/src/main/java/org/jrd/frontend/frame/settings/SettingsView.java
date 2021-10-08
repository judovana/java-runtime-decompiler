package org.jrd.frontend.frame.settings;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.ArchiveManagerOptions;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.Directories;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;
import org.jrd.frontend.frame.main.MainFrameView;
import org.jrd.frontend.frame.plugins.FileSelectorArrayRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsView extends JDialog {

    private JPanel mainPanel;
    private AgentSettingsPanel agentSettingsPanel;
    private CompilationSettingsPanel compilationSettingsPanel;
    private NestedJarsSettingsPanel nestedJarsSettingsPanel;
    private JPanel okCancelPanel;

    private final Config config = Config.getConfig();

    public static class AgentSettingsPanel extends JPanel {

        private JTextField agentPathTextField;
        private JLabel agentPathLabel;
        private JButton browseButton;
        private JFileChooser chooser;

        AgentSettingsPanel(String initialAgentPath) {
            agentPathTextField = new JTextField();
            agentPathTextField.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                    "Select a path to the Decompiler Agent.<br />" +
                    FileSelectorArrayRow.getTextFieldToolTip()
            );
            agentPathTextField.setText(initialAgentPath);

            agentPathLabel = new JLabel("Decompiler Agent path");
            browseButton = new JButton("Browse");

            chooser = new JFileChooser();
            File dir;
            if (Directories.isPortable()) {
                dir = new File(Directories.getJrdLocation() + File.separator + "libs");
            } else {
                dir = new File(Directories.getJrdLocation() + File.separator +
                        "decompiler_agent" + File.separator + "target");
            }
            chooser.setCurrentDirectory(FileSelectorArrayRow.fallback(dir));

            browseButton.addActionListener(actionEvent -> {
                int dialogResult = chooser.showOpenDialog(null);
                if (dialogResult == JFileChooser.APPROVE_OPTION) {
                    agentPathTextField.setText(chooser.getSelectedFile().getPath());
                }
            });

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(5, 5, 5, 5);

            this.add(this.agentPathLabel, gbc);

            gbc.weightx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            this.add(agentPathTextField, gbc);

            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbc.gridx = 2;
            browseButton.setPreferredSize(BytecodeDecompilerView.buttonSizeBasedOnTextField(browseButton, agentPathTextField));
            this.add(browseButton, gbc);
        }

        public String getAgentPath() {
            return agentPathTextField.getText();
        }
    }

    public static class CompilationSettingsPanel extends JPanel {

        private JLabel compilationSettingsLabel;
        private JCheckBox useHostSystemClassesCheckBox;

        public CompilationSettingsPanel(boolean initialUseHostSystemClasses) {
            compilationSettingsLabel = new JLabel("Compilation settings");
            useHostSystemClassesCheckBox = new JCheckBox(
                    "Use host system classes during compilation phase of class overwrite",
                    initialUseHostSystemClasses
            );

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.weightx = 1; // required or else contents are centered

            this.add(compilationSettingsLabel, gbc);

            gbc.gridy = 1;
            this.add(useHostSystemClassesCheckBox, gbc);
        }

        public boolean shouldUseHostSystemClassesCheckBox() {
            return useHostSystemClassesCheckBox.isSelected();
        }
    }

    public static class NestedJarsSettingsPanel extends JPanel {
        private JCheckBox useDefaults;
        private JTextField newExtensionsTextField;
        private JLabel nestedJars;
        private JButton addButton;
        private JButton removeButton;
        private DefaultListModel<String> uniqueListModel;
        private JList<String> currentExtensionsList;
        private JScrollPane scrollPane;

        NestedJarsSettingsPanel() {
            this.setLayout(new GridBagLayout());

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

            uniqueListModel = new DefaultListModel<>() {
                @Override
                public void addAll(Collection<? extends String> c) {
                    Set<String> filter = new LinkedHashSet<>();

                    filter.addAll(Collections.list(this.elements()));
                    filter.addAll(c);

                    super.clear();
                    super.addAll(filter);
                }
            };
            currentExtensionsList = new JList<>(uniqueListModel);
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
                    uniqueListModel.removeElementAt(currentExtensionsList.getSelectedIndex());
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
                    if (!useDefaults.isSelected() && newExtensionsTextField.getText().isEmpty() && actionEvent != null) {
                        newExtensionsTextField.setText(ArchiveManagerOptions.getExtensionString(" "));
                    }
                }
            };
            useDefaults.addActionListener(a);
            useDefaults.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                    "Default extensions are: " + ArchiveManagerOptions.getExtensionString(", "));

            // Setup
            if (ArchiveManagerOptions.getInstance().areExtensionsEmpty()) {
                useDefaults.setSelected(true);
            } else {
                uniqueListModel.addAll(ArchiveManagerOptions.getInstance().getExtensions());
            }
            a.actionPerformed(null);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(5, 5, 5, 5);

            this.add(nestedJars, gbc);

            gbc.gridy = 1;
            this.add(useDefaults, gbc);

            gbc.gridy = 2;
            gbc.weighty = 1;
            gbc.gridwidth = 3;
            this.add(scrollPane, gbc);

            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 1;
            this.add(newExtensionsTextField, gbc);

            Dimension fixedSize = BytecodeDecompilerView.buttonSizeBasedOnTextField(removeButton, newExtensionsTextField);
            addButton.setPreferredSize(fixedSize);
            removeButton.setPreferredSize(fixedSize);

            gbc.weightx = 0;
            gbc.gridx = 1;
            this.add(addButton, gbc);

            gbc.gridx = 2;
            this.add(removeButton, gbc);

            this.setPreferredSize(new Dimension(0, 400));
        }

        void confirmExtensions() {
            uniqueListModel.addAll(
                        Arrays.stream(newExtensionsTextField.getText().split("\\s"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toSet())
            );

            newExtensionsTextField.setText("");
        }

        public List<String> getExtensions() {
            return Collections.list(uniqueListModel.elements());
        }

        public boolean shouldUseDefaultExtensions() {
            return useDefaults.isSelected();
        }
    }

    public SettingsView(MainFrameView mainFrameView) {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            applySettings();
            dispose();
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> dispose());
        cancelButton.setPreferredSize(new Dimension(90, 30));

        okCancelPanel = new JPanel(new GridBagLayout());
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

        agentSettingsPanel = new AgentSettingsPanel(config.getAgentRawPath());
        compilationSettingsPanel = new CompilationSettingsPanel(config.doUseHostSystemClasses());
        nestedJarsSettingsPanel = new NestedJarsSettingsPanel();

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(0, 15, 0, 15));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1;

        mainPanel.add(agentSettingsPanel, gbc);

        gbc.gridy = 1;
        mainPanel.add(compilationSettingsPanel, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        mainPanel.add(nestedJarsSettingsPanel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 0;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        gbc.gridy = 4;
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
        config.setAgentPath(agentSettingsPanel.getAgentPath());
        config.setUseHostSystemClasses(compilationSettingsPanel.shouldUseHostSystemClassesCheckBox());

        try {
            config.saveConfigFile();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }

        List<String> extensions;
        if (nestedJarsSettingsPanel.shouldUseDefaultExtensions()) {
            extensions = new ArrayList<>();
        } else {
            extensions = nestedJarsSettingsPanel.getExtensions();
        }
        ArchiveManagerOptions.getInstance().setExtensions(extensions);
    }
}
