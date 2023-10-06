package org.jrd.backend.data;

import org.jrd.backend.decompiling.PluginManager;

public final class Model {

    private VmManager vmManager = new VmManager();
    private PluginManager pluginManager = new PluginManager();

    private static final Model MODEL = new Model();

    private Model() {
    }

    public VmManager getVmManager() {
        return vmManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public static Model getModel() {
        return MODEL;
    }
}
