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
//import com.redhat.thermostat.storage.model.AgentInformation;
import java.util.Map;
import java.util.Set;

import java.util.HashMap;

public class VmInfo {

    public VmInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum AliveStatus {
        RUNNING,
        EXITED,
        /**
         * We don't know what the status of this VM is. Possible cause: agent
         * was shut down before the VM was.
         */
        UNKNOWN,
    }

    public static class KeyValuePair {

        private String key;
        private String value;

        public KeyValuePair() {
            this(null, null);
        }

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    private String vmId;
    private int vmPid = 0;
    private long startTime = System.currentTimeMillis();
    private long stopTime = Long.MIN_VALUE;
    private String javaVersion = "unknown";
    private String javaHome = "unknown";
    private String javaCommandLine = "unknown";
    private String mainClass = "unknown";
    private String vmName = "unknown";
    private String vmInfo = "unknown";
    private String vmVersion = "unknown";
    private String vmArguments = "unknown";
    private Map<String, String> properties = new HashMap<String, String>();
    private Map<String, String> environment = new HashMap<String, String>();
    private String[] loadedNativeLibraries;
    private long uid;
    private String username;

    public VmInfo(String vmId, int vmPid, long startTime, long stopTime,
            String javaVersion, String javaHome,
            String mainClass, String commandLine,
            String vmName, String vmInfo, String vmVersion, String vmArguments,
            Map<String, String> properties, Map<String, String> environment, String[] loadedNativeLibraries,
            long uid, String username) {

        this.vmId = vmId;
        this.vmPid = vmPid;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.javaVersion = javaVersion;
        this.javaHome = javaHome;
        this.mainClass = mainClass;
        this.javaCommandLine = commandLine;
        this.vmName = vmName;
        this.vmInfo = vmInfo;
        this.vmVersion = vmVersion;
        this.vmArguments = vmArguments;
        this.properties = properties;
        this.environment = environment;
        this.loadedNativeLibraries = loadedNativeLibraries;
        this.uid = uid;
        this.username = username;
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

    public long getStartTimeStamp() {
        return startTime;
    }

    public void setStartTimeStamp(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTimeStamp() {
        return stopTime;
    }

    public void setStopTimeStamp(long stopTime) {
        this.stopTime = stopTime;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    /**
     * If java is invoked as {@code java -jar foo.jar}, then the main class name
     * is {@code foo.jar}.
     */
    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getJavaCommandLine() {
        return javaCommandLine;
    }

    public void setJavaCommandLine(String javaCommandLine) {
        this.javaCommandLine = javaCommandLine;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmArguments() {
        return vmArguments;
    }

    public void setVmArguments(String vmArguments) {
        this.vmArguments = vmArguments;
    }

    public String getVmInfo() {
        return vmInfo;
    }

    public void setVmInfo(String vmInfo) {
        this.vmInfo = vmInfo;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public void setVmVersion(String vmVersion) {
        this.vmVersion = vmVersion;
    }

    /**
     * @deprecated This can incorrectly show a VM as running when the actual
     * status is unknown. Use {@link #isAlive(AgentInformation)} instead.
     */
    @Deprecated
    public boolean isAlive() {
        return getStartTimeStamp() > getStopTimeStamp();
    }
/*
    public AliveStatus isAlive(AgentInformation agentInfo) {
        if (agentInfo.isAlive()) {
            return (isAlive() ? AliveStatus.RUNNING : AliveStatus.EXITED);
        } else {
            return (isAlive() ? AliveStatus.UNKNOWN : AliveStatus.EXITED);
        }
    }
*/
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public KeyValuePair[] getPropertiesAsArray() {
        return getMapAsArray(properties);
    }

    public void setPropertiesAsArray(KeyValuePair[] properties) {
        this.properties = getArrayAsMap(properties);
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public KeyValuePair[] getEnvironmentAsArray() {
        return getMapAsArray(environment);
    }

    public void setEnvironmentAsArray(KeyValuePair[] environment) {
        this.environment = getArrayAsMap(environment);
    }

    private KeyValuePair[] getMapAsArray(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Set<String> keys = map.keySet();
        KeyValuePair[] tuples = new KeyValuePair[keys.size()];
        int i = 0;
        for (String key : keys) {
            tuples[i] = new KeyValuePair(key, map.get(key));
            i++;
        }
        return tuples;
    }

    private Map<String, String> getArrayAsMap(KeyValuePair[] tuples) {
        if (tuples == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (KeyValuePair tuple : tuples) {
            map.put(tuple.getKey(), tuple.getValue());
        }
        return map;
    }

    public String[] getLoadedNativeLibraries() {
        return loadedNativeLibraries;
    }

    public void setLoadedNativeLibraries(String[] loadedNativeLibraries) {
        this.loadedNativeLibraries = loadedNativeLibraries;
    }

    /**
     * Returns the system user id for the owner of this JVM process, or -1 if an
     * owner could not be found.
     */
    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    /**
     * Returns the system user name for the owner of this JVM process, or null
     * if an owner could not be found.
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
