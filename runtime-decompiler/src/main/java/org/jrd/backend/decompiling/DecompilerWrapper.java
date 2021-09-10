package org.jrd.backend.decompiling;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Directories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for holding information about a decompiler wrapper (also called a plugin).
 */
public class DecompilerWrapper {

    /**
     * Constructs a valid wrapper.
     *
     * @param name                  Decompiler name
     * @param wrapperUrl            location of wrapper.java file
     * @param dependencyUrls        location of wrapper dependencies
     * @param decompilerDownloadUrl decompiler download URL
     */
    public DecompilerWrapper(
            String name, String wrapperUrl, List<String> dependencyUrls, String decompilerDownloadUrl
    ) {
        setName(name);
        setWrapperUrlFromUrl(wrapperUrl);
        setFullyQualifiedClassName();
        setDependencyUrlsFromUrl(dependencyUrls);
        setDecompilerDownloadUrl(decompilerDownloadUrl);
        setFileLocation("");
    }

    /**
     * Constructs an invalid wrapper to differentiate them from valid ones.
     * @param url broken wrapper identifier, used as a name
     */
    public DecompilerWrapper(String url) {
        setName(url);
        invalidWrapper = true;
    }

    /**
     * Constructs an empty, but temporarily valid wrapper.
     */
    public DecompilerWrapper() {
    }

    private String name;
    private URL decompilerDownloadUrl;
    private String fileLocation;
    private String fullyQualifiedClassName;
    private ExpandableUrl wrapperUrl;
    private List<ExpandableUrl> dependencyUrls;
    private Method decompileMethodNoInners;
    private Method decompileMethodWithInners;
    private Method compileMethod;
    private Object instance;
    private boolean invalidWrapper = false;

    public static final String JAVAP_NAME = "javap";
    public static final String JAVAP_VERBOSE_NAME = "javap -v";

    public static DecompilerWrapper getJavap() {
        DecompilerWrapper javap = new DecompilerWrapper();
        javap.setName(JAVAP_NAME);
        return javap;
    }

