package org.jrd.backend.data;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * This class is used for creating/removing/updating information about available Java Virtual Machines.
 */
public class VmManager{

    private HashSet<VmInfo> vmInfoSet;
    Set<ActionListener> actionListeners = new HashSet<>();
    boolean changed;

    public VmManager() {
        this.vmInfoSet = new HashSet<>();
        updateLocalVMs();


        Thread VMUpdateThread = new Thread(() -> {
            while (true){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    OutputController.getLogger().log(e);
                }
                updateLocalVMs();
            }
        });
        VMUpdateThread.setDaemon(true);
        VMUpdateThread.start();

    }

    /**
     * Obtains list of Virtual Machines.
     * This list is then compared to vmInfoSet. Old Vms are removed and new are added.
     */
    public void updateLocalVMs() {
        HashSet<VmInfo> newVmInfoSet = new HashSet<>();
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String vmId = descriptor.id();
            String processId = descriptor.id();
            int pid = Integer.parseInt(processId);
            String name = descriptor.displayName();
            newVmInfoSet.add(new VmInfo(vmId, pid, name, true));
        }

        // Add all new VMs.
        newVmInfoSet.forEach(vmInfo -> {
            boolean isNewVm = vmInfoSet.stream().noneMatch(vmInfo1 -> vmInfo1.getVmId().equals(vmInfo.getVmId()));
            if (isNewVm){
                setChanged();
                vmInfoSet.add(vmInfo);
            }
        });

        // Remove old VMs that are no longer available.
        Iterator<VmInfo> iterator = vmInfoSet.iterator();
        HashSet<VmInfo> forRemoval = new HashSet<>();
        while (iterator.hasNext()){
            VmInfo vmInfo = iterator.next();
            boolean noLongerExists = newVmInfoSet.stream().noneMatch(vmInfo1 -> vmInfo1.getVmId().equals(vmInfo.getVmId()));
            if (vmInfo.isLocal() && noLongerExists){
                setChanged();
                forRemoval.add(vmInfo);
            }
        }
        vmInfoSet.removeAll(forRemoval);

        notifyListeners();
    }

    public void createRemoteVM(String hostname, int port){
        String id = UUID.randomUUID().toString();
        VmInfo vmInfo = new VmInfo(id, -1, hostname, false);
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setVmId(id);
        status.setHostname(hostname);
        status.setListenPort(port);
        vmInfo.setVmDecompilerStatus(status);
        vmInfoSet.add(vmInfo);
        setChanged();
        notifyListeners();
    }

    public VmInfo findVmFromPID(String param) {
        int pid = Integer.valueOf(param);
        for (VmInfo vmInfo : vmInfoSet) {
            if (vmInfo.getVmPid() == pid) {
                return vmInfo;
            }
        }
        throw new RuntimeException("VM with pid of " + pid + " not found");
    }

    public VmInfo getVmInfoByID(String VmId){
        for (VmInfo vmInfo : vmInfoSet) {
            if (vmInfo.getVmId().equals(VmId)){
                return vmInfo;
            }
        }
        throw new NoSuchElementException("VmInfo with VmID" + VmId + "does no exist in VmList");
    }

    public HashSet<VmInfo> getVmInfoSet() {
        return this.vmInfoSet;
    }

    public void subscribeToVMChange(ActionListener listener){
        if (listener == null){
            throw new NullPointerException();
        }
        actionListeners.add(listener);
    }

    public void notifyListeners(){
        if (hasChanged()){
            clearChanged();
            for (ActionListener listener: actionListeners){
                listener.actionPerformed(new ActionEvent(this, 0, null));
            }
        }
    }

    private boolean hasChanged(){
        return changed;
    }

    private void setChanged(){
        changed = true;
    }

    private  void clearChanged(){
        changed = false;
    }
}
