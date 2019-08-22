package org.jrd.backend.core;

import org.jrd.backend.data.VmManager;

/**
 * Attach manager for agent contains utility methods and information about 
 * attach.
 */
public class AgentAttachManager {
 
    //private static final Logger logger = LoggingUtils.getLogger(AgentAttachManager.class); 
    private AgentLoader loader;
    private VmManager vmManager;

      
    public AgentAttachManager(VmManager vmManager){
        this.vmManager = vmManager;
        this.loader = new AgentLoader();
        
    }
    
     void setAttacher(AgentLoader loader) {
        this.loader = loader;
    }

    void setVmManager(VmManager vmManager) {
        this.vmManager = vmManager;
    }
  

    VmDecompilerStatus attachAgentToVm(String vmId, int vmPid)  {
        OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "Attaching agent to VM '" + vmPid + "'");
        int attachedPort = AgentLoader.INVALID_PORT;
        try {
            attachedPort = loader.attach(vmId, vmPid);
        } catch (Exception ex) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, ex);
        }
        if (attachedPort == AgentLoader.INVALID_PORT) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "Failed to attach agent for VM '" + vmPid);
            return null;
        }
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setHostname("localhost");
        status.setListenPort(attachedPort);
        status.setVmId(vmId);
        status.setTimeStamp(System.currentTimeMillis());
        vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        return status;
    }
}

    

