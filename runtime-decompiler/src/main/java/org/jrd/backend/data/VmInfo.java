package org.jrd.backend.data;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stores information about Available Virtual Machine.
 */
public class VmInfo {

    public static enum  Type{
        LOCAL, REMOTE, FS
    }

    private VmDecompilerStatus vmDecompilerStatus;
    private String vmId;
    private int vmPid;
    private String vmName;
    private Type type;
    private java.util.List<File> cp;


    /**
     * Stores information about Available Virtual Machine.
     * @param vmId Unique ID for this VmInfo
     * @param vmPid Virtual Machine process ID
     * @param vmName Name for the Virtual Machine. Hostname for remote VMs
     * @param type local, remote, type
     */
    public VmInfo(String vmId, int vmPid, String vmName, Type type, List<File> cp) {
        setVmId(vmId);
        setVmPid(vmPid);
        setVmName(vmName);
        setType(type);
        setCp(cp);
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

    public Type getType() {
        return type;
    }

    public void setType(Type local) {
        this.type = local;
    }

    @SuppressFBWarnings(
            value = "NP_LOAD_OF_KNOWN_NULL_VALUE",
            justification = "Classpath is only used for FS VMs, in other cases getCp() does not get called, thus null is permissible."
    )
    public void setCp(List<File> cp) {
        if (cp == null){
            this.cp = cp;
        } else {
            this.cp = Collections.unmodifiableList(cp);
        }
    }

    public List<File> getCp() {
        return cp;
    }

    private String getCpString() {
        return cp.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    }

    public String nameOrCp() {
        if (cp == null) {
            return getVmName();
        } else {
            if (getVmName() != null && !getVmName().trim().isEmpty()){
                return getVmName();
            } else {
                return getCpString();
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%s %s (type %s",
                vmId, vmName, type
        ) + (type == Type.FS ? ", classpath: " + getCpString() : "" ) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VmInfo vmInfo = (VmInfo) o;
        return vmPid == vmInfo.vmPid &&
                Objects.equals(vmId, vmInfo.vmId) &&
                Objects.equals(vmName, vmInfo.vmName) &&
                type == vmInfo.type &&
                Objects.equals(cp, vmInfo.cp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vmId, vmPid, vmName, type, cp);
    }
}