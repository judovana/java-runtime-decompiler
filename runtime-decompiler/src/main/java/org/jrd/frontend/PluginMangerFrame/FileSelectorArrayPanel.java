package org.jrd.frontend.PluginMangerFrame;

import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class FileSelectorArrayPanel extends JPanel {

    private JLabel jLabel;
    private GridBagConstraints gbc;

    private JButton addButton = new JButton();
    private List<JTextField> pathTextFields = new LinkedList<JTextField>();

    private Boolean first = false;

    FileSelectorArrayPanel(String label) {
        this.jLabel = new JLabel(label);

        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        // Label
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1;
        this.add(jLabel, gbc);

        // gbc.gridy 1-9999 used for FileSelectorArrayRows created with addRow().

        // Button for creating new row
        gbc.gridy = 10000;
        gbc.gridx = 0;
        this.add(addButton, gbc);
        gbc.gridx = 0;
        addButton.addActionListener(actionEvent -> addRow("", true));

        gbc.gridx = 0;
        gbc.gridy = 0;

        // Always have a least one row.
        addRow("", false);
        first = true;
    }

    void addRow(String url, boolean button) {
        // Fill first row created by constructor.
        if (first) {
            if (!button) {
                first = false;
                pathTextFields.get(0).setText(url);
                return;
            }
        }
        gbc.gridy++;
        FileSelectorArrayRow fileSelectorArrayRow = new FileSelectorArrayRow(this, url);
        pathTextFields.add(fileSelectorArrayRow.getTextField());
        this.add(fileSelectorArrayRow, gbc);
        this.revalidate();
    }


    public void removeRow(FileSelectorArrayRow fileSelectorArrayRow) {
        if (pathTextFields.size() > 1) {
            pathTextFields.remove(fileSelectorArrayRow.getTextField());
            this.remove(fileSelectorArrayRow);
            this.revalidate();
        }
    }

    public List<String> getStringList() {
        List<String> list = new LinkedList<>();
        for (JTextField jTextField : pathTextFields) {
            list.add(jTextField.getText());
        }
        return list;
    }
}