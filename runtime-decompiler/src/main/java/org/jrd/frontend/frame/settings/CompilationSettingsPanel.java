package org.jrd.frontend.frame.settings;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class CompilationSettingsPanel extends JPanel {

    private JLabel compilationSettingsLabel;
    private JCheckBox useHostSystemClassesCheckBox;
    private JLabel compilerArgsLabel;
    private JTextField compilerArgsTextField;

    public CompilationSettingsPanel(boolean initialUseHostSystemClasses, String initialCompilerArgs) {
        compilationSettingsLabel = new JLabel("Compilation settings");
        useHostSystemClassesCheckBox =
            new JCheckBox("Use host system classes during compilation phase of class overwrite", initialUseHostSystemClasses);
        compilerArgsLabel = new JLabel("Compiler arguments");
        compilerArgsTextField = new JTextField(initialCompilerArgs);
        compilerArgsTextField.setToolTipText("Arguments that get passed to the compiler, eg. '-source 5 -target 8 -release 9 -Xlint'.");

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 2;
        gbc.weightx = 1; // required or else contents are centered

        this.add(compilationSettingsLabel, gbc);

        gbc.gridy = 1;
        this.add(useHostSystemClassesCheckBox, gbc);

        gbc.insets = new Insets(5, 5 + useHostSystemClassesCheckBox.getInsets().left, 5, 5);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridy = 2;
        this.add(compilerArgsLabel, gbc);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 1;
        this.add(compilerArgsTextField, gbc);
    }

    public boolean shouldUseHostSystemClassesCheckBox() {
        return useHostSystemClassesCheckBox.isSelected();
    }

    public String getCompilerArgs() {
        return compilerArgsTextField.getText();
    }
}
