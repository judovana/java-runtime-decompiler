package org.jrd.frontend.frame.main;

import org.jrd.backend.data.VmInfo;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class VmListRenderer extends JPanel implements ListCellRenderer {

    JLabel pid;
    JLabel name;

    public VmListRenderer() {
        pid = new JLabel();
        name = new JLabel();
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        this.add(name, gbc);
        gbc.weighty = 0.75;
        gbc.gridy = 1;
        this.add(pid, gbc);
        this.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        VmInfo vminfo = (VmInfo) value;
        if (vminfo.getType() == VmInfo.Type.LOCAL) {
            name.setText("   " + vminfo.getVmName().split(" ")[0]);
            pid.setText("   PID: " + vminfo.getVmPid());
            this.setToolTipText(BytecodeDecompilerView.styleTooltip() + "NAME: " + vminfo.getVmName().split(" ")[0] + "<br>PID: " + vminfo.getVmPid() + "</html>");
        } else if (vminfo.getType() == VmInfo.Type.REMOTE) {
            name.setText("   Hostname: " + vminfo.getVmName());
            pid.setText("   Port: " + vminfo.getVmDecompilerStatus().getListenPort());
            this.setToolTipText(BytecodeDecompilerView.styleTooltip() + "Hostname: " + vminfo.getVmName() + "<br>Port: " + vminfo.getVmDecompilerStatus().getListenPort() + "</html>");
        } else {
            name.setText("   " + vminfo.nameOrCp());
            pid.setText("   PID: " + vminfo.getVmPid());
            pid.setText("   id: " + vminfo.getVmId());
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}
