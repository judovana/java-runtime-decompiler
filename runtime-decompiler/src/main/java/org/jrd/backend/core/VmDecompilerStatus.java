package org.jrd.backend.core;



/**
 * This class stores all information about the state of decompiler plugin on 
 * each VM.
 */

public class VmDecompilerStatus{

    private String vmId;
    private long timestamp;

    private String hostname;
    private int listenPort;
    private String[] loadedClassNames;
    private String loadedClassBytes;
    private String bytesClassName;

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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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
