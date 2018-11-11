package org.jrd.backend.data;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;

/**
 * Stores information about Available Virtual Machine.
 */
public class VmInfo {

    private VmDecompilerStatus vmDecompilerStatus;
    private String vmId;
    private int vmPid;
    private String vmName;
    private boolean local;

    /**
     * Stores information about Available Virtual Machine.
     * @param vmId Unique ID for this VmInfo
     * @param vmPid Virtual Machine process ID
     * @param vmName Name for the Virtual Machine. Hostname for remote VMs
     * @param local True - Local VM. False Remote VM
     */
    public VmInfo(String vmId, int vmPid, String vmName, boolean local) {
        setVmId(vmId);
        setVmPid(vmPid);
        setVmName(vmName);
        setLocal(local);
    }

    public VmDecompilerStatus getVmDecompilerStatus() {
        return vmDecompilerStatus;
    }

    public void setVmDecompilerStatus(VmDecompilerStatus vmDecompilerStatus) {
        this.vmDecompilerStatus = vmDecompilerStatus;
    }

    public void removeVmDecompilerStatus() {
        this.vmDecompilerStatus = null;
    }

    public void replaceVmDecompilerStatus(VmDecompilerStatus status) {
        if (getVmId().equals(status.getVmId())){
            setVmDecompilerStatus(status);
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG,
                    "Old and new VmDecompilerStatus id does not match!");
        }
    }

    public String getVmId() {
        return vmId;
    }

    private void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public int getVmPid() {
        return vmPid;
    }

    public void setVmPid(int vmPid) {
        this.vmPid = vmPid;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}