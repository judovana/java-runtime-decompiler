package org.jrd.backend.decompiling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Directories;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Executes manages external decompiler wrapper plugins.
 * Wrapper plugins are stored as .java files along with .json file containing classname, wrapper url and dependencies url.
 * When user calls decompile method with decompiler name and bytecode, wrapper plugin gets compiled and executed.
 */
public class PluginManager {

    private List<DecompilerWrapperInformation> wrappers;

    public List<DecompilerWrapperInformation> getWrappers() {
        return wrappers;
    }

    Gson gson;

    public PluginManager() {
        loadConfigs();
    }

    /**
     * Searches plugin configuration locations and calls loadConfig(file) on files.
     */
    private void loadConfigs() {
        wrappers = new LinkedList<>();
        // keep all three locations - for default system scope, user shared scope and user-only scope
        String[] configLocations = new String[]{"/etc/java-runtime-decompiler/plugins/"
                , "/usr/share/java/java-runtime-decompiler/plugins"
                , new Directories().getXdgJrdBaseDir() + "/plugins/"};
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DecompilerWrapperInformation.class, new DecompilerWrapperInformationDeserializer());
        gson = gsonBuilder.create();
        for (String location : configLocations) {
            File[] files = new File(location).listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                loadConfig(file);
            }
        }
    }

    /**
     * Loads information decompiler json file into List<DecompilerWrapperInformation>Wrapper.
     */
    private void loadConfig(File file){
        if (file.getName().endsWith(".json")) {
            DecompilerWrapperInformation wrapper;
            try {
                wrapper = gson.fromJson(new FileReader(file.getAbsolutePath()), DecompilerWrapperInformation.class);
            } catch (FileNotFoundException | NullPointerException e ) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
                wrapper = null;
            }
            if (wrapper == null) {
                wrapper = new DecompilerWrapperInformation(file.getName());
            }
            wrapper.setFileLocation(file.getAbsolutePath());
            wrappers.add(wrapper);
        }
    }

    /**
     * @param wrapper  decompiler used for decompiling
     * @param bytecode bytecode to be decompiled
     * @return Decompiled bytecode or exception String.
     */
    public synchronized String decompile(DecompilerWrapperInformation wrapper, byte[] bytecode) throws Exception {
        if (wrapper == null) {
            return "No valid decompiler selected. Unable to decompile. \n " +
                    "If there is no decompiler selected, you need to set paths to decompiler in" +
                    "decompiler wrapper";
        }
        if (wrapper.getDecompileMethod() == null) {
            InitializeWrapper(wrapper);
        }
        return (String) wrapper.getDecompileMethod().invoke(wrapper.getInstance(), bytecode);
    }

    /**
     * Compiles wrapper plugin, loads it into JVM and stores it for later.
     *
     * @param wrapper
     * @throws Exception
     */
    private void InitializeWrapper(DecompilerWrapperInformation wrapper) throws Exception {
        try {
            // Compile Wrapper
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, wrapper.getWrapperURL().getPath(),
                    "-cp", URLListToCSV(wrapper.getDependencyURLs(), ':'), "-d", System.getProperty("java.io.tmpdir"));
            // Load wrapper
            URL tempDirURL = new URL("file://" + System.getProperty("java.io.tmpdir") + "/");
            List<URL> s = new LinkedList(wrapper.getDependencyURLs());
            s.add(tempDirURL);
            URL[] classpath = new URL[s.size()];
            s.toArray(classpath);
            ClassLoader loader = URLClassLoader.newInstance(
                    classpath,
                    getClass().getClassLoader()
            );
            // Reflect classes and store them in DecompilerWrapperInformation for later use
            Class DecompilerClass = loader.loadClass(wrapper.getFullyQualifiedClassName());
            Constructor constructor = DecompilerClass.getConstructor();
            wrapper.setInstance(constructor.newInstance());
            wrapper.setDecompileMethod(DecompilerClass.getMethod("decompile", byte[].class));
        } catch (Exception e) {
            throw e;
        } finally {
            // Delete compiled class
            new File(System.getProperty("java.io.tmpdir") + "/" + wrapper.getFullyQualifiedClassName() + ".class").delete();
        }
    }

    public void replace(DecompilerWrapperInformation oldWrapper, DecompilerWrapperInformation newWrapper) {
        if (oldWrapper.getName().equals("") || oldWrapper.equals(null)){
            setLocationForNewWrapper(newWrapper);
        }
        wrappers.remove(oldWrapper);
        wrappers.add(newWrapper);

            try {
                saveWrapper(newWrapper);
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Error saving wrapper.", e));
            }
    }

    public void deleteWrapper(DecompilerWrapperInformation wrapperInformation){
        wrappers.remove(wrapperInformation);

        if (wrapperInformation.getScope().equals("local")){
            new File(wrapperInformation.getFileLocation()).delete();
        }
    }

    private void setLocationForNewWrapper(DecompilerWrapperInformation wrapperInformation) {
        File file = new File(new Directories().getXdgJrdBaseDir() + "/plugins/" + wrapperInformation.getName().replaceAll(" ", "_") + ".json");
        int i = 1;
        while (file.exists()) {
            file = new File(new Directories().getXdgJrdBaseDir() + "/plugins/" + wrapperInformation.getName() + '(' + i + ')' + ".json");
            i++;
        }
        wrapperInformation.setFileLocation(file.getAbsolutePath());
    }

    public DecompilerWrapperInformation createWrapper(){
        DecompilerWrapperInformation newWrapper = new DecompilerWrapperInformation("");
        setLocationForNewWrapper(newWrapper);
        wrappers.add(newWrapper);
        return newWrapper;
    }

    private void saveWrapper(DecompilerWrapperInformation wrapper) throws IOException {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DecompilerWrapperInformation.class, new DecompilerWrapperInformationSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        final String json = gson.toJson(wrapper);
        File pluginDir = new File(new Directories().getXdgJrdBaseDir() + "/plugins");
        if (!pluginDir.exists()){
            pluginDir.mkdir();
        }
        try (PrintWriter out = new PrintWriter(wrapper.getFileLocation())) {
            out.println(json);
        }
    }

    /**
     * Converts list of URLs to CSV String<br>
     * example: (list){URL1,URL2,URL3} -> (String)URL1:URL2:URL3
     *
     * @param list
     * @param delimeter
     * @return
     */
    private String URLListToCSV(List<URL> list, char delimeter) {
        String out = "";
        for (URL url : list) {
            out += url.getPath() + delimeter;
        }
        if (out.length() == 0) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

}
