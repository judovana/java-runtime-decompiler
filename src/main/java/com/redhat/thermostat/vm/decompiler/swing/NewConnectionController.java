package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmManager;

public class NewConnectionController {

    NewConnectionView newConnectionView;
    VmManager vmManager;

    NewConnectionController(NewConnectionView newConnectionView, VmManager vmManager){
        this.newConnectionView = newConnectionView;
        this.vmManager = vmManager;

        newConnectionView.setAddButtonListener(e -> addRemoteVmInfo());
    }

    public void addRemoteVmInfo(){
        String hostname = newConnectionView.getHostname();
        int port = newConnectionView.getPort();

        vmManager.createRemoteVM(hostname, port);
    }

}
