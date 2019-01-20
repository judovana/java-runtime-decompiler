package org.jrd.backend.decompiling;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Directories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DecompilerWrapperInformation {

    /**
     * Class containing information about available Decompiler wrapper
     *
     * @param name           - Decompiler name
     * @param wrapperURL     - location of wrapper.java file
     * @param dependencyURLs - location of wrapper dependencies
     * @throws MalformedURLException
     */
    public DecompilerWrapperInformation(String name, String wrapperURL, List<String> dependencyURLs,
                                        String decompilerDownloadURL) {
        setName(name);
        setWrapperURL(wrapperURL);
        setFullyQualifiedClassName();
        setDependencyURLs(dependencyURLs);
        setDecompilerDownloadURL(decompilerDownloadURL);
        setFileLocation("");
    }

    // Constructor for broken wrappers, so we can track them.
    public DecompilerWrapperInformation(String url) {
        setName(url);
        invalidWrapper = true;
    }

    public DecompilerWrapperInformation() {
    }

    private String name;
    private URL decompilerDownloadURL;
    private String fileLocation;
    private String fullyQualifiedClassName;
    private URL wrapperURL;
    private List<URL> DependencyURLs;
    private Method decompileMethod;
    private Object instance;
    private boolean invalidWrapper = false;

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public boolean isInvalidWrapper() {
        return invalidWrapper;
    }

    public String getFullyQualifiedClassName() {
        if (fullyQualifiedClassName == null){
            setFullyQualifiedClassName();
        }
        return fullyQualifiedClassName;
    }

    public void setFullyQualifiedClassName() {
        try (BufferedReader br = new BufferedReader(new FileReader(wrapperURL.getPath()))) {
            String packageName = "";
            String className = "";
            String line = br.readLine();
            // Check first line for package name
            if (line.startsWith("package ")) {
                packageName = line.replace(";", ".").split(" ")[1];
            }
            // Find class name
            while ((line = br.readLine()) != null) {
                if (line.startsWith("public class ")) {
                    className = line.split(" ")[2];
                    break;
                }
            }
            fullyQualifiedClassName = packageName + className;
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
            invalidWrapper = true;
        }
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }


    public Method getDecompileMethod() {
        return decompileMethod;
    }

    public void setDecompileMethod(Method decompile) {
        this.decompileMethod = decompile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getWrapperURL() {
        return wrapperURL;
    }

    public void setWrapperURL(String wrapperURL) {
        wrapperURL = addFileProtocolIfNone(wrapperURL);
        wrapperURL = expandEnvVars(wrapperURL);
        try {
            this.wrapperURL = new URL(wrapperURL);
            File file = new File(this.wrapperURL.getFile());
            if (!(file.exists() && file.canRead())) {
                invalidWrapper = true;
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Cant read file or does not exist! " + file.getAbsolutePath()));
            }
        } catch (MalformedURLException e) {
            this.wrapperURL = null;
            this.invalidWrapper = true;
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
    }

    public List<URL> getDependencyURLs() {
        return DependencyURLs;
    }

    public void setDependencyURLs(List<String> dependencyURLs) {
        DependencyURLs = new LinkedList<>();
        for (String s : dependencyURLs) {
            s = addFileProtocolIfNone(s);
            try {
                URL dependencyURL = new URL(expandEnvVars(s));
                DependencyURLs.add(dependencyURL);
                File file = new File(dependencyURL.getFile());
                if (!(file.exists() && file.canRead())) {
                    invalidWrapper = true;
                }
            } catch (MalformedURLException e) {
                DependencyURLs.add(null);
                this.invalidWrapper = true;
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }
    }

    public URL getDecompilerDownloadURL() {
        return decompilerDownloadURL;
    }

    public void setDecompilerDownloadURL(String decompilerDownloadURL) {
        try {
            this.decompilerDownloadURL = new URL(decompilerDownloadURL);
        } catch (MalformedURLException e1) {
            this.decompilerDownloadURL = null;
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e1);
        }
    }

    public static String LOCAL_SCOPE = "local";

    public String getScope() {
        String scope = "unknown";
        if (fileLocation == null){
            return "Internal";
        }
        if (fileLocation.startsWith("/etc/")) {
            scope = "system";
        } else if (fileLocation.startsWith("/usr/share/")) {
            scope = "user shared";

        } else if (fileLocation.startsWith(Directories.getXdgJrdBaseDir())) {
            scope = LOCAL_SCOPE;
        }
        return scope;
    }

    private String addFileProtocolIfNone(String URL) {
        if (URL.contains("://")) {
            return URL;
        } else {
            return "file://" + URL;
        }
    }

    private static String expandEnvVars(String text) {
        text.replaceAll("\\$\\{XDG_CONFIG_HOME\\}", Directories.getXdgJrdBaseDir());
        Map<String, String> envMap = System.getenv();
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            text = text.replaceAll("\\$\\{" + key + "\\}", value);
        }
        return text;
    }
    @Override
    public String toString() {
        return getName() + " [" + getScope() + "]";
    }
}
