package org.jrd.backend.data.cli.utils;

public class PluginWrapperWithMetaInfo {
    private final PluginWithOptions wrapper;
    private final boolean haveCompiler;

    public PluginWrapperWithMetaInfo(PluginWithOptions wrapper, boolean haveCompiler) {
        this.wrapper = wrapper;
        this.haveCompiler = haveCompiler;
    }

    public PluginWithOptions getWrapper() {
        return wrapper;
    }

    public boolean haveCompiler() {
        return haveCompiler;
    }
}
