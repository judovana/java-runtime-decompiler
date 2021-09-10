package org.jrd.frontend.frame.plugins;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Directories;
import org.jrd.backend.decompiling.ExpandableUrl;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileSelectorArrayRow extends JPanel {

    private JTextField textField;
    private JButton removeButton;
    private JButton browseButton;
    private JFileChooser chooser;

    private static final String DELETE_ICON = "/icons/icons8-trash-24.png";

    FileSelectorArrayRow(FileSelectorArrayPanel parent, String url) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        textField = new JTextField(url);
        textField.setPreferredSize(new Dimension(0, 32));
        textField.setToolTipText(BytecodeDecompilerView.styleTooltip() +
                "Select a path to the dependency .jar file.<br />" +
                getTextFieldToolTip()
        );

        try {
            ImageIcon icon = new ImageIcon(FileSelectorArrayRow.class.getResource(DELETE_ICON));
            removeButton = new JButton(icon);
        } catch (NullPointerException e) {
            removeButton = new JButton("X");
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("File " + DELETE_ICON + " not found. Falling back to String version.", e));
        }
        removeButton.addActionListener(actionEvent -> {
            parent.removeRow(this);
        });

        chooser = new JFileChooser();
        File dir;
        if (Directories.isPortable()) {
            dir = new File(Directories.getJrdLocation() + File.separator + "libs" + File.separator + "decompilers");
        } else {
            dir = new File(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
        }
        chooser.setCurrentDirectory(fallback(dir));

        browseButton = new JButton("Browse");
        browseButton.addActionListener(actionEvent -> {
            int returnVar = chooser.showOpenDialog(this);
            if (returnVar == JFileChooser.APPROVE_OPTION) {
                textField.setText(chooser.getSelectedFile().getPath());
            }
        });
        removeButton.setPreferredSize(new Dimension(32, 32));

        gbc.gridx = 0;
        gbc.weighty = 1;
        gbc.weightx = 1;
        this.add(textField, gbc);
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.gridx = 1;
        this.add(removeButton, gbc);
        gbc.gridx = 2;
        this.add(Box.createHorizontalStrut(20), gbc);
        gbc.gridx = 3;
        this.add(browseButton, gbc);
    }

    public static File fallback(File currentDir) {
        while (!currentDir.exists() && currentDir.getParentFile() != null) {
            currentDir = currentDir.getParentFile();
        }

        return currentDir;
    }

    public static String getTextFieldToolTip() {
        return "A valid path is absolute, " +
                ((Directories.isOsWindows()) ? "can start with a single forward slash \"/\", " : "") +
                "and can contain the following macros:<br /><ul>" +

                "<li><b>${HOME}</b>, which substitutes <b>" +
                ExpandableUrl.unifySlashes(System.getProperty("user.home")) + "</b></li>" +

                "<li><b>${XDG_CONFIG_HOME}</b>, which substitutes <b>" +
                ExpandableUrl.unifySlashes(Directories.getXdgJrdBaseDir()) + "</b></li>" +

                "<li><b>${JRD}</b>, which substitutes <b>" +
                ExpandableUrl.unifySlashes(Directories.getJrdLocation()) + "</b></li>" +
                "</ul></html>";
    }

    public JTextField getTextField() {
        return textField;
    }

}
