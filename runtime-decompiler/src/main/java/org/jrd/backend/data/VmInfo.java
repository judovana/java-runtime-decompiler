package org.jrd.backend.data;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stores information about Available Virtual Machine.
 */
public class VmInfo implements Serializable {

    public enum Type {
        LOCAL, REMOTE, FS
    }

    private static final long serialVersionUID = 111L;

    private transient VmDecompilerStatus vmDecompilerStatus;
    private String vmId;
    private int vmPid;
    private String vmName;
    private Type type;
    private java.util.List<File> cp;

    private static final Comparator<VmInfo> HOSTNAME_COMPARATOR = Comparator.comparing(
            info -> info.getVmDecompilerStatus().getHostname(), String::compareTo
    );
    private static final Comparator<VmInfo> PORT_COMPARATOR = Comparator.comparingInt(
            info -> info.getVmDecompilerStatus().getListenPort()
    );
    public static final Comparator<VmInfo> LOCAL_VM_COMPARATOR = Comparator.comparingInt(VmInfo::getVmPid);
    public static final Comparator<VmInfo> REMOTE_VM_COMPARATOR = HOSTNAME_COMPARATOR.thenComparing(PORT_COMPARATOR);
    public static final Comparator<VmInfo> FS_VM_COMPARATOR = LOCAL_VM_COMPARATOR.reversed();

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
        if (getVmId().equals(status.getVmId())) {
            setVmDecompilerStatus(status);
        } else {
            Logger.getLogger().log(Logger.Level.DEBUG, "Old and new VmDecompilerStatus IDs do not match!");
        }
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

    public Type getType() {
        return type;
    }

    public void setType(Type local) {
        this.type = local;
    }

    @SuppressFBWarnings(
            value = "NP_LOAD_OF_KNOWN_NULL_VALUE",
            justification = "Classpath is only used for FS VMs, in other cases getCp() does not get called"
    )
    public void setCp(List<File> cp) {
        if (cp == null) {
            this.cp = null;
        } else {
            this.cp = Collections.unmodifiableList(cp);
        }
    }

    public List<File> getCp() {
        return Collections.unmodifiableList(cp);
    }

    public String getCpString() {
        return cp.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    }

    public boolean hasName() {
        return getVmName() != null && !getVmName().trim().isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
                "%s %s (type %s",
                vmId, vmName, type
        ) + (type == Type.FS ? ", classpath: " + getCpString() : "") + ")";
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

    private byte[] serialize() throws IOException {
        try (
                ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
                ObjectOutputStream so = new ObjectOutputStream(bo)
        ) {
            so.writeObject(this);
            so.flush();
            return bo.toByteArray();
        }
    }

    String base64Serialize() throws IOException {
        return Base64.getEncoder().encodeToString(serialize());
    }

    static VmInfo base64Deserialize(String base64Representation) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                Base64.getDecoder().decode(base64Representation)
        ));
        return (VmInfo) ois.readObject();
    }
}
