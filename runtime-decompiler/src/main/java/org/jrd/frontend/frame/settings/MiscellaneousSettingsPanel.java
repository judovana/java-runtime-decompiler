package org.jrd.frontend.frame.settings;

import org.jrd.backend.data.Config;
import org.jrd.frontend.frame.filesystem.NewFsVmView;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Enumeration;

public class MiscellaneousSettingsPanel extends JPanel implements ChangeReporter {

    private final JLabel miscSettingsLabel;
    private final JCheckBox useJavapSignaturesCheckBox;
    private final JCheckBox detectAutocompletionCheckBox;
    private final JComboBox<Config.DepndenceNumbers> dependenceNumbers;
    private final JTextField srcPath;
    private final JTextField classPath;
    ButtonGroup additionalAgentPlace = new ButtonGroup();
    JRadioButton nothing = new JRadioButton("do nothing");
    JRadioButton add = new JRadioButton("add them to remote vms");
    JRadioButton ask = new JRadioButton("ask");
    JRadioButton addAndSave = new JRadioButton("add them to remote vms and save");

    public MiscellaneousSettingsPanel(
            boolean initialUseJavapSignatures, Config.DepndenceNumbers initialConfigNumbers, String cp, String sp,
            boolean detectAutocompletion, Config.AdditionalAgentAction additionalAgentAction
    ) {
        miscSettingsLabel = new JLabel("Miscellaneous settings");
        this.setName(miscSettingsLabel.getText());
        useJavapSignaturesCheckBox = new JCheckBox("Use Javap signatures in Agent API insertion menu", initialUseJavapSignatures);
        detectAutocompletionCheckBox = new JCheckBox("Detect and enable autocompletion in text editor", detectAutocompletion);
        detectAutocompletionCheckBox.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "for assemblers, the bytecode will be loaded.<br/>" + "For byteman, byteman.<br/>" +
                        "But for java, it depends on editor - in JRD it will runtime modification api"
        );
        dependenceNumbers = new JComboBox(Config.DepndenceNumbers.values());
        srcPath = new JTextField(sp);
        classPath = new JTextField(cp);

