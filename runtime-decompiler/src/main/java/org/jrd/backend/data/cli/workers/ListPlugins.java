package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.jrd.backend.data.cli.CliSwitches.LIST_PLUGINS;

public class ListPlugins {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final PluginManager pluginManager;

    public ListPlugins(List<String> filteredArgs, Saving saving, PluginManager pluginManager) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.pluginManager = pluginManager;
    }

    public void listPlugins() throws IOException {
        if (filteredArgs.size() != 1) {
            throw new RuntimeException(LIST_PLUGINS + " does not expect arguments.");
        }

        PrintStream out = System.out;
        try {
            if (saving != null && saving.getAs() != null) {
                out = saving.openPrintStream();
            }

            for (DecompilerWrapper dw : pluginManager.getWrappers()) {
                out.printf(
                        "%s %s/%s - %s%n", dw.getName(), dw.getScope(), CliUtils.invalidityToString(dw.isInvalidWrapper()),
                        dw.getFileLocation()
                );
            }

            out.flush();
        } finally {
            if (saving != null && saving.getAs() != null) {
                out.close();
            }
        }
    }
}
