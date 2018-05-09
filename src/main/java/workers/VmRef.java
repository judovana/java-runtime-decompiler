/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

/**
 *
 * @author pmikova
 */
public class VmRef {

    private final String id;
    private final Integer pid;
    private final String name;

    public VmRef( String id, Integer pid, String name) {
        this.id = id;
        this.pid = pid;
        this.name = name;
    }

    public VmRef(VmInfo vmInfo) {
        this.id = vmInfo.getVmId();
        this.pid = vmInfo.getVmPid();
        this.name = vmInfo.getMainClass();
    }


    @Override
    public String toString() {
        return this.pid + " - " + this.name;
    }

    
    public String getVmId() {
        return id;
    }
    
    public Integer getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        VmRef other = (VmRef) obj;
        if (equals(this.id, other.id)
                && equals(this.pid, other.pid) && equals(this.name, other.name)) {
            return true;
        }
        return false;
    }

    private static boolean equals(Object obj1, Object obj2) {
        return (obj1 == null && obj2 == null) || (obj1 != null && obj1.equals(obj2));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getStringID() {
        return id;
    }
}