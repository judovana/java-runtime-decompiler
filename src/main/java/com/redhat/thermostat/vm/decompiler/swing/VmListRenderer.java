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
        this.setBorder(new EtchedBorder(1));

        this.add(pid);
        this.add(name);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        VmInfo vminfo = (VmInfo)value;
        pid.setText(vminfo.getVmId());
        name.setText(vminfo.getVmName().split(" ")[0]);
        name.setToolTipText(vminfo.getVmName());

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
