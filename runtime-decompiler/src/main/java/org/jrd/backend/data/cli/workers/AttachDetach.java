package org.jrd.backend.data.cli.workers;

import org.jrd.backend.communication.CallDecompilerAgent;
import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.AgentConfig;

import java.util.List;

public class AttachDetach {

    private final List<String> filteredArgs;
    private final VmManager vmManager;

    public AttachDetach(List<String> filteredArgs, VmManager vmManager) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
    }

    public VmInfo attach(AgentConfig agntConfig) throws Exception {
        final int mandatoryParam = 2;
        if (filteredArgs.size() < mandatoryParam) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.ATTACH_FORMAT + "'.");
        }
        if (CliUtils.guessType(filteredArgs.get(1)) != VmInfo.Type.LOCAL) {
            throw new IllegalArgumentException("Sorry, first argument must be running jvm PID, nothing else.");
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        VmDecompilerStatus status = new AgentAttachManager(vmManager).attachAgentToVm(vmInfo.getVmId(), vmInfo.getVmPid(), agntConfig);
        System.out.println("Attached. Listening on: " + status.getListenPort());
        return vmInfo;
    }

    public void detach() {
        if (filteredArgs.size() < 2) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.DETACH_FORMAT + "'.");
        }
        if (filteredArgs.get(1).contains(":")) {
            String[] hostPort = filteredArgs.get(1).split(":");
            Lib.detach(hostPort[0], Integer.parseInt(hostPort[1]), vmManager);
        } else {
            //TODO is pid? If so, detach its port, else localhost
            detach(Integer.parseInt(filteredArgs.get(1)));
        }
    }

    private void detach(int port) {
        detachLocalhost(port, vmManager);
    }

    public static void detachLocalhost(int port, VmManager vmManager) {
        Lib.detach(CallDecompilerAgent.DEFAULT_ADDRESS, port, vmManager);
    }

}
