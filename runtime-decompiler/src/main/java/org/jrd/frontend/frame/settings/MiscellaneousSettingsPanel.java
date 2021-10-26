package org.jrd.frontend.frame.settings;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class MiscellaneousSettingsPanel extends JPanel {

    private JLabel miscSettingsLabel;
    private JCheckBox useJavapSignaturesCheckBox;

    public MiscellaneousSettingsPanel(boolean initialUseJavapSignatures) {
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
    }

    public boolean shouldUseJavapSignatures() {
        return useJavapSignaturesCheckBox.isSelected();
    }
}
