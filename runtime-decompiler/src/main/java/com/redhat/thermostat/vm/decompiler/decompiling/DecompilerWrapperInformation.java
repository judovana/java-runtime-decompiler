package com.redhat.thermostat.vm.decompiler.decompiling;

import java.io.File;
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
    DecompilerWrapperInformation(String name, String fullyQualifiedClassName, String wrapperURL, List<String> dependencyURLs) {
        setName(name);
        setFullyQualifiedClassName(fullyQualifiedClassName);
        setWrapperURL(wrapperURL);
        setDependencyURLs(dependencyURLs);
    }
    // Constructor for broken wrappers, so we can track them.
    DecompilerWrapperInformation(String url) {
        setName(url);
        invalidWrapper = true;
    }

    private String name;

    private String fullyQualifiedClassName;
    private URL wrapperURL;
    private List<URL> DependencyURLs;
    private Method decompileMethod;
    private Object instance;
    private boolean invalidWrapper = false;

    public boolean isInvalidWrapper() {
        return invalidWrapper;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public void setFullyQualifiedClassName(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
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

    private void setWrapperURL(String wrapperURL){
        wrapperURL = addFileProtocolIfNone(wrapperURL);
        wrapperURL = expandEnvVars(wrapperURL);
        try{
            this.wrapperURL = new URL(wrapperURL);
            File file = new File(this.wrapperURL.getFile());
            if (!(file.exists() && file.canRead())){
                invalidWrapper = true;
            }
        } catch (MalformedURLException e){
            this.wrapperURL = null;
            this.invalidWrapper = true;
        }
    }

    public List<URL> getDependencyURLs() {
        return DependencyURLs;
    }

    private void setDependencyURLs(List<String> dependencyURLs){
        DependencyURLs = new LinkedList<>();
        for (String s : dependencyURLs) {
            s = addFileProtocolIfNone(s);
            try{
                URL dependencyURL = new URL(expandEnvVars(s));
                DependencyURLs.add(dependencyURL);
                File file = new File(dependencyURL.getFile());
                if (!(file.exists() && file.canRead())){
                    invalidWrapper = true;
                }
            } catch (MalformedURLException e){
                DependencyURLs.add(null);
                this.invalidWrapper = true;
            }
        }
    }

    private String addFileProtocolIfNone(String URL) {
        if (URL.contains("://")) {
            return URL;
        } else {
            return "file://" + URL;
        }
    }

    private static String expandEnvVars(String text) {
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
        return getName();
    }
}
