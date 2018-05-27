package com.redhat.thermostat.vm.decompiler.data;

public class VmInfo {

    private String vmId;
    private int vmPid;
    private String vmName;

    public VmInfo(String VmId, int VmPid, String vmName) {
        this.vmId = VmId;
        this.vmPid = VmPid;
        this.vmName = vmName;
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
}