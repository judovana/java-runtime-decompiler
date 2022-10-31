package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Saving;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.utility.AgentApiGenerator;

import java.io.PrintStream;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Api {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final VmManager vmManager;
    private final PluginManager pluginManager;

    public Api(List<String> filteredArgs, Saving saving, VmManager vmManager, PluginManager pluginManager) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.vmManager = vmManager;
        this.pluginManager = pluginManager;
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "The stream is clsoed as conditionally as is created")
    public VmInfo api() throws Exception {
        PrintStream out = System.out;
        try {
            if (saving != null && saving.getAs() != null) {
                out = saving.openPrintStream();
            }

            if (filteredArgs.size() != 2) {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.API_FORMAT + "'.");
            }

            VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
            AgentApiGenerator.initItems(vmInfo, vmManager, pluginManager);
            out.println(AgentApiGenerator.getInterestingHelp());
            out.flush();
            return vmInfo;
        } finally {
            if (saving != null && saving.getAs() != null) {
                out.close();
            }
        }
    }
}
