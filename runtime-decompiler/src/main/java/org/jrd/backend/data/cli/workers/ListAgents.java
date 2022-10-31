package org.jrd.backend.data.cli.workers;

import org.jrd.backend.core.agentstore.KnownAgent;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Lib;

import java.util.ArrayList;
import java.util.List;

import static org.jrd.backend.data.cli.CliSwitches.VERSIONS;

public class ListAgents {

    private final VmManager vmManager;
    private final boolean verbose;

    public ListAgents(boolean verbose, VmManager vmManager) {
        this.verbose = verbose;
        this.vmManager = vmManager;
    }

    public List<VmInfo> listAgents(List<String> params) {
        boolean versions = false;
        if (params.size() > 1) {
            String filteredSecondParam = CliUtils.cleanParameter(params.get(1));
            versions = filteredSecondParam.equals(VERSIONS);
        }
        return listAgents(versions);
    }

    private List<VmInfo> listAgents(boolean versions) {
        List<VmInfo> connections = new ArrayList<>();
        for (KnownAgent agent : KnownAgents.getInstance().getAgents()) {
            System.out.println(agent.toPrint());
            if (versions) {
                try {
                    Lib.HandhshakeResult vmInfo = Lib.handshakeAgent(agent, vmManager);
                    System.out.println("  - " + vmInfo.getAgentVersion());
                    System.out.println("  - " + vmInfo.getDiff());
                    connections.add(vmInfo.getVmInfo());
                } catch (Exception ex) {
                    if (verbose) {
                        ex.printStackTrace();
                    }
                    System.out.println("  - unknown version. Most likely old agent. Should work for most cases though.");
                }
            }
        }
        return connections;
    }
}
