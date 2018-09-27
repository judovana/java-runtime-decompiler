package com.redhat.thermostat.vm.decompiler.data;

public final class Directories {

    private final String xdgConfigSuffix = "/conf";
    private final String xdgJrdHome = "/.config/java-runtime-decompiler";


    /**
     * Locate configuration directory as per XDG base directory specification.
     * @return xdg config directory (e.g. ~/.config/java-runtime-decompiler/conf
     */
    public String getConfigDirectory() {

        String xdgJrdBase = getXdgJrdBaseDir();
        String xdgConf = xdgJrdBase + xdgConfigSuffix;
        return xdgConf;

    }

    /**
     * Returns specific xdg directory for the framework
     * @return xdg decompiler directory (e.g. ~/.config/java-runtime-decompiler)
     */
    public String getXdgJrdBaseDir() {
        String homeDir = System.getProperty("user.home");
        String res = System.getenv("XDG_CONFIG_HOME");
        if (homeDir.isEmpty() && (res == null || res.isEmpty()) ){
             /* fail here */
            throw new NullPointerException("No such a directory in the system!");
        }
        String xdgConfigHome;
        if (res == null || res.equals("")) {
            xdgConfigHome = homeDir + this.xdgJrdHome;
        } else {
            xdgConfigHome = res + this.xdgJrdHome;

        }
        return xdgConfigHome;
    }

}
