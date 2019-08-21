package org.jrd.frontend.ConfigureFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Config;
import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.jrd.backend.data.Directories.*;
import static org.jrd.frontend.PluginMangerFrame.FileSelectorArrayRow.fallback;

public class ConfigureView extends JDialog{

    private confBrosePanel configureAgentPathPanel;
    private confBrosePanel configureDecompilerPathPanel;
    private JPanel configureOKCancelPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel okCancelPanel;
    private Config config = Config.getConfig();


    JPanel mainPanel;

    public class confBrosePanel extends JPanel{

        public JTextField textField;
        public JLabel label;
        public JButton browseButton;
        public JFileChooser chooser;

        confBrosePanel(String label){
            this(label, "Browse");
        }

        confBrosePanel(String label, String ButtonLabel){

            this.textField = new JTextField();
            this.label = new JLabel(label);
            this.browseButton = new JButton(ButtonLabel);

            chooser = new JFileChooser();
            File dir;
            if(isPortable()){
                dir = new File(getJrdLocation() + File.separator + "libs");
            } else {
                dir = new File(getJrdLocation() + File.separator + "decompiler_agent" + File.separator + "target");
            }
            chooser.setCurrentDirectory(fallback(dir));

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 1;
            this.add(this.label, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            this.add(Box.createHorizontalStrut(20), gbc);
            gbc.weightx = 1;
            gbc.gridx = 1;
            this.add(textField, gbc);
            gbc.weightx = 0;
            gbc.gridx = 2;
            this.add(Box.createHorizontalStrut(20), gbc);
            gbc.gridx = 3;
            this.add(browseButton, gbc);
            gbc.gridx = 4;
            this.add(Box.createHorizontalStrut(20), gbc);
            this.setPreferredSize(new Dimension(0,120));
        }
    }


    public ConfigureView(MainFrameView mainFrameView){
        configureAgentPathPanel = new confBrosePanel("Decompiler Agent path");
        configureAgentPathPanel.textField.setText(config.getAgentPath());
        configureAgentPathPanel.browseButton.addActionListener(actionEvent -> {
            int returnVar = configureAgentPathPanel.chooser.showOpenDialog(configureAgentPathPanel);
            if (returnVar == JFileChooser.APPROVE_OPTION){
                configureAgentPathPanel.textField.setText(configureAgentPathPanel.chooser.getSelectedFile().getPath());
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(actionEvent -> {
            config.setAgentPath(configureAgentPathPanel.textField.getText());
            try {
                config.saveConfigFile();
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
            dispose();
        });
        okButton.setPreferredSize(new Dimension(90,30));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> {
            dispose();
        });
        cancelButton.setPreferredSize(new Dimension(90,30));

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
        configureOKCancelPanel.setPreferredSize(new Dimension(0,60));


        mainPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(configureAgentPathPanel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 1;
        mainPanel.add(Box.createVerticalGlue(),gbc);
        gbc.gridy = 2;
        gbc.weighty = 0;
        mainPanel.add(configureOKCancelPanel, gbc);


        this.setTitle("Configure Decompiler Agent");
        this.setSize(new Dimension(800,400));
        this.setMinimumSize(new Dimension(250,330));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(mainFrameView.getMainFrame());
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.add(mainPanel);
        this.setVisible(true);
    }

}
