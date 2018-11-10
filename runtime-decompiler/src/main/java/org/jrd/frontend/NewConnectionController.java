package org.jrd.frontend;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.VmManager;

import javax.swing.*;

public class NewConnectionController {

    NewConnectionView newConnectionView;
    VmManager vmManager;

    NewConnectionController(NewConnectionView newConnectionView, VmManager vmManager){
        this.newConnectionView = newConnectionView;
        this.vmManager = vmManager;

        newConnectionView.setAddButtonListener(e -> addRemoteVmInfo());
    }

    private void addRemoteVmInfo(){
        String hostname = newConnectionView.getHostname();
        String portString = newConnectionView.getPortString();
        if (hostname.isEmpty()){
            JOptionPane.showMessageDialog(newConnectionView, "Hostname is Empty.", " ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (isValidPort(portString)){
            int port = Integer.parseInt(portString);
            vmManager.createRemoteVM(hostname, port);
            newConnectionView.dispose();
        } else {
            JOptionPane.showMessageDialog(newConnectionView, "VM port is invalid.", " ", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Returns true if portString is an integer between 0 and 65535
     * @param portString
     * @return
     */
    boolean isValidPort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            return false;
        }
    }

}
