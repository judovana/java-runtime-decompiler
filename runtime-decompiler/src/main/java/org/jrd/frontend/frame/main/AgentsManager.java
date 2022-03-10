package org.jrd.frontend.frame.main;

import org.jrd.backend.core.agentstore.KnownAgent;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.VmInfo;
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AgentsManager {

    private static final AgentsManager DIALOG = new AgentsManager();

    private static final String VM_TITLE = "Known agents: ";
    private final JLabel activeOverridesLabel = new JLabel(VM_TITLE);
    private final JList<KnownAgentWrapper> knownAgents;
    private final JDialog window = new JDialog((JFrame) null, "Manage known agents");
    private VmManager vmManager;

    private static final class KnownAgentWrapper {
        private final KnownAgent agent;
        private Optional<String> version = Optional.empty();
        private Optional<String> result = Optional.empty();
        private Optional<Exception> exception = Optional.empty();

        KnownAgentWrapper(KnownAgent agent) {
            this.agent = agent;
        }

        @Override
        public String toString() {
            String s = agent.toPrint();
            if (version.isPresent()) {
                s = s + "<br> <li>" + version.get() + "</li>";
            }
            if (result.isPresent()) {
                s = s + "<br> <li>" + result.get() + "</li>";
            }
            if (exception.isPresent()) {
                s = s + "<br> <li>" + exception.get().toString() + "</li>";
            }
            return s;
        }
    }

    private AgentsManager() {
        window.setLayout(new BorderLayout(5, 5));
        window.getRootPane().setBorder(BorderFactory.createCompoundBorder(window.getRootPane().getBorder(), new EmptyBorder(5, 5, 5, 5)));
        window.setMinimumSize(new Dimension(400, 400));

        knownAgents = new JList<>();
        knownAgents.setCellRenderer((jList, knownAgentWrapper, index, selected, hasFocus) -> {
            String s = "<html>";
            if (selected) {
                s = s + "<body bgcolor=\"#00ffff\">";
            } else {
                s = s + "<body>";
            }
            s = s + knownAgentWrapper.toString();
            JLabel l = new JLabel(s);
            return l;
        });
        knownAgents.setMinimumSize(new Dimension(600, 400));
        knownAgents.setModel(new DefaultComboBoxModel<>());

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(a -> setKnownAgents(false));

        JButton hanshakes = new JButton("handshake");
        hanshakes.addActionListener(a -> setKnownAgents(true));

        JButton remove = new JButton("Detach selected!");
        remove.addActionListener(a -> dettach());

        JButton close = new JButton("Close");
        close.addActionListener(a -> hide());

        JPanel southPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        southPanel.add(refresh);
        southPanel.add(hanshakes);
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
        DIALOG.setKnownAgents(false);
        DIALOG.window.setModal(true);
        DIALOG.window.setVisible(true);
    }

    private void dettach() {
        try {
            for (final KnownAgentWrapper agentw : knownAgents.getSelectedValuesList()) {
                KnownAgent agent = agentw.agent;
                Lib.detach(agent.getHost(), agent.getPort(), vmManager);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
        setKnownAgents(false);
    }

    private void setKnownAgents(boolean handshake) {
        try {
            KnownAgentWrapper[] wrappers = KnownAgents.getInstance().getAgents().stream().map(a -> new KnownAgentWrapper(a))
                    .collect(Collectors.toList()).toArray(new KnownAgentWrapper[0]);
            if (handshake) {
                for (KnownAgentWrapper wrapper : wrappers) {
                    try {
                        List<VmInfo> localVms = new ArrayList<>();
                        vmManager.getVmInfoSet().forEach(info -> {
                            if (info.getType() == VmInfo.Type.LOCAL) {
                                localVms.add(info);
                            }
                            //see updateVmLists in DecompilationController
                            // That handles also other types of vms. But as KnownAgents are
                            // the only one pid based.... It is ommited here.
                        });
                        boolean found = false;
                        Lib.HandhshakeResult r = null;
                        for (VmInfo vmInfo : localVms) {
                            if (vmInfo.getVmPid() == wrapper.agent.getPid()) {
                                found = true;
                                r = Lib.handshakeAgent(wrapper.agent, vmInfo, vmManager);
                                break;
                            }
                        }
                        if (!found) {
                            r = Lib.handshakeAgent(wrapper.agent, vmManager);
                        }
                        wrapper.version = Optional.of(r.getAgentVersion());
                        wrapper.result = Optional.of(r.getDiff());
                    } catch (Exception ex) {
                        wrapper.exception = Optional.of(ex);
                    }
                }
            }
            knownAgents.setModel(new DefaultComboBoxModel<>(wrappers));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

    }

    private void setVmManager(VmManager vmManager) {
        this.vmManager = vmManager;
    }
}
