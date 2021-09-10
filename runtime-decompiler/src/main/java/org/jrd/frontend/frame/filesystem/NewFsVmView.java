package org.jrd.frontend.frame.filesystem;

import org.jrd.frontend.frame.main.MainFrameView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;

public class NewFsVmView extends JDialog {

    private static String lastOpened = System.getProperty("user.home");

    private JPanel mainPanel;
    private CpNamePanel mCpNamePanel;
    private JPanel okCancelPanel;
    private JPanel configureOkCancelPanel;
    private JButton okButton;
    private JButton cancelButton;

    private ActionListener addButtonListener;

    public static class CpNamePanel extends JPanel {
        JTextField cpTextField;
        JTextField nameTextField;
        JButton selectCpButton;
        JPanel textAndName;
        JCheckBox keepFsVmCheckbox;

        int padding = 10;

        CpNamePanel() {
            this.selectCpButton = new JButton("...");
            this.selectCpButton.setPreferredSize(new Dimension(30, 30));
            this.cpTextField = new JTextField();
            this.nameTextField = new JTextField();
            this.nameTextField.setPreferredSize(new Dimension(90, 0));
            this.textAndName = new JPanel();
            this.keepFsVmCheckbox = new JCheckBox("Keep this FS VM saved after closing JRD");

            textAndName.setLayout(new GridBagLayout());
            textAndName.setBorder(new EmptyBorder(padding, padding, padding, padding));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 2;
            textAndName.add(new JLabel("Classpath"), gbc);
            gbc.gridx = 4;
            textAndName.add(new JLabel("Optional name"), gbc);
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.weighty = 0;
            textAndName.add(selectCpButton, gbc);
            gbc.gridx = 1;
            textAndName.add(Box.createHorizontalStrut(padding), gbc);
            gbc.weightx = 1;
            gbc.gridx = 2;
            textAndName.add(cpTextField, gbc);
            gbc.weightx = 0;
            gbc.gridx = 3;
            textAndName.add(Box.createHorizontalStrut(padding), gbc);
            gbc.gridx = 4;
            textAndName.add(nameTextField, gbc);
            gbc.gridy = 2;
            gbc.gridx = 0;
            textAndName.add(Box.createVerticalStrut(padding), gbc);
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            textAndName.add(keepFsVmCheckbox, gbc);

            textAndName.setPreferredSize(new Dimension(0, 125));

            this.setLayout(new BorderLayout());
            this.add(textAndName, BorderLayout.CENTER);
            selectCpButton.addActionListener(actionEvent -> selectCp());
        }

        private void selectCp() {
            JFileChooser jf = new JFileChooser(lastOpened);
            jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            int returnVal = jf.showOpenDialog(selectCpButton);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (jf.getSelectedFile().isDirectory()) {
                    lastOpened = jf.getSelectedFile().getAbsolutePath();
                } else {
                    lastOpened = jf.getSelectedFile().getParentFile().getAbsolutePath();
                }

                String lcp = jf.getSelectedFile().getAbsolutePath();
                if (cpTextField.getText().trim().isEmpty()) {
                    cpTextField.setText(lcp);
                } else {
                    cpTextField.setText(cpTextField.getText() + File.pathSeparator + lcp);
                }
            }
        }
    }

    public NewFsVmView(MainFrameView mainFrameView) {
        mCpNamePanel = new CpNamePanel();

        okButton = new JButton("Add");
        okButton.addActionListener(actionEvent -> {
            addButtonListener.actionPerformed(actionEvent);
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> dispose());
        cancelButton.setPreferredSize(new Dimension(90, 30));

        okCancelPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridy = 0;
        gbc.weightx = 1;
        okCancelPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        okCancelPanel.add(okButton, gbc);
        gbc.gridx = 2;
        okCancelPanel.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 3;
        okCancelPanel.add(cancelButton, gbc);
        gbc.gridx = 4;
        okCancelPanel.add(Box.createHorizontalStrut(20), gbc);

        configureOkCancelPanel = new JPanel(new GridBagLayout());
        configureOkCancelPanel.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        configureOkCancelPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.gridx = 1;
        configureOkCancelPanel.add(okCancelPanel, gbc);
        configureOkCancelPanel.setPreferredSize(new Dimension(0, 60));

        mainPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(mCpNamePanel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 1;
        mainPanel.add(Box.createVerticalGlue(), gbc);
        gbc.gridy = 2;
        gbc.weighty = 0;
        mainPanel.add(configureOkCancelPanel, gbc);

        this.setTitle("New filesystem VM");
        this.setSize(new Dimension(400, 220));
        this.setMinimumSize(new Dimension(250, 220));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);
    }

    String getCp() {
        return mCpNamePanel.cpTextField.getText();
    }

    String getNameHelper() throws NumberFormatException {
        return mCpNamePanel.nameTextField.getText();
    }

    boolean shouldBeSaved() {
        return mCpNamePanel.keepFsVmCheckbox.isSelected();
    }

    void setAddButtonListener(ActionListener addButtonListener) {
        this.addButtonListener = addButtonListener;
    }
}
