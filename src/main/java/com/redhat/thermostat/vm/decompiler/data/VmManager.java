/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.data;

import com.redhat.thermostat.vm.decompiler.core.VmDecompilerStatus;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import workers.VmId;
import workers.VmRef;

/**
 *
 * @author pmikova
 */
public class VmManager {

    ArrayList<VmRef> vmList;
    HashMap<VmId, VmDecompilerStatus> vmStatusMap;
    List<VirtualMachineDescriptor> descriptors;

    //constructor
    public VmManager() {
          createVmList();
    }
    // get

    public ArrayList<VmRef> getAllVm() {
        return this.vmList;

    }

    public VmDecompilerStatus getVmDecompilerStatus(VmId vmId) {
        return vmStatusMap.get(vmId);

    }

    // set
    public void addVmDecompilerStatus(VmId vmId, VmDecompilerStatus status) {
        // we need to check if the status is correct
        if (!vmId.equals(status.getVmId())) {
            throw new IllegalArgumentException("Given VM ID does not equal VM"
                    + " ID given in the VmDecompilerStatus class. This is not"
                    + " allowed state.");
        }
        vmStatusMap.put(vmId, status);
    }

    public void removeVmDecompilerStatus(VmId vmId) {
        vmStatusMap.remove(vmId);
    }

    // trigger reload
    //must contain removing of old statuses, reload of vm's list, etc.
    public void replaceVmDecompilerStatus(VmId vmId, VmDecompilerStatus status) {
        if (!vmId.equals(status.getVmId())) {
            throw new IllegalArgumentException("Given VM ID does not equal VM"
                    + " ID given in the VmDecompilerStatus class. This is not"
                    + " allowed state.");
        } //log that the status was kept old
        else {
            vmStatusMap.replace(vmId, status);
        }
    }

    private void createVmList() {
        this.descriptors = VirtualMachine.list();
         /*for (VirtualMachineDescriptor descriptor : descriptors) {
            System.out.println("Found JVM: " + descriptor.displayName());
            try {
                VirtualMachine vm = VirtualMachine.attach(descriptor);
                String version = vm.getSystemProperties().getProperty("java.runtime.version");
                System.out.println("   Runtime Version: " + version);

            } catch (Exception e) {
                // ...
            }
        }*/
    }
    
    private void createReferences(){
        
    }
}
