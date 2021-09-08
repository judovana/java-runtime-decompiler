package org.jrd.frontend.frame.agent;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.Directories;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;
import org.jrd.frontend.frame.main.MainFrameView;
import org.jrd.frontend.frame.plugins.FileSelectorArrayRow;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("Indentation") // indented Swing components greatly help with orientation
public class ConfigureView extends JDialog {

    private JPanel mainPanel;
        private ConfigurePanel configurePanel;
        private JPanel okCancelPanel;
            private JButton okButton;
            private JButton cancelButton;

    private final Config config = Config.getConfig();

    public static class ConfigurePanel extends JPanel {

        public JTextField agentPathTextField;
        public JLabel agentPathLabel;
        public JButton browseButton;
        public JLabel checkBoxSettings;
        public JCheckBox useHostSystemClassesCheckBox;

        public JFileChooser chooser;

        ConfigurePanel(String initialAgentPath, boolean initialUseHostSystemClasses) {

            this.agentPathTextField = new JTextField();
            this.agentPathTextField.setToolTipText(BytecodeDecompilerView.styleTooltip() + "Select a path to the Decompiler Agent.<br />" +
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
                dir = new File(Directories.getJrdLocation() + File.separator + "decompiler_agent" + File.separator + "target");
            }
            chooser.setCurrentDirectory(FileSelectorArrayRow.fallback(dir));

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;

            gbc.gridx = 1;
            this.add(this.agentPathLabel, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            this.add(Box.createHorizontalStrut(20), gbc);

            gbc.weightx = 1;
            gbc.gridx = 1;
            this.add(agentPathTextField, gbc);

            gbc.weightx = 0;
            gbc.gridx = 2;
            this.add(Box.createHorizontalStrut(20), gbc);

            gbc.gridx = 3;
            this.add(browseButton, gbc);

            gbc.gridx = 4;
            this.add(Box.createHorizontalStrut(20), gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            this.add(Box.createVerticalStrut(20), gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            this.add(checkBoxSettings, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            this.add(Box.createHorizontalStrut(20), gbc);

            gbc.gridx = 1;
            this.add(useHostSystemClassesCheckBox, gbc);

            this.setPreferredSize(new Dimension(0, 150));
        }
    }

    public ConfigureView(MainFrameView mainFrameView) {
        configurePanel = new ConfigurePanel(config.getAgentRawPath(), config.doUseHostSystemClasses());
        configurePanel.browseButton.addActionListener(actionEvent -> {
            int dialogResult = configurePanel.chooser.showOpenDialog(configurePanel);
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                configurePanel.agentPathTextField.setText(configurePanel.chooser.getSelectedFile().getPath());
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            config.setAgentPath(configurePanel.agentPathTextField.getText());
            config.setUseHostSystemClasses(configurePanel.useHostSystemClassesCheckBox.isSelected());

            try {
                config.saveConfigFile();
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
            dispose();
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> {
            dispose();
        });
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
        mainPanel.add(configurePanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        mainPanel.add(okCancelPanel, gbc);

        this.setTitle("Configure Decompiler Agent");
        this.setSize(new Dimension(800, 400));
        this.setMinimumSize(new Dimension(250, 330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);
        this.setVisible(true);
    }
}