    public static DecompilerWrapper getJavapVerbose() {
        DecompilerWrapper javapVerbose = new DecompilerWrapper();
        javapVerbose.setName(JAVAP_VERBOSE_NAME);
        return javapVerbose;
    }

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
        if (fullyQualifiedClassName == null) {
            setFullyQualifiedClassName();
        }
        return fullyQualifiedClassName;
    }

    public void setFullyQualifiedClassName() {
        String wrapperPath = wrapperUrl.getExpandedPath();

        try (BufferedReader br = new BufferedReader(new FileReader(wrapperPath, StandardCharsets.UTF_8))) {
            String packageName = "";
            String className = "";
            String line = br.readLine();

            if (line == null) {
                Logger.getLogger().log(Logger.Level.ALL, "Wrapper '" + wrapperPath + "' is empty!");
                invalidWrapper = true;
                return;
            }

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
            Logger.getLogger().log(Logger.Level.DEBUG, e);
            invalidWrapper = true;
        }
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Method getDecompileMethodNoInners() {
        return decompileMethodNoInners;
    }

    public void setDecompileMethodNoInners(Method decompileMethodNoInners) {
        this.decompileMethodNoInners = decompileMethodNoInners;
    }

    public Method getDecompileMethodWithInners() {
        return decompileMethodWithInners;
    }

    public void setDecompileMethodWithInners(Method decompileMethodWithInners) {
        this.decompileMethodWithInners = decompileMethodWithInners;
    }

    public Method getCompileMethod() {
        return compileMethod;
    }

    public void setCompileMethod(Method compileMethod) {
        this.compileMethod = compileMethod;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExpandableUrl getWrapperUrl() {
        return wrapperUrl;
    }

    private void setWrapperUrl(Runnable r) {
        try {
            r.run();
            File file = this.wrapperUrl.getFile();

            if (!file.exists()) {
                invalidWrapper = true;
                Logger.getLogger().log(Logger.Level.ALL, new FileNotFoundException(file.getAbsolutePath()));
            }
            if (!file.canRead()) {
                invalidWrapper = true;
                Logger.getLogger().log(Logger.Level.ALL, new IOException("Unable to read file '" + file.getAbsolutePath() + "'."));
            }
        } catch (ExpandableUrl.MalformedUrlToPath e) {
            this.wrapperUrl = null;
            this.invalidWrapper = true;
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
    }

    public void setWrapperUrlFromPath(String path) {
        setWrapperUrl(() -> DecompilerWrapper.this.wrapperUrl = ExpandableUrl.createFromPath(path));
    }

    private void setWrapperUrlFromUrl(String url) {
        setWrapperUrl(() -> DecompilerWrapper.this.wrapperUrl = ExpandableUrl.createFromStringUrl(url));
    }

    public List<ExpandableUrl> getDependencyUrls() {
        return Collections.unmodifiableList(dependencyUrls);
    }

    public void setDependencyUrlsFromPath(List<String> dependencyUrls) {
        setDependencyUrls(dependencyUrls, ExpandableUrl::createFromPath);
    }

    public void setDependencyUrlsFromUrl(List<String> dependencyUrls) {
        setDependencyUrls(dependencyUrls, ExpandableUrl::createFromStringUrl);
    }

    private void setDependencyUrls(List<String> dependencyUrls, Switcher switcher) {
        this.dependencyUrls = new LinkedList<>();
        for (String s : dependencyUrls) {
            try {
                ExpandableUrl dependencyUrl = switcher.getExpandableUrl(s);
                this.dependencyUrls.add(dependencyUrl);
                File file = dependencyUrl.getFile();
                if (!(file.exists() && file.canRead())) {
                    invalidWrapper = true;
                }
            } catch (ExpandableUrl.MalformedUrlToPath e) {
                this.dependencyUrls.add(null);
                this.invalidWrapper = true;
                Logger.getLogger().log(Logger.Level.ALL, e);
            }
        }
    }

    public boolean haveDecompilerMethod() {
        return getDecompileMethodNoInners() != null || getDecompileMethodWithInners() != null;
    }

    private interface Switcher {
        ExpandableUrl getExpandableUrl(String s);
    }

    public URL getDecompilerDownloadUrl() {
        return decompilerDownloadUrl;
    }

    public void setDecompilerDownloadUrl(String decompilerDownloadUrl) {
        try {
            this.decompilerDownloadUrl = new URL(decompilerDownloadUrl);
        } catch (MalformedURLException e1) {
            this.decompilerDownloadUrl = null;
            Logger.getLogger().log(Logger.Level.DEBUG, e1);
        }
    }

    public static final String LOCAL_SCOPE = "local";

    public String getScope() {
        String scope = "unknown";
        if (fileLocation == null) {
            return "Internal";
        }
        if (fileLocation.startsWith("/etc/")) {
            scope = "system";
        } else if (fileLocation.startsWith("/usr/share/")) {
            scope = "user shared";

        } else {
            Path wrapperFile = Paths.get(fileLocation);
            Path baseDir = Paths.get(Directories.getXdgJrdBaseDir());

            if (wrapperFile.startsWith(baseDir)) {
                scope = LOCAL_SCOPE;
            }
        }
        return scope;
    }

    public boolean isLocal() {
        return getScope().equals(LOCAL_SCOPE);
    }

    public boolean isJavap() {
        return name.equals(DecompilerWrapper.JAVAP_NAME);
    }

    public boolean isJavapVerbose() {
        return name.equals(DecompilerWrapper.JAVAP_VERBOSE_NAME);
    }

    @Override
    public String toString() {
        return getName() + " [" + getScope() + "]";
    }

    @Override
    public int hashCode() {
        return new File(this.fileLocation).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DecompilerWrapper)) {
            return false;
        }

        DecompilerWrapper other = (DecompilerWrapper) obj;
        if (this.fileLocation == null || other.fileLocation == null) {
            return getName().equals(other.getName());
        } else {
            return new File(this.getFileLocation()).equals(new File(other.getFileLocation()));
        }
    }

}
