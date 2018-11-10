package org.jrd.backend.data;

import org.jrd.backend.core.VmDecompilerStatus;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * This class is used for creating/removing/updating information about available Java Virtual Machines.
 */
public class VmManager {

    private ArrayList<VmInfo> vmInfoList;

    private ActionListener updateVmListsListener;

    public VmManager() {
        this.vmInfoList = new ArrayList<>();
        createLocalVMs();
    }

    private void createLocalVMs() {
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String vmId = descriptor.id();
            String processId = descriptor.id();
            int pid = Integer.parseInt(processId);
            String name = descriptor.displayName();
            vmInfoList.add(new VmInfo(vmId, pid, name, true));
        }

    }

    public void createRemoteVM(String hostname, int port){
        String id = UUID.randomUUID().toString();
        VmInfo vmInfo = new VmInfo(id, -1, hostname, false);
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setVmId(id);
        status.setHostname(hostname);
        status.setListenPort(port);
        vmInfo.setVmDecompilerStatus(status);
        vmInfoList.add(vmInfo);
        updateLists();
    }

    public VmInfo findVmFromPID(String param) {
        int pid = Integer.valueOf(param);
        for (VmInfo vmInfo : vmInfoList) {
            if (vmInfo.getVmPid() == pid) {
                return vmInfo;
            }
        }
        throw new RuntimeException("VM with pid of " + pid + " not found");
    }

    public VmInfo getVmInfoByID(String VmId){
        for (VmInfo vmInfo : vmInfoList) {
            if (vmInfo.getVmId().equals(VmId)){
                return vmInfo;
            }
        }
        throw new NoSuchElementException("VmInfo with VmID" + VmId + "does no exist in VmList");
    }

    public ArrayList<VmInfo> getVmInfoList() {
        return this.vmInfoList;
    }

    public void setUpdateVmListsListener(ActionListener listener){
        updateVmListsListener = listener;
    }

    public void updateLists(){
        if (updateVmListsListener != null){
            updateVmListsListener.actionPerformed(new ActionEvent(this, 0, null));
        }
    }
}
