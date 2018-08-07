package com.redhat.thermostat.vm.decompiler.decompiling;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class DecompilerWrapperInformation {

    /**
     * Class containing information about available Decompiler wrapper
     * @param name - Decompiler name
     * @param wrapperURL - location of wrapper.java file
     * @param DecompilerURL location of decompiler
     * @param source folder containing this .info file.
     * @throws MalformedURLException
     */
    DecompilerWrapperInformation(String name, String fullyQualifiedClassName, String wrapperURL, String DecompilerURL, String source) throws MalformedURLException{
        setName(name);
        setFullyQualifiedClassName(fullyQualifiedClassName);
        setWrapperURL(wrapperURL);
        setDecompilerURL(DecompilerURL);
        setSource(source);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    private String name;

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public void setFullyQualifiedClassName(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    private String fullyQualifiedClassName;
    private URL wrapperURL;
    private URL decompilerURL;
    private String source;
    private Method decompileMethod;
    private Object instance;


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

    private void setWrapperURL(String wrapperURL) throws MalformedURLException {
        wrapperURL = addFileProtocolIfNone(wrapperURL);
        wrapperURL = expandEnvVars(wrapperURL);
        this.wrapperURL = new URL(wrapperURL);
    }

    public URL getDecompilerURL() {
        return decompilerURL;
    }

    private void setDecompilerURL(String decompilerURL) throws MalformedURLException{
        decompilerURL = addFileProtocolIfNone(decompilerURL);
        decompilerURL = expandEnvVars(decompilerURL);
        this.decompilerURL = new URL(decompilerURL);
    }

    private String addFileProtocolIfNone(String URL){
        if (URL.contains("://")){
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

}
