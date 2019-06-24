package org.jrd.backend.data;

import java.io.File;

public final class Directories {

    private static final String XDG_CONFIG_SUFFIX = File.separator + "conf";
    private static final String XDG_PLUGIN_SUFFIX = File.separator + "plugins";
    private static final String XDG_JRD_HOME = File.separator + ".config" + File.separator + "java-runtime-decompiler";

    private Directories(){
    }

    /**
     * Locate plugin directory as per XDG base directory specification.
     * @return xdg config directory (e.g. ~/.config/java-runtime-decompiler/conf
     */
    public static String getConfigDirectory() {
        return getXdgJrdBaseDir() + XDG_CONFIG_SUFFIX;
    }

    /**
     * Locate configuration directory as per XDG base directory specification.
     * @return xdg config directory (e.g. ~/.config/java-runtime-decompiler/conf
     */
    public static String getPluginDirectory() {
       return getXdgJrdBaseDir() + XDG_PLUGIN_SUFFIX;
    }

    /**
     * Returns specific xdg directory for the framework
     * @return xdg decompiler directory (e.g. ~/.config/java-runtime-decompiler)
     */
    public static String getXdgJrdBaseDir() {
        String homeDir = System.getProperty("user.home");
        String res = System.getenv("XDG_CONFIG_HOME");
        if (homeDir.isEmpty() && (res == null || res.isEmpty()) ){
             /* fail here */
            throw new NullPointerException("No such a directory in the system!");
        }
        String xdgConfigHome;
        if (res == null || res.equals("")) {
            xdgConfigHome = homeDir + XDG_JRD_HOME;
        } else {
            xdgConfigHome = res + XDG_JRD_HOME;

        }
        return xdgConfigHome;
    }

}
