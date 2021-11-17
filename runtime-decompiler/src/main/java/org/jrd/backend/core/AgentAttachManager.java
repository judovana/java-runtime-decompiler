package org.jrd.backend.core;

import org.jrd.backend.core.agentstore.KnownAgent;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.VmManager;

import java.util.List;

/**
 * Attach manager for agent contains utility methods and information about
 * attach.
 */
public class AgentAttachManager {

    private AgentLoader loader;
    private VmManager vmManager;

    public AgentAttachManager(VmManager vmManager) {
        this.vmManager = vmManager;
        this.loader = new AgentLoader();
    }

    public VmDecompilerStatus attachAgentToVm(String vmId, int vmPid) {
        List<KnownAgent> knownAgentsOnPid = KnownAgents.getInstance().findAgents(vmPid);
        if (knownAgentsOnPid.size() == 1) {
            System.err.println("reusing " + knownAgentsOnPid.get(0).toString());
            VmDecompilerStatus status = new VmDecompilerStatus(true);
            status.setHostname(knownAgentsOnPid.get(0).getHost());
            status.setListenPort(knownAgentsOnPid.get(0).getPort());
            status.setVmId(vmId);
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
            return status;
        } else {
            Logger.getLogger().log(Logger.Level.DEBUG, "Attaching agent to VM '" + vmPid + "'");
            int attachedPort = AgentLoader.INVALID_PORT;
            try {
                attachedPort = loader.attach(vmPid);
            } catch (Exception ex) {
                Logger.getLogger().log(Logger.Level.ALL, ex);
            }
            if (attachedPort == AgentLoader.INVALID_PORT) {
                Logger.getLogger().log(Logger.Level.DEBUG, "Failed to attach agent for VM '" + vmPid);
                return null;
            }
            VmDecompilerStatus status = new VmDecompilerStatus();
            status.setHostname("localhost");
            status.setListenPort(attachedPort);
            status.setVmId(vmId);
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
            return status;
        }
    }
}
