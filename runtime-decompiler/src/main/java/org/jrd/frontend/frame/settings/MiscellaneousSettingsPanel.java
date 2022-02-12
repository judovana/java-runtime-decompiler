package org.jrd.frontend.frame.settings;

import org.jrd.backend.data.Config;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class MiscellaneousSettingsPanel extends JPanel implements ChangeReporter {

    private JLabel miscSettingsLabel;
    private JCheckBox useJavapSignaturesCheckBox;
    private JComboBox<Config.DepndenceNumbers> dependenceNumbers;

    public MiscellaneousSettingsPanel(boolean initialUseJavapSignatures, Config.DepndenceNumbers initialConfigNumbers) {
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
    }
}
