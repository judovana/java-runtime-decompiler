package org.jrd.backend.data;

public final class Directories {

    private static final String xdgConfigSuffix = "/conf";
    private static final String xdgJrdHome = "/.config/java-runtime-decompiler";

    private Directories(){
    }

    /**
     * Locate configuration directory as per XDG base directory specification.
     * @return xdg config directory (e.g. ~/.config/java-runtime-decompiler/conf
     */
    public static String getConfigDirectory() {

        String xdgJrdBase = getXdgJrdBaseDir();
        String xdgConf = xdgJrdBase + xdgConfigSuffix;
        return xdgConf;

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
            xdgConfigHome = homeDir + xdgJrdHome;
        } else {
            xdgConfigHome = res + xdgJrdHome;

        }
        return xdgConfigHome;
    }

}
