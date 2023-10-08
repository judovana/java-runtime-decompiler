package org.jrd.frontend.frame.main;

import org.jrd.backend.communication.CallDecompilerAgent;
import org.jrd.backend.core.AgentLoader;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.agentstore.AgentLoneliness;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.Model;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.cli.utils.AgentConfig;
import org.jrd.frontend.frame.main.decompilerview.BytecodeDecompilerView;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public final class NewAgentDialog extends JDialog {

    private JList<VmInfo> vmsList;
    private JButton selectButton;

    public static void show(JList<VmInfo> vms) {
        NewAgentDialog nad = NewAgentDialog.create();
        nad.setVms(vms);
        nad.setVisible(true);
    }

    private NewAgentDialog() {
    }

    private void setVms(JList l) {
        this.vmsList = l;
    }

    private static NewAgentDialog create() {
        NewAgentDialog nad = new NewAgentDialog();
        nad.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        nad.setModal(false);
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4));
        ButtonGroup liveliness = new ButtonGroup();
        addLivelinessButtons(buttonsPanel, liveliness);
        JTextField portField = new JTextField();
        portField.setToolTipText(
                BytecodeDecompilerView.styleTooltip() + "Type number to enforce agent port<br>leave empty for default guessing"
        );
        buttonsPanel.add(portField);
        ButtonGroup loneliness = new ButtonGroup();
        addLonelinessButtons(buttonsPanel, loneliness);
        nad.add(buttonsPanel);
        JPanel vmPanel = new JPanel();
        vmPanel.add(new JLabel("Attach to pid:"), BorderLayout.WEST);
        JTextField pidField = new JTextField("????????");
        vmPanel.add(pidField, BorderLayout.CENTER);
        nad.selectButton = new JButton("Select");
        nad.selectButton.addActionListener(a -> {
            if (nad.vmsList == null || nad.vmsList.getSelectedValue() == null) {
                pidField.setText("?" + pidField.getText() + "?");
            } else {
                pidField.setText(nad.vmsList.getSelectedValue().getVmId());
            }
        });
        vmPanel.add(nad.selectButton, BorderLayout.EAST);
        nad.add(vmPanel, BorderLayout.NORTH);
        JPanel okCancelPanel = new JPanel();
        JButton attach = new JButton("Attach");
        attach.addActionListener(new AttachActionListener(portField, loneliness, liveliness, pidField, nad));
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

    public static int manualAttach(Component parent, AgentConfig aconf, int targetPid, boolean gui) {
        int secondJrdPort = 0;
        try {
            secondJrdPort = AgentLoader.attachImpl(targetPid, aconf);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex.getMessage());
            Logger.getLogger().log(Logger.Level.DEBUG, ex);
            if (gui) {
                JOptionPane.showMessageDialog(parent, ex.getMessage());
            }
        }
        if (secondJrdPort > 0) {
            switch (Config.getConfig().getAdditionalAgentAction()) {
                case ADD:
                    Model.getModel().getVmManager().createRemoteVM(CallDecompilerAgent.DEFAULT_ADDRESS, secondJrdPort, false);
                    break;
                case ADD_AND_SAVE:
                    Model.getModel().getVmManager().createRemoteVM(CallDecompilerAgent.DEFAULT_ADDRESS, secondJrdPort, true);
                    break;
                case NOTHING:
                    break;
                case ASK:
                    if (gui) {
                        int r = JOptionPane.showConfirmDialog(
                                parent,
                                " Do you want to connect to VM via port " + secondJrdPort + " via JRD asap? save? " + "Pres yes for " +
                                        "add and save, no for add and cancel for nothing. If nothing, write donw the " + "port :) " +
                                        secondJrdPort
                        );
                        if (r == JOptionPane.YES_OPTION) {
                            Model.getModel().getVmManager().createRemoteVM(CallDecompilerAgent.DEFAULT_ADDRESS, secondJrdPort, true);
                        } else if (r == JOptionPane.NO_OPTION) {
                            Model.getModel().getVmManager().createRemoteVM(CallDecompilerAgent.DEFAULT_ADDRESS, secondJrdPort, false);
                        }
                    } else {
                        Logger.getLogger()
                                .log(Logger.Level.ALL, "Ask mode is on. Without gui, no-op. Connect to " + secondJrdPort + " manually");
                    }
                    break;
                default:
                    throw new RuntimeException("Undefined case: " + Config.getConfig().getAdditionalAgentAction());
            }
        } else {
            String s = "Attach failed, consult logs of *foreign* process.";
            Logger.getLogger().log(Logger.Level.ALL, s);
            if (gui) {
                JOptionPane.showMessageDialog(parent, s);
            }
        }
        return secondJrdPort;
    }

    private static void addLonelinessButtons(JPanel buttonsPanel, ButtonGroup loneliness) {
        for (AgentLoneliness al : AgentLoneliness.values()) {
            JToggleButton t = new JToggleButton(al.toButton());
            loneliness.add(t);
            buttonsPanel.add(t);
            t.setActionCommand(al.toString());
            t.setToolTipText(BytecodeDecompilerView.styleTooltip() + al.toHelp());
            if (al.equals(AgentLoneliness.SINGLE_INSTANCE)) {
                t.setSelected(true);
            }
        }
    }

    private static void addLivelinessButtons(JPanel buttonsPanel, ButtonGroup liveliness) {
        for (AgentLiveliness al : AgentLiveliness.values()) {
            JToggleButton t = new JToggleButton(al.toButton());
            liveliness.add(t);
            buttonsPanel.add(t);
            t.setActionCommand(al.toString());
            t.setToolTipText(BytecodeDecompilerView.styleTooltip() + al.toHelp());
            if (al.equals(AgentLiveliness.PERMANENT)) {
                t.setSelected(true);
            }
            if (al.equals(AgentLiveliness.ONE_SHOT)) {
                t.setEnabled(false);
                t.setToolTipText(t.getToolTipText() + "<br>Although this agent exists for CLI, have no reason for gui");
            }
        }
    }

    private static class AttachActionListener implements ActionListener {
        private final JTextField portField;
        private final ButtonGroup loneliness;
        private final ButtonGroup liveliness;
        private final JTextField pidField;
        private final NewAgentDialog nad;

        AttachActionListener(
                JTextField portField, ButtonGroup loneliness, ButtonGroup liveliness, JTextField pidField, NewAgentDialog nad
        ) {
            this.portField = portField;
            this.loneliness = loneliness;
            this.liveliness = liveliness;
            this.pidField = pidField;
            this.nad = nad;
        }

        @Override
        public void actionPerformed(ActionEvent a) {
            Optional<Integer> port;
            if (portField.getText().trim().isEmpty()) {
                port = Optional.empty();
            } else {
                int futurePort = Integer.parseInt(portField.getText().trim());
                if (futurePort <= 0) {
                    port = Optional.empty();
                } else {
                    port = Optional.ofNullable(futurePort);
                }
            }
            String lonelinessSel = loneliness.getSelection().getActionCommand();
            String livelinessSel = liveliness.getSelection().getActionCommand();
            AgentConfig aconf = new AgentConfig(AgentLoneliness.fromString(lonelinessSel), AgentLiveliness.fromString(livelinessSel), port);
            int targetPid = -1;
            try {
                targetPid = Integer.parseInt(pidField.getText());
            } catch (Exception ex) {
                Logger.getLogger().log(Logger.Level.ALL, ex.getMessage());
                Logger.getLogger().log(Logger.Level.DEBUG, ex);
                JOptionPane.showMessageDialog(null, "No pid? " + ex.getMessage());
                return;
            }
            manualAttach(nad, aconf, targetPid, true);
            nad.setVisible(false);
        }
    }
}
