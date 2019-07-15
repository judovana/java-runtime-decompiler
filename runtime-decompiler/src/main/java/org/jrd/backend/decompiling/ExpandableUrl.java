package org.jrd.backend.decompiling;

import org.jrd.backend.data.Directories;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ExpandableUrl {
    public static class MalformedURLToPath extends RuntimeException{
        public MalformedURLToPath(Throwable cause) {
            super(cause);
        }
    }

    private String url;

    public ExpandableUrl(String urlString) {
        this.url = urlString;
    }

    static String prependFileProtocolIfNone(String url) {
        if (url.startsWith("file:/")) {
            return url;
        } else {
            if(url.startsWith("/")){
                return "file:" + url;
            } else {
                return "file:/" + url;
            }
        }
    }

    static String expandEnvVars(String url){
        String pluginDir = Directories.getXdgJrdBaseDir();
        String homeDir = System.getProperty("user.home");

        if(System.getProperty("os.name").toLowerCase().startsWith("win")){
            homeDir = "/" + homeDir.replaceAll("\\\\", "/");
            pluginDir = "/" + pluginDir;
        }

        url = url.replace("${XDG_CONFIG_HOME}", pluginDir);
        url = url.replace("${HOME}", homeDir);

        return url;
    }

    static String collapseEnvVars(String url){
        return null; //TODO
    }

    public URL getExpandedURL() throws MalformedURLException { // file:/C:/Users/user/path
        return new URL(expandEnvVars(prependFileProtocolIfNone(url)));
    }

    public String getRawURL(){ // file:/${HOME}/path (returns as String cause URL constructor doesn't like the ${...} part)
        return prependFileProtocolIfNone(url);
    }

    public String getRawPath(){ // ${HOME}/path
        return url;
    }

    public String getExpandedPath(){ // C:/Users/user/path (doesn't use expandEnvVars(url) cause the exception logic is needed)
        try {
            return getExpandedURL().getPath();
        } catch (MalformedURLException e) {
            throw new MalformedURLToPath(e);
        }
    }

    public File getFile(){
        return new File(getExpandedPath());
    }

    @Override
    public String toString() {
        return url;
    }
}
