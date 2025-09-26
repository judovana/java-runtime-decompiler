package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.utils.Saving;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.jrd.backend.data.cli.CliSwitches.LIST_JVMS;

public class ListJvms {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final VmManager vmManager;

    public ListJvms(List<String> filteredArgs, Saving saving, VmManager vmManager) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.vmManager = vmManager;
    }

    public void listJvms() throws IOException {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_JVMS + " does not expect arguments.");
        }

        PrintStream out = System.out;
        try {
            if (saving != null && saving.getAs() != null) {
                out = saving.openPrintStream();
            }

            for (VmInfo vmInfo : vmManager.getVmInfoSet()) {
                out.println(vmInfo.getVmPid() + " " + vmInfo.getVmName());
            }

            out.flush();
        } finally {
            if (saving != null && saving.getAs() != null) {
                out.close();
            }
        }
    }
}
