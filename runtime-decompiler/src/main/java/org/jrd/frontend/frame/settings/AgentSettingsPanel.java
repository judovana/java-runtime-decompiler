package org.jrd.frontend.frame.settings;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import org.jrd.backend.data.Directories;
import org.jrd.frontend.frame.main.BytecodeDecompilerView;
import org.jrd.frontend.frame.plugins.FileSelectorArrayRow;

public class AgentSettingsPanel extends JPanel implements ChangeReporter {

    private JTextField agentPathTextField;
    private JLabel agentPathLabel;
    private JButton browseButton;
    private JFileChooser chooser;

    AgentSettingsPanel(String initialAgentPath) {
        agentPathTextField = new JTextField();
        agentPathTextField.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "Select a path to the Decompiler Agent.<br />" +
                        FileSelectorArrayRow.getTextFieldToolTip()
        );
        agentPathTextField.setText(initialAgentPath);

        agentPathLabel = new JLabel("Decompiler Agent path");
        browseButton = new JButton("Browse");

        chooser = new JFileChooser();
        File dir;
        if (Directories.isPortable()) {
            dir = new File(Directories.getJrdLocation() + File.separator + "libs");
        } else {
            dir = new File(Directories.getJrdLocation() + File.separator + "decompiler_agent" + File.separator + "target");
        }
        chooser.setCurrentDirectory(FileSelectorArrayRow.fallback(dir));

        browseButton.addActionListener(actionEvent -> {
            int dialogResult = chooser.showOpenDialog(null);
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                agentPathTextField.setText(chooser.getSelectedFile().getPath());
            }
        });

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        this.add(this.agentPathLabel, gbc);

        gbc.weightx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        this.add(agentPathTextField, gbc);

        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        browseButton.setPreferredSize(BytecodeDecompilerView.buttonSizeBasedOnTextField(browseButton, agentPathTextField));
        this.add(browseButton, gbc);
    }

    public String getAgentPath() {
        return agentPathTextField.getText();
    }

    @Override
    public void setChangeReporter(ActionListener listener) {
        ChangeReporter.addTextChangeListener(listener, agentPathTextField);
    }
}
