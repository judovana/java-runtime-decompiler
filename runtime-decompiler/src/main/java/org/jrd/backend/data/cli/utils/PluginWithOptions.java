package org.jrd.backend.data.cli.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.decompiling.DecompilerWrapper;

public class PluginWithOptions {

    private final DecompilerWrapper decompiler;
    private final String[] options;

    public PluginWithOptions(DecompilerWrapper decompiler, String[] options) {
        this.decompiler = decompiler;
        this.options = options;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public DecompilerWrapper getDecompiler() {
        return decompiler;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String[] getOptions() {
        return options;
    }
}
