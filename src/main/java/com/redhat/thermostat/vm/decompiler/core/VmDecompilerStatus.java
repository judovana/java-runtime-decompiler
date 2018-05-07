package com.redhat.thermostat.vm.decompiler.core;



/**
 * This class stores all information about the state of decompiler plugin on 
 * each VM.
 */

public class VmDecompilerStatus{

    private String vmId;
    private long timestamp;
    private int listenPort;
    String[] loadedClassNames;
    String loadedClassBytes;
    String bytesClassName;

    public VmDecompilerStatus() {
        this.bytesClassName = "";
        this.loadedClassBytes = "";
        this.loadedClassNames = new String[]{};
    }

    
    public String getBytesClassName(){
        return bytesClassName;
    }
    
    public void setBytesClassName(String bytesClassName){
        this.bytesClassName = bytesClassName;
    }
    
    
    public String getVmId() {
        return vmId;
    }

    
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    
    public void setListenPort(int port) {
        this.listenPort = port;
    }

    
    public int getListenPort() {
        return listenPort;
    }

    
    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    
    public void setLoadedClassNames(String[] loadedClassNames) {
        this.loadedClassNames = loadedClassNames;

    }

    
    public String getLoadedClassBytes() {
        return loadedClassBytes;
    }

    
    public String[] getLoadedClassNames() {
        return loadedClassNames;
    }

    
    public void setLoadedClassBytes(String value) {
        loadedClassBytes = value;
    }

}
