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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsView extends JDialog {

    private final JPanel mainPanelWrapper;
    private final JTabbedPane mainPanel;
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
        GridBagConstraints okGbc = new GridBagConstraints();
        okGbc.anchor = GridBagConstraints.EAST;
        okGbc.fill = GridBagConstraints.BOTH;
        okGbc.gridy = 0;
        okGbc.weightx = 1;
        okCancelPanel.add(Box.createHorizontalGlue(), okGbc);
        okGbc.weightx = 0;
        okGbc.gridx = 1;
        okCancelPanel.add(okButton, okGbc);
        okGbc.gridx = 2;
        okCancelPanel.add(Box.createHorizontalStrut(15), okGbc);
        okGbc.gridx = 3;
        okCancelPanel.add(cancelButton, okGbc);

        agentSettingsPanel = new AgentSettingsPanel(config.getAgentRawPath());
        compilationSettingsPanel = new CompilationSettingsPanel(
                config.doUseHostSystemClasses(), config.getCompilerArgsString(), config.doUseHostJavaLangObject(), config.doOverwriteST()
        );
        nestedJarsSettingsPanel = new NestedJarsSettingsPanel();
        nestedJarsSettingsPanel.setMinimumSize(new Dimension(200, 200));
        miscSettingsPanel = new MiscellaneousSettingsPanel(
                config.doUseJavapSignatures(), config.doDepndenceNumbers(), config.getAdditionalCP(), config.getAdditionalSP(),
                config.doAutocompletion(), config.getAdditionalAgentAction(), (int) (config.getFontSizeOverride()), mainFrameView
        );

        for (
            ChangeReporter panel : new ChangeReporter[]{agentSettingsPanel, compilationSettingsPanel, nestedJarsSettingsPanel,
                    miscSettingsPanel}
        ) {
            panel.setChangeReporter(e -> isChanged = true);
        }

        mainPanelWrapper = new JPanel(new BorderLayout());
        mainPanelWrapper.setBorder(new EmptyBorder(0, 15, 0, 15));
        mainPanel = new JTabbedPane();

        JTextField thisSettings = new JTextField(Config.getConfig().getConfFile().getAbsolutePath());
        thisSettings.setEditable(false);
        thisSettings.setFont(new Font("Monospaced", thisSettings.getFont().getStyle(), (thisSettings.getFont().getSize() * 2) / 3));
        mainPanelWrapper.add(thisSettings, BorderLayout.NORTH);
        mainPanel.add(agentSettingsPanel);
        mainPanel.add(compilationSettingsPanel);
        mainPanel.add(nestedJarsSettingsPanel);
        mainPanel.add(miscSettingsPanel);
        mainPanelWrapper.add(mainPanel);
        mainPanelWrapper.add(okCancelPanel, BorderLayout.SOUTH);

        this.setTitle("Settings");
        this.setSize(new Dimension(800, 600));
        this.setMinimumSize(new Dimension(250, 600));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanelWrapper);
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
        config.setAdditionalAgentAction(miscSettingsPanel.getAdditionalAgentAction());
        config.setFontSizeOverride(miscSettingsPanel.getFontSizeOverride());
        try {
            config.saveConfigFile();
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
        config.setFonts();
    }
}
