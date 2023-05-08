package org.jrd.frontend.frame.settings;

import org.jrd.backend.data.Config;
import org.jrd.frontend.frame.filesystem.NewFsVmView;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class MiscellaneousSettingsPanel extends JPanel implements ChangeReporter {

    private final JLabel miscSettingsLabel;
    private final JCheckBox useJavapSignaturesCheckBox;
    private final JCheckBox detectAutocompletionCheckBox;
    private final JComboBox<Config.DepndenceNumbers> dependenceNumbers;
    private final JTextField srcPath;
    private final JTextField classPath;

    public MiscellaneousSettingsPanel(
            boolean initialUseJavapSignatures, Config.DepndenceNumbers initialConfigNumbers, String cp, String sp, boolean detectAutocompletion
    ) {
        miscSettingsLabel = new JLabel("Miscellaneous settings");
        useJavapSignaturesCheckBox = new JCheckBox("Use Javap signatures in Agent API insertion menu", initialUseJavapSignatures);
        detectAutocompletionCheckBox = new JCheckBox("Detect and enable autocompletion in text editor", detectAutocompletion);
        detectAutocompletionCheckBox.setToolTipText(
                BytecodeDecompilerView.styleTooltip() +
                        "for assemblers, the bytecode will be loaded.<br/> For byteman, byteman.<br/> But for java, it depends on editor - in JRD it will runtime modification api");
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 2;
        gbc.weightx = 1; // required or else contents are centered

        this.add(miscSettingsLabel, gbc);

        gbc.gridy = 1;
        this.add(useJavapSignaturesCheckBox, gbc);
        gbc.gridy = 2;
        this.add(detectAutocompletionCheckBox, gbc);
        dependenceNumbers = new JComboBox(Config.DepndenceNumbers.values());
        gbc.gridy = 3;
        this.add(dependenceNumbers, gbc);
        dependenceNumbers.addActionListener(a -> {
            dependenceNumbers.setToolTipText(
                    BytecodeDecompilerView.styleTooltip() + ((Config.DepndenceNumbers) (dependenceNumbers.getSelectedItem())).description
            );
        });
        dependenceNumbers.setSelectedItem(initialConfigNumbers);

        srcPath = new JTextField(sp);
        classPath = new JTextField(cp);

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
        this.add(aspl, gbc);
        gbc.gridy = 5;
        this.add(srcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        JButton selectSrcPath = new JButton("...");
        this.add(selectSrcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 6;
        JLabel acpl = new JLabel("Additional class-path");
        acpl.setToolTipText(
                hint + "Although JRD was designed to <b>actually fill" + " missing classes</b> on FS from running VM,<br>" +
                        "it proved itself, that soem weird class may still be missing. Thus you can add those classes via cp.<br>" +
                        "Future JRD should be able to also upload any non-existing classes to running VM."
        );
        classPath.setToolTipText(acpl.getToolTipText());
        this.add(acpl, gbc);
        gbc.gridy = 7;
        this.add(classPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        JButton selectClassPath = new JButton("...");
        this.add(selectClassPath, gbc);

        selectSrcPath.addActionListener(actionEvent -> NewFsVmView.CpNamePanel.selectCp(srcPath, selectSrcPath));
        selectClassPath.addActionListener(actionEvent -> NewFsVmView.CpNamePanel.selectCp(classPath, selectClassPath));

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
    }

    public String getAdditioalCP() {
        return classPath.getText();
    }

    public String getAdditionalSP() {
        return srcPath.getText();
    }
}
