package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import java.util.ArrayList;

public class ConfigPanel extends JPanel {

    private TextInputPanel namePanel;
    private boolean canWrite;

    private FileSelectorPanel wrapperUrlPanel;
    private FileSelectorArrayPanel dependencyUrlPanel;
    private JButton decompilerUrlLink;
    private ActionListener okButtonListener;

    private ActionListener cancelButtonListener;

    public void setRemoveButtonListener(ActionListener removeButtonListener) {
        this.removeButtonListener = removeButtonListener;
    }

    private ActionListener removeButtonListener;

    private JPanel trashCanPanel;
    private JButton removeButton;

    public DecompilerWrapperInformation getDecompilerWrapperInformatio() {
        return decompilerWrapperInformatio;
    }

    DecompilerWrapperInformation decompilerWrapperInformatio;

    JButton okButton;
    JButton cancelButton;

    JPanel okCancelPanel;
    JPanel bottomPanel;
    GridBagConstraints gbc;

    private static final String TRASH_ICON = "/icons/trash.png";

    public ConfigPanel(DecompilerWrapperInformation wrapperInformation) {
        if (wrapperInformation == null){
            return;
        }

        this.decompilerWrapperInformatio = wrapperInformation;
        okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            okButtonListener.actionPerformed(actionEvent);
        });
        okButton.setPreferredSize(new Dimension(90, 30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> {
            this.cancelButtonListener.actionPerformed(actionEvent);
        });
        cancelButton.setPreferredSize(new Dimension(90, 30));

        okCancelPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
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

        // Panel for removing plugin
        trashCanPanel = new JPanel();
        try {
            ImageIcon icon = new ImageIcon(FileSelectorArrayRow.class.getResource(TRASH_ICON));
            removeButton = new JButton(icon);
        } catch (NullPointerException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("File " + TRASH_ICON + "not found. Falling back to String version.", e));
            removeButton = new JButton("X");
        }
        removeButton.addActionListener(actionEvent -> {
            removeButtonListener.actionPerformed(actionEvent);
        });
        trashCanPanel.setLayout(null);
        removeButton.setBounds(-2, 26, 32, 32); //🙈
        if (wrapperInformation.getScope().equals("local")) {
            trashCanPanel.add(removeButton);
        }
        trashCanPanel.setPreferredSize(new Dimension(60, 60));

        bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(new EtchedBorder());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        bottomPanel.add(trashCanPanel, gbc);
        gbc.gridx = 1;
        bottomPanel.add(okCancelPanel, gbc);
        bottomPanel.setPreferredSize(new Dimension(0, 60));

        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        // addComponent inserts here
        gbc.gridy = 99999;
        gbc.weighty = 1;
        this.add(Box.createVerticalGlue(), gbc);
        gbc.gridy = 100000;
        gbc.weighty = 0;
        this.add(bottomPanel, gbc);
        gbc.gridy = 0;
        File f = new File(wrapperInformation.getFileLocation());
        canWrite = f.canWrite();


        JLabel label = new JLabel();
        label.setText("Configuration JSON file: " + wrapperInformation.getFileLocation());
        this.addComponent(label);
        if (!canWrite){
            JLabel access = new JLabel();
            access.setText("You are not able to edit this configuration, because you don't have the right permissions.");
            this.addComponent(access);
        }

        namePanel = new TextInputPanel("Name");
        namePanel.textField.setText(wrapperInformation.getName());


        this.addComponent(namePanel);
        wrapperUrlPanel = new FileSelectorPanel("Decompiler wrapper URL");
        if (wrapperInformation.getWrapperURL() != null) {
            wrapperUrlPanel.setText(wrapperInformation.getWrapperURL().getPath());
        }
        this.addComponent(wrapperUrlPanel);
        dependencyUrlPanel = new FileSelectorArrayPanel("Decompiler and dependency jars");
        if (wrapperInformation.getDependencyURLs() != null) {
            wrapperInformation.getDependencyURLs().forEach(url -> dependencyUrlPanel.addRow(url.getPath(), false));
        }
        this.addComponent(dependencyUrlPanel);
        decompilerUrlLink = new JButton();
        decompilerUrlLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(wrapperInformation.getDecompilerURL().toString()));
                } catch (IOException | URISyntaxException e1 ) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e1);
                }
            }
        });
        if (wrapperInformation.getDecompilerURL() != null) {
            decompilerUrlLink.setName(wrapperInformation.getDecompilerURL().toString());
            decompilerUrlLink.setText("Go to decompiler website: " + wrapperInformation.getDecompilerURL().toString());
            //decompilerUrlLink.setName();
            this.addComponent(decompilerUrlLink);
        }
        this.setEnabled(canWrite);
        List<Component> components = getAllComponents(this);
        for  (Component compon : components){
            if (!(compon instanceof JLabel)){
                compon.setEnabled(canWrite);
            }
        }
        decompilerUrlLink.setEnabled(true);

    }

    public List<Component> getAllComponents(final Container c) {
        Component[] comps = c.getComponents();
        List<Component> compList = new ArrayList<>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container) {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }
    private void addComponent(Component component) {
        gbc.gridy++;
        this.add(component, gbc);
    }

    public void setOkButtonListener(ActionListener okButtonListener) {
        this.okButtonListener = okButtonListener;
    }

    public void setCancelButtonListener(ActionListener cancelButtonListener) {
        this.cancelButtonListener = cancelButtonListener;
    }

    public TextInputPanel getNamePanel() {
        return namePanel;
    }

    public FileSelectorPanel getWrapperUrlPanel() {
        return wrapperUrlPanel;
    }

    public FileSelectorArrayPanel getDependencyUrlPanel() {
        return dependencyUrlPanel;
    }
    public JButton getDecompilerLabel(){
        return decompilerUrlLink;
    }
}