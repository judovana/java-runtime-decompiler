package org.jrd.frontend.frame.main;

import org.jrd.backend.core.agentstore.KnownAgent;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.Lib;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public final class AgentsManager {

    private static final AgentsManager DIALOG = new AgentsManager();

    private static final String VM_TITLE = "Known agents: ";
    private final JLabel activeOverridesLabel = new JLabel(VM_TITLE);
    private final JList<KnownAgent> knownAgents;
    private final JDialog window = new JDialog((JFrame) null, "Manage known agents");
    private VmManager vmManager;

    private AgentsManager() {
        window.setLayout(new BorderLayout(5, 5));
        window.getRootPane().setBorder(BorderFactory.createCompoundBorder(window.getRootPane().getBorder(), new EmptyBorder(5, 5, 5, 5)));
        window.setMinimumSize(new Dimension(400, 400));

        knownAgents = new JList<>();
        knownAgents.setCellRenderer((jList, knownAgent, index, selected, hasFocus) -> {
            JLabel l = new JLabel(knownAgent.toPrint());
            if (selected) {
                l.setForeground(Color.RED);
            }
            return l;
        });
        knownAgents.setMinimumSize(new Dimension(600, 400));
        knownAgents.setModel(new DefaultComboBoxModel<>());

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(a -> setKnownAgents());

        JButton remove = new JButton("Detach selected!");
        remove.addActionListener(a -> removeOverride());

        JButton close = new JButton("Close");
        close.addActionListener(a -> hide());

        JPanel southPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        southPanel.add(refresh);
        southPanel.add(remove);
        southPanel.add(close);

        window.add(activeOverridesLabel, BorderLayout.NORTH);
        window.add(new JScrollPane(knownAgents), BorderLayout.CENTER);
        window.add(southPanel, BorderLayout.SOUTH);
        window.pack();
        ScreenFinder.centerWindowToCurrentScreen(window);
    }

    public void hide() {
        window.setVisible(false);
    }

    public static void showFor(JFrame parent, VmManager vmManager) {
        DIALOG.setVmManager(vmManager);
        DIALOG.setKnownAgents();
        DIALOG.window.setModal(false);
        DIALOG.window.setVisible(true);
    }

    private void removeOverride() {
        try {
            for (final KnownAgent agent : knownAgents.getSelectedValuesList()) {
                Lib.detach(agent.getHost(), agent.getPort(), vmManager);
            }
            setKnownAgents();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

        setKnownAgents();
    }

    private void setKnownAgents() {
        try {
            knownAgents.setModel(new DefaultComboBoxModel<>(KnownAgents.getInstance().getAgents().toArray(new KnownAgent[0])));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

    }

    private void setVmManager(VmManager vmManager) {
        this.vmManager = vmManager;
    }
}
