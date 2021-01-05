package org.jrd.frontend.NewFsVmFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.VmManager;

import javax.swing.JOptionPane;

public class NewFsVmController {

    NewFsVmView newConnectionView;
    VmManager vmManager;

    public NewFsVmController(NewFsVmView newConnectionView, VmManager vmManager){
        this.newConnectionView = newConnectionView;
        this.vmManager = vmManager;

        newConnectionView.setAddButtonListener(e -> addRemoteVmInfo());
    }

    private void addRemoteVmInfo(){
        String cp = newConnectionView.getHostname();
        String name = newConnectionView.getPortString();
        if (cp.isEmpty()) {
            JOptionPane.showMessageDialog(newConnectionView, "CP is Empty.", " ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        //spllit, verifyu existend
    }



}
