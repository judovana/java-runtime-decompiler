package org.jrd.frontend.frame.settings;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.ArchiveManagerOptions;
import org.jrd.backend.data.Config;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsView extends JDialog {

    private final JPanel mainPanel;
    private final AgentSettingsPanel agentSettingsPanel;
    private final CompilationSettingsPanel compilationSettingsPanel;
    private final NestedJarsSettingsPanel nestedJarsSettingsPanel;
    private final MiscellaneousSettingsPanel miscSettingsPanel;
    private final JPanel okCancelPanel;

    private boolean isChanged = false;

    private final Config config = Config.getConfig();

    public SettingsView(JFrame mainFrameView) {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            applySettings();
            dispose();
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(90, 30));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (isChanged) {
                    int confirmationResult = JOptionPane.showConfirmDialog(
                            SettingsView.this, "You have unsaved changes. Do you wish to discard them and leave?", "Unsaved settings",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
                    );

                    if (confirmationResult == JOptionPane.NO_OPTION) {
                        return;
                    }
                }

                SettingsView.this.dispose();
            }
        });

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
        compilationSettingsPanel = new CompilationSettingsPanel(
                config.doUseHostSystemClasses(), config.getCompilerArgsString(), config.doUseHostJavaLangObject(), config.doOverwriteST()
        );
        nestedJarsSettingsPanel = new NestedJarsSettingsPanel();
        miscSettingsPanel = new MiscellaneousSettingsPanel(
                config.doUseJavapSignatures(), config.doDepndenceNumbers(), config.getAdditionalCP(), config.getAdditionalSP(),
                config.doAutocompletion()
        );

        for (
            ChangeReporter panel : new ChangeReporter[]{agentSettingsPanel, compilationSettingsPanel, nestedJarsSettingsPanel,
                    miscSettingsPanel}
        ) {
            panel.setChangeReporter(e -> isChanged = true);
        }

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(0, 15, 0, 15));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1;

        JTextField thisSettings = new JTextField(Config.getConfig().getConfFile().getAbsolutePath());
        thisSettings.setEditable(false);
        thisSettings.setFont(new Font("Monospaced", thisSettings.getFont().getStyle(), (thisSettings.getFont().getSize() * 2) / 3));
        mainPanel.add(thisSettings, gbc);

        gbc.gridy = 1;
        mainPanel.add(agentSettingsPanel, gbc);

        gbc.gridy = 2;
        mainPanel.add(compilationSettingsPanel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1;
        mainPanel.add(nestedJarsSettingsPanel, gbc);

        gbc.gridy = 4;
        gbc.weighty = 0;
        mainPanel.add(miscSettingsPanel, gbc);

        gbc.gridy = 5;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        gbc.gridy = 6;
        mainPanel.add(okCancelPanel, gbc);

        this.setTitle("Settings");
        this.setSize(new Dimension(800, 850));
        this.setMinimumSize(new Dimension(250, 600));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);
        //this.pack(); this would kill the layout due to nested jars suffxes big box
        this.setVisible(true);
    }

    private void applySettings() {
        List<String> extensions;
        if (nestedJarsSettingsPanel.shouldUseDefaultExtensions()) {
            extensions = new ArrayList<>();
        } else {
            extensions = nestedJarsSettingsPanel.getExtensions();
        }
        ArchiveManagerOptions.getInstance().setExtensions(extensions);

        config.setAgentPath(agentSettingsPanel.getAgentPath());
        config.setUseHostSystemClasses(compilationSettingsPanel.shouldUseHostSystemClassesCheckBox());
        config.setUseHostJavaLangObject(compilationSettingsPanel.shouldUseHostJavaLangObjectCheckBox());
        config.setOverwriteST(compilationSettingsPanel.shouldOverwriteStCheckBox());
        config.setCompilerArguments(compilationSettingsPanel.getCompilerArgs());
        config.setUseJavapSignatures(miscSettingsPanel.shouldUseJavapSignatures());
        config.setAutocomplete(miscSettingsPanel.shouldAutocomplete());
        config.setDepndenceNumbers(miscSettingsPanel.futurreDependenciesNumbers());
        config.setNestedJarExtensions(extensions);
        config.setAdditionalCP(miscSettingsPanel.getAdditioalCP());
        config.setAdditionalSP(miscSettingsPanel.getAdditionalSP());

        try {
            config.saveConfigFile();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
    }
}
