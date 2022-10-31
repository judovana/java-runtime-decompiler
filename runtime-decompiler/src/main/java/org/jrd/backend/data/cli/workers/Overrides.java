package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;

import java.util.List;

public class Overrides {

    private final List<String> filteredArgs;
    private final VmManager vmManager;

    public Overrides(List<String> filteredArgs, VmManager vmManager) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
    }

    public VmInfo removeOverrides() {
        if (filteredArgs.size() != 3) {
            throw new RuntimeException("expected two params: " + Help.REMOVE_OVERRIDES_FORMAT);
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        String regex = filteredArgs.get(2);
        String[] was = Lib.obtainOverrides(vmInfo, vmManager);
        Lib.removeOverrides(vmInfo, vmManager, regex);
        String[] is = Lib.obtainOverrides(vmInfo, vmManager);
        System.out.println("Removed: " + (was.length - is.length) + " (was: " + was.length + ", is: " + is.length + ")");
        return vmInfo;
    }

    public VmInfo listOverrides() {
        if (filteredArgs.size() != 2) {
            throw new RuntimeException("expected two params: " + Help.LIST_OVERRIDES_FORMAT);
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        String[] overrides = Lib.obtainOverrides(vmInfo, vmManager);
        for (String override : overrides) {
            System.out.println(override);
        }
        System.out.println("Total: " + overrides.length);
        return vmInfo;

    }
}
