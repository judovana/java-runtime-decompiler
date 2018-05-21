package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmInfo;

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
        this.setBorder(new EtchedBorder(1));

    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        VmInfo vminfo = (VmInfo)value;
        pid.setText("   PID: " + vminfo.getVmId());
        name.setText("   " + vminfo.getVmName().split(" ")[0]);
        this.setToolTipText("<html>NAME: " + vminfo.getVmName().split(" ")[0] + "<br>PID: " + vminfo.getVmId() + "</html>");

        if (isSelected){
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}
