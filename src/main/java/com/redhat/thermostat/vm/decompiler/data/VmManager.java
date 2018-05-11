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
import java.util.HashSet;
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
    HashSet<VirtualMachineDescriptor> descriptors;

    //constructor
    public VmManager() {
        this.vmList = new ArrayList<>();
        this.vmStatusMap = new HashMap<>();
        this.descriptors = new HashSet<>();
        createVmList();
        createReferences(vmList);
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
        if (!vmId.get().equals(status.getVmId())) {
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
    public boolean replaceVmDecompilerStatus(VmId vmId, VmDecompilerStatus status) {
        for (VmId id : vmStatusMap.keySet()) {
            if (id.equals(vmId)) {
                if (!vmId.get().equals(status.getVmId())) {
                    return false;
                } //log that the status was kept old
                else {
                    vmStatusMap.replace(vmId, status);
                    return true;
                }
            }
        }

        this.addVmDecompilerStatus(vmId, status);
        return true;
    }

    private void createVmList() {
        List<VirtualMachineDescriptor> descr = VirtualMachine.list();
        this.descriptors = new HashSet(descr);
    }

    private void createReferences(List<VmRef> refList) {
        for (VirtualMachineDescriptor descriptor : descriptors) {
            String id = descriptor.id();
            String processId = descriptor.id();
            Integer pid = Integer.parseInt(processId);
            String name = descriptor.displayName();
            VmRef ref = new VmRef(id, pid, name);
            refList.add(ref);

        }

    }

    public void refreshReferences() {
        ArrayList<VmRef> refList = new ArrayList<>();
        createVmList();
        createReferences(refList);
        //part of the code where the old list is compared to the new list

    }

}
