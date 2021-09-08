package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class FileSelectorArrayPanel extends JPanel {

    private final JLabel jLabel;
    private final FileSelectorArrayAddRow fileSelectorArrayAddRow;

    private final GridBagConstraints gbc;
    private List<JTextField> pathTextFields;

    private Boolean first = false;

    FileSelectorArrayPanel(String label) {
        gbc = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        pathTextFields = new LinkedList<>();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        jLabel = new JLabel(label);
        fileSelectorArrayAddRow = new FileSelectorArrayAddRow();
        fileSelectorArrayAddRow.getAddButton().addActionListener(actionEvent -> {
            addRow("", true);
        });

        gbc.weightx = 1;
        this.add(jLabel, gbc);
        gbc.gridy = 10000;
        this.add(fileSelectorArrayAddRow, gbc);
        gbc.gridy = 0;

        // Always have at least one row.
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
