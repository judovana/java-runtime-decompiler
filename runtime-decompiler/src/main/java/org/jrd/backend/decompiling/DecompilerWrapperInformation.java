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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class DecompilerWrapperInformation {

    /**
     * Class containing information about available Decompiler wrapper
     *
     * @param name                  Decompiler name
     * @param wrapperURL            location of wrapper.java file
     * @param dependencyURLs        location of wrapper dependencies
     * @param decompilerDownloadURL decompiler download URL
     */
    public DecompilerWrapperInformation(String name, String wrapperURL, List<String> dependencyURLs,
                                        String decompilerDownloadURL) {
        setName(name);
        setWrapperURLFromURL(wrapperURL);
        setFullyQualifiedClassName();
        setDependencyURLsFromURL(dependencyURLs);
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
    private ExpandableUrl wrapperURL;
    private List<ExpandableUrl> DependencyURLs;
    private Method decompileMethodNoInners;
    private Method decompileMethodWithInners;
    private Method compileMethod;
    private Object instance;
    private boolean invalidWrapper = false;

    public static final String JAVAP_NAME = "javap";
    public static final String JAVAP_VERBOSE_NAME = "javap -v";

    public static DecompilerWrapperInformation getJavap() {
        DecompilerWrapperInformation javap = new DecompilerWrapperInformation();
        javap.setName(JAVAP_NAME);
        return javap;
    }

    public static DecompilerWrapperInformation getJavapv() {
        DecompilerWrapperInformation javapv = new DecompilerWrapperInformation();
        javapv.setName(JAVAP_VERBOSE_NAME);
        return javapv;
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
        try (BufferedReader br = new BufferedReader(new FileReader(wrapperURL.getExpandedPath()))) {
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

    public ExpandableUrl getWrapperURL() {
        return wrapperURL;
    }

    private void setWrapperURL(Runnable r) {
        try {
            r.run();
            File file = this.wrapperURL.getFile();
            if (!(file.exists() && file.canRead())) {
                invalidWrapper = true;
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Cant read file or does not exist! " + file.getAbsolutePath()));
            }
        } catch (ExpandableUrl.MalformedURLToPath e) {
            this.wrapperURL = null;
            this.invalidWrapper = true;
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
    }

    public void setWrapperURLFromPath(String wrapperURL) {
        setWrapperURL(() -> DecompilerWrapperInformation.this.wrapperURL = ExpandableUrl.createFromPath(wrapperURL));
    }

    private void setWrapperURLFromURL(String wrapperURL) {
        setWrapperURL(() -> DecompilerWrapperInformation.this.wrapperURL = ExpandableUrl.createFromStringUrl(wrapperURL));
    }

    public List<ExpandableUrl> getDependencyURLs() {
        return DependencyURLs;
    }

    private void setDependencyURLs(List<String> dependencyURLs, Switcher switcher) {
        DependencyURLs = new LinkedList<>();
        for (String s : dependencyURLs) {
            try {
                ExpandableUrl dependencyURL = switcher.getExpandableUrl(s);
                DependencyURLs.add(dependencyURL);
                File file = dependencyURL.getFile();
                if (!(file.exists() && file.canRead())) {
                    invalidWrapper = true;
                }
            } catch (ExpandableUrl.MalformedURLToPath e) {
                DependencyURLs.add(null);
                this.invalidWrapper = true;
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }
    }

    public boolean haveDecompilerMethod() {
        return getDecompileMethodNoInners() != null || getDecompileMethodWithInners() != null;
    }

    private interface Switcher {
        ExpandableUrl getExpandableUrl(String s);
    }

    public void setDependencyURLsFromPath(List<String> dependencyURLs) {
        setDependencyURLs(dependencyURLs, ExpandableUrl::createFromPath);
    }

    public void setDependencyURLsFromURL(List<String> dependencyURLs) {
        setDependencyURLs(dependencyURLs, ExpandableUrl::createFromStringUrl);
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DecompilerWrapperInformation))
            return false;

        DecompilerWrapperInformation other = (DecompilerWrapperInformation) obj;
        if (this.fileLocation == null || other.fileLocation == null) {
            return getName().equals(other.getName());
        } else {
            return new File(this.getFileLocation()).equals(new File(other.getFileLocation()));
        }
    }

}