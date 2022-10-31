package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;

import java.util.List;

public class InitClass {

    private final List<String> filteredArgs;
    private final VmManager vmManager;

    public InitClass(List<String> filteredArgs, VmManager vmManager) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
    }

    public VmInfo init() throws Exception {
        if (filteredArgs.size() != 3) {
            throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.INIT_FORMAT + "'.");
        }
        String fqn = filteredArgs.get(2);
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        Lib.initClass(vmInfo, vmManager, fqn, System.out);
        return vmInfo;
    }

}
