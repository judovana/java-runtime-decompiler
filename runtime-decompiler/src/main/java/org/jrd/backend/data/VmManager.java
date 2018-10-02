package org.jrd.backend.data;

import org.jrd.backend.core.VmDecompilerStatus;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * @author pmikova
 */
public class VmManager {

    ArrayList<VmInfo> vmList;
    HashMap<VmInfo, VmDecompilerStatus> vmStatusMap;
    HashSet<VirtualMachineDescriptor> descriptors;

    ActionListener updateVmListsListener;

    public VmManager() {
        this.vmList = new ArrayList<>();
        this.vmStatusMap = new HashMap<>();
        this.descriptors = new HashSet<>();
        createVmList();
        createReferences(vmList);
    }

    public void createRemoteVM(String hostname, int port){
        String id = UUID.randomUUID().toString();
        VmInfo vmInfo = new VmInfo(id, -1, hostname, false);
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setVmId(id);
        status.setHostname(hostname);
        status.setListenPort(port);
        vmList.add(vmInfo);
        addVmDecompilerStatus(vmInfo, status);
        updateLists();
    }

    public void setUpdateVmListsListener(ActionListener listener){
        updateVmListsListener = listener;
    }

    public void updateLists(){
        updateVmListsListener.actionPerformed(new ActionEvent(this, 0, null));
    }


    public VmInfo getVmInfoByID(String VmId){
        for (VmInfo vmInfo : vmList) {
            if (vmInfo.getVmId().equals(VmId)){
                return vmInfo;
            }
        }
        throw new NoSuchElementException("VmInfo with VmID" + VmId + "does no exist in VmList");
    }


    public ArrayList<VmInfo> getAllVm() {
        return this.vmList;
    }

    public VmDecompilerStatus getVmDecompilerStatus(VmInfo vmInfo) {
        return vmStatusMap.get(vmInfo);

    }

    public void addVmDecompilerStatus(VmInfo vmInfo, VmDecompilerStatus status) {
        // we need to check if the status is correct
        if (!vmInfo.getVmId().equals(status.getVmId())) {
            throw new IllegalArgumentException("Given VM ID does not equal VM"
                    + " ID given in the VmDecompilerStatus class. This is not"
                    + " allowed state.");
        }
        vmInfo.setVmDecompilerStatus(status);
        vmStatusMap.put(vmInfo, status);
    }

    public void removeVmDecompilerStatus(VmInfo vmInfo) {
        vmStatusMap.remove(vmInfo);
    }

    // trigger reload
    //must contain removing of old statuses, reload of vm's list, etc.
    public boolean replaceVmDecompilerStatus(VmInfo vmInfo, VmDecompilerStatus status) {
        for (VmInfo statusMapVmInfo : vmStatusMap.keySet()) {
            if (statusMapVmInfo.equals(vmInfo)) {
                if (vmInfo.getVmId().equals(status.getVmId())) {
                    vmStatusMap.replace(vmInfo, status);
                    return true;
                }
                else {
                    //log that the status was kept old
                    return false;
                }
            }
        }

        this.addVmDecompilerStatus(vmInfo, status);
        return true;
    }

    private void createVmList() {
        List<VirtualMachineDescriptor> descr = VirtualMachine.list();
        this.descriptors = new HashSet(descr);
    }

    private void createReferences(List<VmInfo> vmList) {
        for (VirtualMachineDescriptor descriptor : descriptors) {
            String vmId = descriptor.id();
            String processId = descriptor.id();
            Integer pid = Integer.parseInt(processId);
            String name = descriptor.displayName();
            VmInfo vmInfo = new VmInfo(vmId, pid, name);
            vmList.add(vmInfo);
        }

    }

    public void refreshReferences() {
        ArrayList<VmInfo> refList = new ArrayList<>();
        createVmList();
        createReferences(refList);
        //part of the code where the old list is compared to the new list

    }

}
