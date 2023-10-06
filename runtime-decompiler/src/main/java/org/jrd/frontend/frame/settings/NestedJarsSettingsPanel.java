package org.jrd.frontend.frame.settings;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.ArchiveManagerOptions;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;

public class NestedJarsSettingsPanel extends JPanel implements ChangeReporter {
    private JCheckBox useDefaults;
    private JTextField newExtensionsTextField;
    private JLabel nestedJars;
    private JButton addButton;
    private JButton removeButton;
    private DefaultListModel<String> uniqueListModel;
    private JList<String> currentExtensionsList;
    private JScrollPane scrollPane;

    NestedJarsSettingsPanel() {
        this.setLayout(new GridBagLayout());

        nestedJars = new JLabel("Nested Jars Settings:");
        this.setName(nestedJars.getText());
        newExtensionsTextField = new JTextField();
        newExtensionsTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmExtensions();
                }
            }
        });

        uniqueListModel = new DefaultListModel<String>() {
            @Override
            public void addAll(Collection<? extends String> c) {
                Set<String> filter = new LinkedHashSet<>();

                filter.addAll(Collections.list(this.elements()));
                filter.addAll(c);

                super.clear();
                super.addAll(filter);
            }
        };
        currentExtensionsList = new JList<>(uniqueListModel);
        currentExtensionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentExtensionsList.setLayoutOrientation(JList.VERTICAL);
        currentExtensionsList.setVisibleRowCount(-1);

        scrollPane = new JScrollPane(currentExtensionsList);

        addButton = new JButton("Add");
        addButton.addActionListener(actionEvent -> confirmExtensions());

        removeButton = new JButton("Remove");
        removeButton.addActionListener(actionEvent -> {
            try {
                uniqueListModel.removeElementAt(currentExtensionsList.getSelectedIndex());
            } catch (ArrayIndexOutOfBoundsException e) {
                Logger.getLogger().log(Logger.Level.DEBUG, "Tried to remove extension out of bounds");
            }
        });

        useDefaults = new JCheckBox("Use default extensions for nested jars");
        ActionListener a = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                newExtensionsTextField.setEnabled(!useDefaults.isSelected());
                addButton.setEnabled(!useDefaults.isSelected());
                removeButton.setEnabled(!useDefaults.isSelected());
                currentExtensionsList.setEnabled(!useDefaults.isSelected());
                if (!useDefaults.isSelected() && newExtensionsTextField.getText().isEmpty() && actionEvent != null) {
                    newExtensionsTextField.setText(ArchiveManagerOptions.getExtensionString(" "));
                }
            }
        };
        useDefaults.addActionListener(a);
        useDefaults.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "Default extensions are: " + ArchiveManagerOptions.getExtensionString(", ")
        );

        // Setup
        if (ArchiveManagerOptions.getInstance().areExtensionsEmpty()) {
            useDefaults.setSelected(true);
        } else {
            uniqueListModel.addAll(ArchiveManagerOptions.getInstance().getExtensions());
        }
        a.actionPerformed(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        this.add(nestedJars, gbc);

        gbc.gridy = 1;
        this.add(useDefaults, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.gridwidth = 3;
        this.add(scrollPane, gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        this.add(newExtensionsTextField, gbc);

        Dimension fixedSize = BytecodeDecompilerView.buttonSizeBasedOnTextField(removeButton, newExtensionsTextField);
        addButton.setPreferredSize(fixedSize);
        removeButton.setPreferredSize(fixedSize);

        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(addButton, gbc);

        gbc.gridx = 2;
        this.add(removeButton, gbc);

        this.setPreferredSize(new Dimension(0, 400));
    }

    void confirmExtensions() {
        uniqueListModel
                .addAll(Arrays.stream(newExtensionsTextField.getText().split("\\s")).filter(s -> !s.isBlank()).collect(Collectors.toSet()));

        newExtensionsTextField.setText("");
    }

    public List<String> getExtensions() {
        return Collections.list(uniqueListModel.elements());
    }

    public boolean shouldUseDefaultExtensions() {
        return useDefaults.isSelected();
    }

    @Override
    public void setChangeReporter(ActionListener listener) {
        ChangeReporter.addCheckboxListener(listener, useDefaults);
        ChangeReporter.addJListListener(listener, currentExtensionsList);
    }
}
