package org.jrd.backend.data;

import org.jrd.backend.decompiling.PluginManager;

public class Model {

    private VmManager vmManager = new VmManager();
    private PluginManager pluginManager = new PluginManager();

    public VmManager getVmManager() {
        return vmManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

}
