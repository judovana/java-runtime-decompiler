package org.jrd.backend.decompiling;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Directories;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

public final class ExpandableUrl {

    public static class MalformedUrlToPath extends RuntimeException {
        public MalformedUrlToPath(Throwable cause) {
            super(cause);
        }
    }

    public static class MalformedMacroExpansion extends RuntimeException {
        public MalformedMacroExpansion(Throwable cause) {
            super(cause);
        }
    }

    private String path;

    private ExpandableUrl(String s) {
        String expandedPath = expandEnvVars(s);
        if (!new File(expandedPath).exists()) {
            String message = s.isEmpty() ? "Filename empty." : expandedPath;
            Logger.getLogger().log(Logger.Level.ALL, new FileNotFoundException(message));
        }
        this.path = s;
    }

    public static ExpandableUrl createFromPath(String path) {
        return new ExpandableUrl(collapseEnvVars(path));
    }

    public static ExpandableUrl createFromStringUrl(String url) throws MalformedMacroExpansion {
        // Backward compatibility - prior to JRD 2.0.0 URLs were stored without file protocol prefix in the .json file
        if (!url.startsWith("file:")) {
            url = prependFileProtocol(url);
        }

        try {
            return createFromPath(new URL(expandEnvVars(url, true)).getPath());
        } catch (MalformedURLException e) {
            throw new MalformedMacroExpansion(e);
        }
    }

    /*
     * There were many differences in file protocol handling
     * between jdk8 and jdk11. Especially on Windows, Where
     * redundant / could auto point to C:\ or simply kill the path
     * Although this method should be of signature URL:(File),
     * it was left as String String, as the slashes are making us mad.
     */
    protected static String prependFileProtocol(String url) {
        return "file:" + url;
    }

    static String expandEnvVars(String path) {
        return expandEnvVars(path, true);
    }

    static String expandEnvVars(String path, boolean prependSlash) {
        String pluginDir = unifySlashes(Directories.getXdgJrdBaseDir(), prependSlash);
        String homeDir = unifySlashes(System.getProperty("user.home"), prependSlash);
        String jrdDir = unifySlashes(Directories.getJrdLocation(), prependSlash);

        path = path.replace("${JRD}", jrdDir);
        path = path.replace("${XDG_CONFIG_HOME}", pluginDir);
        path = path.replace("${HOME}", homeDir);

        return path;
    }

    private static String collapseEnvVars(String path) {
        String pluginDir = unifySlashes(Directories.getXdgJrdBaseDir());
        String homeDir = unifySlashes(System.getProperty("user.home"));
        String jrdDir = unifySlashes(Directories.getJrdLocation());

        return collapseEnvVars(unifySlashes(path), homeDir, pluginDir, jrdDir);
    }

    static String collapseEnvVars(String path, String home, String xdgConfigHome, String jrd) {
        path = path.replace(jrd, "${JRD}");
        path = path.replace(xdgConfigHome, "${XDG_CONFIG_HOME}");
        path = path.replace(home, "${HOME}");

        return path;
    }

    public static String unifySlashes(String dir) {
        return unifySlashes(dir, true);
    }

    public static String unifySlashes(String dir, boolean prependSlash) {
        dir = dir.replaceAll("\\\\", "/");
        if (prependSlash && Directories.isOsWindows() &&
            !dir.startsWith("file") && dir.length() > 0 && dir.charAt(0) != '/' && dir.charAt(0) != '$'
        ) {
            dir = "/" + dir;
        }

        return dir;
    }

    public URL getExpandedUrl() throws MalformedURLException {
        return new URL("file", "", expandEnvVars(this.path, false));
    }

    public String getRawUrl() {
        try {
            return collapseEnvVars(new URL("file", "", expandEnvVars(this.path)).toString());
        } catch (MalformedURLException e) {
            throw new MalformedMacroExpansion(e);
        }
    }

    public String getExpandedPath() {
        try {
            return getExpandedUrl().getPath();
        } catch (MalformedURLException e) {
            throw new MalformedUrlToPath(e);
        }
    }

    public String getRawPath() { // ${HOME}/path
        return collapseEnvVars(path);
    }

    public File getFile() {
        return new File(getExpandedPath());
    }

    @Override
    public String toString() {
        return path;
    }
}
