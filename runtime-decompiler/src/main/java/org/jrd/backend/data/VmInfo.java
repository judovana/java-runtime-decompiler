package org.jrd.backend.data;

import org.jrd.backend.core.VmDecompilerStatus;

public class VmInfo {

    public VmDecompilerStatus getVmDecompilerStatus() {
        return vmDecompilerStatus;
    }

    public void setVmDecompilerStatus(VmDecompilerStatus vmDecompilerStatus) {
        this.vmDecompilerStatus = vmDecompilerStatus;
    }

    private VmDecompilerStatus vmDecompilerStatus;

    private String vmId;
    private int vmPid;
    private String vmName;
    private boolean local;

    public VmInfo(String VmId, int VmPid, String vmName) {
        this(VmId, VmPid, vmName, true);
    }

    public VmInfo(String VmId, int VmPid, String vmName, boolean local) {
        this.vmId = VmId;
        this.vmPid = VmPid;
        this.vmName = vmName;
        this.local = local;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
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