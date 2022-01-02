package org.jrd.frontend.frame.settings;

import org.jrd.frontend.frame.main.BytecodeDecompilerView;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class CompilationSettingsPanel extends JPanel implements ChangeReporter {

    private JLabel compilationSettingsLabel;
    private JCheckBox useHostSystemClassesCheckBox;
    private JCheckBox useHostJavaLangObjectCheckBox;
    private JLabel compilerArgsLabel;
    private JTextField compilerArgsTextField;

    public CompilationSettingsPanel(boolean initialUseHostSystemClasses, String initialCompilerArgs, boolean initialUseHostJavaObject) {
        compilationSettingsLabel = new JLabel("Compilation settings");
        useHostSystemClassesCheckBox =
                new JCheckBox("Use host system classes during compilation phase of class overwrite", initialUseHostSystemClasses);
        useHostJavaLangObjectCheckBox =
                new JCheckBox("Always use host class java.lang.Object (e.g. DCEVM requires this to work)", initialUseHostJavaObject);
        useHostSystemClassesCheckBox.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "<b>very tricky switch</b><br>" +
                        "If true, then (should be default) then system classes (like java.lang) are loaded from THIS jvm<br>" +
                        "If false, then all classes are onl from remote vm. Where this is more correct, it slower and may have issues<br>" +
                        "Note, that even true, may bring some unexpected behavior, and is hard to determine what is better." +
                        " With false on FS, you have to provide also system classes to cp!"
        );
        compilerArgsLabel = new JLabel("Compiler arguments");
        compilerArgsTextField = new JTextField(initialCompilerArgs);
        compilerArgsTextField.setToolTipText(
                BytecodeDecompilerView.styleTooltip() +
                        "Arguments that get passed to the compiler, eg. '-source 5 -target 8 -release 9 -Xlint'."
        );
        compilerArgsLabel.setToolTipText(compilerArgsTextField.getToolTipText());

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
        gbc.gridy = 2;
        this.add(useHostJavaLangObjectCheckBox, gbc);

        gbc.insets = new Insets(5, 5 + useHostSystemClassesCheckBox.getInsets().left, 5, 5);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridy = 3;
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

    public boolean shouldUseHostJavaLangObjectCheckBox() {
        return useHostJavaLangObjectCheckBox.isSelected();
    }

    public String getCompilerArgs() {
        return compilerArgsTextField.getText();
    }

    @Override
    public void setChangeReporter(ActionListener listener) {
        ChangeReporter.addCheckboxListener(listener, useHostSystemClassesCheckBox);
        ChangeReporter.addCheckboxListener(listener, useHostJavaLangObjectCheckBox);
        ChangeReporter.addTextChangeListener(listener, compilerArgsTextField);
    }
}