        initMainPanel(initialConfigNumbers, additionalAgentAction);
    }

    private void initMainPanel(Config.DepndenceNumbers initialConfigNumbers, Config.AdditionalAgentAction additionalAgentAction) {
        JPanel mainPanel = this;
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 2;
        gbc.weightx = 1; // required or else contents are centered

        mainPanel.add(miscSettingsLabel, gbc);

        gbc.gridy = 1;
        mainPanel.add(useJavapSignaturesCheckBox, gbc);
        gbc.gridy = 2;
        mainPanel.add(detectAutocompletionCheckBox, gbc);
        gbc.gridy = 3;
        mainPanel.add(dependenceNumbers, gbc);
        dependenceNumbers.addActionListener(a -> {
            dependenceNumbers.setToolTipText(
                    BytecodeDecompilerView.styleTooltip() + ((Config.DepndenceNumbers) (dependenceNumbers.getSelectedItem())).description
            );
        });
        dependenceNumbers.setSelectedItem(initialConfigNumbers);
        String hint = BytecodeDecompilerView.styleTooltip() +
                "Additional source-path and classpath is used to show on-fs available src/bytecode of given classes" +
                " and to help with possible compilation issues. <br>";
        gbc.gridy = 4;
        JLabel aspl = new JLabel("Additional source-path");
        aspl.setToolTipText(
                hint + "Although JRD was usually used no FS, it happened, that <b>decompilers</b> sucks," +
                        " and disassemblers have ist issues.<br>" + "So if you have sources, and wish to back-compile," +
                        " it is good habit to have original source available."
        );
        srcPath.setToolTipText(aspl.getToolTipText());
        mainPanel.add(aspl, gbc);
        gbc.gridy = 5;
        mainPanel.add(srcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        JButton selectSrcPath = new JButton("...");
        mainPanel.add(selectSrcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 6;
        JLabel acpl = new JLabel("Additional class-path");
        acpl.setToolTipText(
                hint + "Although JRD was designed to <b>actually fill" + " missing classes</b> on FS from running VM,<br>" +
                        "it proved itself, that soem weird class may still be missing. Thus you can add those classes via cp.<br>" +
                        "Future JRD should be able to also upload any non-existing classes to running VM.<br><br>" +
                        "In addition, this  classpath is used in standalone text editor's code completion"
        );
        classPath.setToolTipText(acpl.getToolTipText());
        mainPanel.add(acpl, gbc);
        gbc.gridy = 7;
        mainPanel.add(classPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        JButton selectClassPath = new JButton("...");
        mainPanel.add(selectClassPath, gbc);
        selectSrcPath.addActionListener(actionEvent -> NewFsVmView.CpNamePanel.selectCp(srcPath, selectSrcPath));
        selectClassPath.addActionListener(actionEvent -> NewFsVmView.CpNamePanel.selectCp(classPath, selectClassPath));
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 8;
        JLabel additionalAgents = new JLabel("Byteman companion/additional agents:");
        mainPanel.add(additionalAgents, gbc);
        nothing.setActionCommand(Config.AdditionalAgentAction.NOTHING.toString());
        add.setActionCommand(Config.AdditionalAgentAction.ADD.toString());
        ask.setActionCommand(Config.AdditionalAgentAction.ASK.toString());
        addAndSave.setActionCommand(Config.AdditionalAgentAction.ADD_AND_SAVE.toString());
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        gbc.gridx = 1;
        gbc.gridy = 9;
        JPanel radioPanel = new JPanel(new FlowLayout());
        radioPanel.add(nothing);
        radioPanel.add(ask);
        radioPanel.add(add);
        radioPanel.add(addAndSave);
        additionalAgentPlace.add(nothing);
        additionalAgentPlace.add(ask);
        additionalAgentPlace.add(add);
        additionalAgentPlace.add(addAndSave);
        ask.setSelected(true);
        Enumeration<AbstractButton> enumeration = additionalAgentPlace.getElements();
        while (enumeration.hasMoreElements()) {
            AbstractButton ab = enumeration.nextElement();
            Config.AdditionalAgentAction current = Config.AdditionalAgentAction.fromString(ab.getActionCommand());
            if (current == additionalAgentAction) {
                ab.setSelected(true);
            } else {
                ab.setSelected(false);
            }
        }
        mainPanel.add(radioPanel, gbc);
    }

    public boolean shouldUseJavapSignatures() {
        return useJavapSignaturesCheckBox.isSelected();
    }

    public boolean shouldAutocomplete() {
        return detectAutocompletionCheckBox.isSelected();
    }

    public Config.DepndenceNumbers futurreDependenciesNumbers() {
        return (Config.DepndenceNumbers) dependenceNumbers.getSelectedItem();
    }

    @Override
    public void setChangeReporter(ActionListener listener) {
        ChangeReporter.addCheckboxListener(listener, useJavapSignaturesCheckBox);
        ChangeReporter.addCheckboxListener(listener, detectAutocompletionCheckBox);
        ChangeReporter.addTextChangeListener(listener, srcPath);
        ChangeReporter.addTextChangeListener(listener, classPath);
        ChangeReporter.addCheckboxListener(listener, add);
        ChangeReporter.addCheckboxListener(listener, addAndSave);
        ChangeReporter.addCheckboxListener(listener, nothing);
        ChangeReporter.addCheckboxListener(listener, ask);
    }

    public String getAdditioalCP() {
        return classPath.getText();
    }

    public String getAdditionalSP() {
        return srcPath.getText();
    }

    public Config.AdditionalAgentAction getAdditionalAgentAction() {
        return Config.AdditionalAgentAction.fromString(additionalAgentPlace.getSelection().getActionCommand());
    }
}
