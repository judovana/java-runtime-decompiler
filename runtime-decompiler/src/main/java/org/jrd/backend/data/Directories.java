package org.jrd.backend.data;

import org.jrd.backend.core.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Directories {

    private static final String XDG_JRD_SUFFIX = File.separator + "java-runtime-decompiler";
    private static final String XDG_CONFIG_SUFFIX = File.separator + "conf";
    private static final String XDG_PLUGIN_SUFFIX = File.separator + "plugins";
    private static final String XDG_JRD_HOME = File.separator + ".config" + XDG_JRD_SUFFIX;

    private static int jrdLlocationFalback = 0;

    private Directories() {
    }

    /**
     * Locate configuration directory as per XDG base directory specification.
     *
     * @return xdg config directory (e.g. ~/.config/java-runtime-decompiler/conf
     */
    public static String getConfigDirectory() {
        return getXdgJrdBaseDir() + XDG_CONFIG_SUFFIX;
    }

    /**
     * Locate configuration directory as per XDG base directory specification.
     * @return xdg plugin directory (e.g. ~/.config/java-runtime-decompiler/plugins
     */
    public static String getPluginDirectory() {
        return getXdgJrdBaseDir() + XDG_PLUGIN_SUFFIX;
    }

    /**
     * Returns specific xdg directory for the framework
     * @return xdg decompiler directory (e.g. ~/.config/java-runtime-decompiler)
     */
    public static String getXdgJrdBaseDir() {
        if (isPortable()) {
            return getJrdLocation() + File.separator + "config";
        } else {
            String homeDir = System.getProperty("user.home");
            String res = System.getenv("XDG_CONFIG_HOME");
            String xdgConfigHome;

            if (homeDir.isEmpty() && (res == null || res.isEmpty())) {
                /* fail here */
                throw new NullPointerException("No such a directory in the system!");
            }
            if (res == null || "".equals(res)) {
                xdgConfigHome = homeDir + XDG_JRD_HOME;
            } else {
                xdgConfigHome = res + XDG_JRD_SUFFIX;
            }

            return xdgConfigHome;
        }
    }

    public static String getJrdLocation() {
        if (System.getProperty("jrd.location") == null) {
            if (jrdLlocationFalback < 10) {
                if (Logger.getLogger().isVerbose()) {
                    jrdLlocationFalback++;
                }
                if (jrdLlocationFalback == 9) {
                    Logger.getLogger().log(Logger.Level.DEBUG, "jrd.location variable not found will no longer be reported");
                }
                Logger.getLogger().log(Logger.Level.DEBUG, "jrd.location environment variable not found, using fallback");
            }
            return Paths.get(".").normalize().toAbsolutePath().toString();
        } else {
            return System.getProperty("jrd.location");
        }
    }

    public static File getFreshlyBuiltAgent() {
        return new File(
                getJrdLocation() + File.separator + "decompiler_agent" + File.separator + "target" + File.separator + getAgentFile()
        );
    }

    public static File getPotentialAgentLocation() {
        if (isPortable()) {
            return new File(getJrdLocation() + File.separator + "libs" + File.separator + getAgentFile());
        } else {
            return getFreshlyBuiltAgent();
        }
    }

    public static String getRelativePotentialAgentLocation() {
        return getPotentialAgentLocation().getAbsolutePath().replace(getJrdLocation(), ".");
    }

    private static String getAgentFile() {
        return "decompiler-agent-" + MetadataProperties.getInstance().getVersion() + ".jar";
    }

    public static boolean isPortable() {
        String purpose = System.getProperty("jrd.purpose");
        return "PORTABLE".equals(purpose);
    }

    public static boolean isOsWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    public static void createPluginDirectory() {
        File pluginDir = new File(getPluginDirectory());

        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                Logger.getLogger().log(Logger.Level.ALL, "Unable to create plugin directory '" + pluginDir.getAbsolutePath() + "'.");
            }
        }
    }

    public static void deleteWithException(String stringPath) {
        try {
            Files.delete(Path.of(stringPath));
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
    }
}
