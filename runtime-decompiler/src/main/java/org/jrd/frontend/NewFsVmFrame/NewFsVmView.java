package org.jrd.frontend.NewFsVmFrame;

import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NewFsVmView extends JDialog {

    private static String lastOpened = System.getProperty("user.home");

    private JPanel mainPanel;
    private CpNamePanel mCpNamePanel;
    private JPanel okCancelPanel;
    private JPanel configureOKCancelPanel;
    private JButton okButton;
    private JButton cancelButton;

    private ActionListener addButtonListener;

    public class CpNamePanel extends JPanel {

        JTextField cpTextField;
        JTextField nameTextField;
        JButton selectCpButton;
        JPanel textAndName;

        CpNamePanel() {
            this.selectCpButton = new JButton("...");
            this.cpTextField = new JTextField();
            this.nameTextField = new JTextField();
            this.nameTextField.setPreferredSize(new Dimension(90, 0));
            this.textAndName = new JPanel();

            textAndName.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 1;
            textAndName.add(new JLabel("Classpath"), gbc);
            gbc.gridx = 3;
            textAndName.add(new JLabel("Optional name"), gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            textAndName.add(Box.createHorizontalStrut(20), gbc);
            gbc.weightx = 1;
            gbc.gridx = 1;
            textAndName.add(cpTextField, gbc);
            gbc.weightx = 0;
            gbc.gridx = 2;
            textAndName.add(Box.createHorizontalStrut(20), gbc);
            gbc.gridx = 3;
            textAndName.add(nameTextField, gbc);
            gbc.gridx = 4;
            textAndName.add(Box.createHorizontalStrut(20), gbc);
            textAndName.setPreferredSize(new Dimension(0, 100));
            this.setLayout(new BorderLayout());
            this.add(textAndName);
            JPanel selectButtonPane = new JPanel(new GridBagLayout());
            selectButtonPane.add(selectCpButton);
            selectCpButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JFileChooser jf = new JFileChooser(lastOpened);
                    jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    int returnVal = jf.showOpenDialog(selectCpButton);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if (jf.getSelectedFile().isDirectory()){
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
            });
            this.add(selectButtonPane, BorderLayout.WEST);
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

        configureOKCancelPanel = new JPanel(new GridBagLayout());
        configureOKCancelPanel.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        configureOKCancelPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.gridx = 1;
        configureOKCancelPanel.add(okCancelPanel, gbc);
        configureOKCancelPanel.setPreferredSize(new Dimension(0, 60));


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
        mainPanel.add(configureOKCancelPanel, gbc);


        this.setTitle("New filesystem `VM`");
        this.setSize(new Dimension(400, 220));
        this.setMinimumSize(new Dimension(250, 220));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);

    }

    String getCP() {
        return mCpNamePanel.cpTextField.getText();
    }

    String getNameHelper() throws NumberFormatException {
        return mCpNamePanel.nameTextField.getText();
    }

    void setAddButtonListener(ActionListener addButtonListener) {
        this.addButtonListener = addButtonListener;
    }
}
