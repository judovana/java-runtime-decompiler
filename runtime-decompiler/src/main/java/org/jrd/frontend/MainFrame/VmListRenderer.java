package org.jrd.frontend.MainFrame;

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
        this.setBorder(new EtchedBorder(1));

    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        VmInfo vminfo = (VmInfo)value;
        if (vminfo.isLocal()){
            name.setText("   " + vminfo.getVmName().split(" ")[0]);
            pid.setText("   PID: " + vminfo.getVmPid());
            this.setToolTipText("<html>NAME: " + vminfo.getVmName().split(" ")[0] + "<br>PID: " + vminfo.getVmPid() + "</html>");
        } else {
            name.setText("   Hostname: " + vminfo.getVmName());
            pid.setText("   Port: " + vminfo.getVmDecompilerStatus().getListenPort());
            this.setToolTipText("<html>Hostname: " + vminfo.getVmName() + "<br>Port: " + vminfo.getVmDecompilerStatus().getListenPort() + "</html>");
        }

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
