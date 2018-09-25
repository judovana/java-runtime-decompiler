package com.redhat.thermostat.vm.decompiler.data;

public final class Directories {

    private final String xdgConfigSuffix = "/conf";
    private final String xdgJrdHome = "/.config/java-runtime-decompiler";

    public String getConfigDirectory() {

        String xdgJrdBase = getXdgJrdBaseDir();
        if (xdgJrdBase.equals("") || xdgJrdBase == null) {
            /* fail here */
            throw new NullPointerException("No such a directory in the system!");
        } else {
            String xdgConf = xdgJrdBase + xdgConfigSuffix;
            return xdgConf;
        }

    }

    public String getXdgJrdBaseDir() {
        String homeDir = System.getProperty("user.home");
        String res = System.getenv("XDG_CONFIG_HOME");
        String xdgConfigHome;
        if (res == null || res.equals("")) {
            xdgConfigHome = homeDir + this.xdgJrdHome;
        } else {
            xdgConfigHome = res + this.xdgJrdHome;

        }
        return xdgConfigHome;
    }

}
