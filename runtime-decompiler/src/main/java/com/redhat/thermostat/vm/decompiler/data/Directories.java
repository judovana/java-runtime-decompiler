package com.redhat.thermostat.vm.decompiler.data;

public final class Directories {

    private final String xdgConfigSuffix = "/.config/java-runtime-decompiler/conf";

    /**
     * Locate configuration directory as per XDG base directory specification.
     * @return
     */
    public String getConfigDirectory(){

        String homeDir = System.getProperty("user.home");
        String res = System.getenv("XDG_CONFIG_HOME");
        String xdgConfigHome;
        if (res==null || res.equals("")){
            xdgConfigHome = homeDir + this.xdgConfigSuffix;
        }
        else{
            xdgConfigHome = res + this.xdgConfigSuffix;
        }
        if (homeDir.isEmpty() && (res == null || res.isEmpty()) ){
            /* fail here */
            throw new NullPointerException("No such a directory in the system!");
        }
        return xdgConfigHome;


    }
}
