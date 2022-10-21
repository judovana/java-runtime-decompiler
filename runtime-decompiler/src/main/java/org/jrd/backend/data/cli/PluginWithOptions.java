package org.jrd.backend.data.cli;

import org.jrd.backend.decompiling.DecompilerWrapper;

class PluginWithOptions {

    private final DecompilerWrapper decompiler;
    private final String[] options;

    PluginWithOptions(DecompilerWrapper decompiler, String[] options) {
        this.decompiler = decompiler;
        this.options = options;
    }

    DecompilerWrapper getDecompiler() {
        return decompiler;
    }

    String[] getOptions() {
        return options;
    }
}
