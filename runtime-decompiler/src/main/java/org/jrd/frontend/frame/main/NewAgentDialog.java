package org.jrd.frontend.frame.main;

import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.agentstore.AgentLoneliness;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public final class NewAgentDialog {

    private static final JDialog NAD = NewAgentDialog.create();

    public static void show() {
        NAD.setVisible(true);
    }

    private NewAgentDialog() {
    }

    private static JDialog create() {
        JDialog nad = new JDialog();
        nad.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        nad.setModal(false);
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4));
        ButtonGroup liveliness = new ButtonGroup();
        for (AgentLiveliness al : AgentLiveliness.values()) {
            JToggleButton t = new JToggleButton(al.toButton());
            liveliness.add(t);
            buttonsPanel.add(t);
            t.setToolTipText(BytecodeDecompilerView.styleTooltip() + al.toHelp());
            if (al.equals(AgentLiveliness.PERMANENT)) {
                t.setSelected(true);
            }
        }
        JTextField portField = new JTextField();
        portField.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "Type number to enforce agent port<br>leave empty for default guessing"
        );
        buttonsPanel.add(portField);
        ButtonGroup loneliness = new ButtonGroup();
        for (AgentLoneliness al : AgentLoneliness.values()) {
            JToggleButton t = new JToggleButton(al.toButton());
            loneliness.add(t);
            buttonsPanel.add(t);
            t.setToolTipText(BytecodeDecompilerView.styleTooltip() + al.toHelp());
            if (al.equals(AgentLoneliness.SINGLE_INSTANCE)) {
                t.setSelected(true);
            }
        }
        nad.add(buttonsPanel);
        JPanel vmPanel = new JPanel();
        vmPanel.add(new JLabel("Attach to pid:"), BorderLayout.WEST);
        vmPanel.add(new JTextField("????????"), BorderLayout.CENTER);
        vmPanel.add(new JButton("Select"), BorderLayout.EAST);
        nad.add(vmPanel, BorderLayout.NORTH);
        JPanel okCancelPanel = new JPanel();
        JButton attach = new JButton("Attach");
        attach.addActionListener(a -> {
            JOptionPane.showMessageDialog(nad, "why failed or port");
            nad.setVisible(false);
        });
        okCancelPanel.add(attach, BorderLayout.WEST);
        okCancelPanel.add(new JButton("Reset to Defaults"), BorderLayout.CENTER);
        JButton hide = new JButton("Hide");
        hide.addActionListener(a -> nad.setVisible(false));
        okCancelPanel.add(hide, BorderLayout.EAST);
        nad.add(okCancelPanel, BorderLayout.SOUTH);
        nad.pack();
        ScreenFinder.centerWindowToCurrentScreen(nad);
        return nad;
    }
}
