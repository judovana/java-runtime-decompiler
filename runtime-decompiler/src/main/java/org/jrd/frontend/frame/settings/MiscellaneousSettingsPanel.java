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
    private final JComboBox<Config.DepndenceNumbers> dependenceNumbers;
    private final JTextField srcPath;
    private final JTextField classPath;

    public MiscellaneousSettingsPanel(
            boolean initialUseJavapSignatures, Config.DepndenceNumbers initialConfigNumbers, String cp, String sp
    ) {
        miscSettingsLabel = new JLabel("Miscellaneous settings");
        useJavapSignaturesCheckBox = new JCheckBox("Use Javap signatures in Agent API insertion menu", initialUseJavapSignatures);

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
        dependenceNumbers = new JComboBox(Config.DepndenceNumbers.values());
        gbc.gridy = 2;
        this.add(dependenceNumbers, gbc);
        dependenceNumbers.addActionListener(a -> {
            dependenceNumbers.setToolTipText(
                    BytecodeDecompilerView.styleTooltip() + ((Config.DepndenceNumbers) (dependenceNumbers.getSelectedItem())).description
            );
        });
        dependenceNumbers.setSelectedItem(initialConfigNumbers);

        srcPath = new JTextField(sp);
        classPath = new JTextField(cp);

        gbc.gridy = 3;
        this.add(new JLabel("Additional source-path"), gbc);
        gbc.gridy = 4;
        this.add(srcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        JButton selectSrcPath = new JButton("...");
        this.add(selectSrcPath, gbc);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 5;
        this.add(new JLabel("Additional class-path"), gbc);
        gbc.gridy = 6;
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

    public Config.DepndenceNumbers futurreDependenciesNumbers() {
        return (Config.DepndenceNumbers) dependenceNumbers.getSelectedItem();
    }

    @Override
    public void setChangeReporter(ActionListener listener) {
        ChangeReporter.addCheckboxListener(listener, useJavapSignaturesCheckBox);
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
