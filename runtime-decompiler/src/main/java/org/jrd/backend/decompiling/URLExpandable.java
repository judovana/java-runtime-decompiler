package org.jrd.backend.decompiling;

import org.jrd.backend.data.Directories;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class URLExpandable {
    public static class MalformedURLToPath extends RuntimeException{
        public MalformedURLToPath(Throwable cause) {
            super(cause);
        }
    }

    private String url;

    public URLExpandable(String urlString) {
        urlString = prependFileProtocolIfNone(urlString);
        urlString = expandEnvVars(urlString);
        this.url = urlString;
    }
    public URLExpandable(URL url) {
        this.url = url.toString();
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

    public URL getURL() throws MalformedURLException {
        return new URL(url);
    }

    public String getURLString(){
        return url;
    }

    public String getPath() {
        try {
            return getURL().getPath();
        } catch (MalformedURLException e) {
            throw new MalformedURLToPath(e);
        }
    }

    public File getFile(){
        return new File(getPath());
    }

    @Override
    public String toString() {
        return url;
    }
}
