/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.data;

import com.redhat.thermostat.vm.decompiler.core.VmDecompilerStatus;
import java.util.ArrayList;
import java.util.HashMap;
import workers.VmId;

/**
 *
 * @author pmikova
 */
public class VmManager {
    
    ArrayList<VmId> vmList;
    HashMap<VmId, VmDecompilerStatus> vmStatusMap;
    
    //constructor
    public VmManager(){
    
}
    // get
    
    public ArrayList<VmId> getAllVm() {
        return this.vmList;
        
    }
    
    public VmDecompilerStatus getVmDecompilerStatus(VmId vmId){
        return vmStatusMap.get(vmId);
        
        
    }
    
    // set
    
    public void addVmDecompilerStatus(VmId vmId, VmDecompilerStatus status){
        // we need to check if the status is correct
        if(!vmId.equals(status.getVmId())){
            throw new IllegalArgumentException("Given VM ID does not equal VM"
                    + " ID given in the VmDecompilerStatus class. This is not"
                    + " allowed state.");
        }
        vmStatusMap.put(vmId, status);        
    }
    
    public void removeVmDecompilerStatus(VmId vmId){
        vmStatusMap.remove(vmId);
    }
    
    // trigger reload
    
    //must contain removing of old statuses, reload of vm's list, etc.
    
    public void replaceVmDecompilerStatus(VmId vmId, VmDecompilerStatus status){
        if(!vmId.equals(status.getVmId())){
            throw new IllegalArgumentException("Given VM ID does not equal VM"
                    + " ID given in the VmDecompilerStatus class. This is not"
                    + " allowed state.");
        }
        //log that the status was kept old
        else{
        vmStatusMap.replace(vmId, status);
        }
    }
    
}
