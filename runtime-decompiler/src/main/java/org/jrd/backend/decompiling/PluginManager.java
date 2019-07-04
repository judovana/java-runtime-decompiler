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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    public void loadConfigs() {
        wrappers = new LinkedList<>();
        // keep all three locations - for default system scope, user shared scope and user-only scope
        String[] configLocations = new String[]{"/etc/java-runtime-decompiler/plugins/"
                , "/usr/share/java/java-runtime-decompiler/plugins"
                , Directories.getPluginDirectory()};
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

        wrappers.add(DecompilerWrapperInformation.getJavap());
        wrappers.add(DecompilerWrapperInformation.getJavapv());
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
     * Decompile with default options
     */
    public String decompile(DecompilerWrapperInformation wrapper, byte[] bytecode) throws Exception {
        return decompile(wrapper, bytecode, null);
    }

    /**
     * @param wrapper  decompiler used for decompiling
     * @param bytecode bytecode to be decompiled
     * @return Decompiled bytecode or exception String.
     */
    public synchronized String decompile(DecompilerWrapperInformation wrapper, byte[] bytecode, String[] options) throws Exception{
        if (wrapper == null) {
            return "No valid decompiler selected. Unable to decompile. \n " +
                    "If there is no decompiler selected, you need to set paths to decompiler in" +
                    "decompiler wrapper";
        }
        if (wrapper.getDecompileMethod() == null) {
            InitializeWrapper(wrapper);
        }
        return (String) wrapper.getDecompileMethod().invoke(wrapper.getInstance(), bytecode, options);
    }

    /**
     * Compiles wrapper plugin, loads it into JVM and stores it for later.
     *
     * @param wrapper
     * @throws RuntimeException
     */
    private void InitializeWrapper(DecompilerWrapperInformation wrapper){
        if (wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_NAME)){
            try {
                wrapper.setInstance(new JavapDisassemblerWrapper(""));
                wrapper.setDecompileMethod(JavapDisassemblerWrapper.class.getMethod("decompile", byte[].class, String[].class));
            } catch (NoSuchMethodException e) {
                OutputController.getLogger().log("Could not find decompile method in org/jrd/backend/decompiling/JavapDisassemblerWrapper");
            }
        } else if (wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_VERBOSE_NAME)){
            try {
                wrapper.setInstance(new JavapDisassemblerWrapper("-v"));
                wrapper.setDecompileMethod(JavapDisassemblerWrapper.class.getMethod("decompile", byte[].class, String[].class));
            } catch (NoSuchMethodException e) {
                OutputController.getLogger().log("Could not find decompile method in org/jrd/backend/decompiling/JavapDisassemblerWrapper");
            }
        } else {
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
                wrapper.setDecompileMethod(DecompilerClass.getMethod("decompile", byte[].class, String[].class));
            } catch (Exception e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "Decompiler wrapper could not be loaded. " + e.getMessage());
                OutputController.getLogger().log(e);
            } finally {
                // Delete compiled class
                new File(System.getProperty("java.io.tmpdir") + "/" + wrapper.getFullyQualifiedClassName() + ".class").delete();
            }
        }
    }

    public void replace(DecompilerWrapperInformation oldWrapper, DecompilerWrapperInformation newWrapper) throws IOException{
        if (oldWrapper == null || newWrapper == null){
            return;
        }
        boolean nameChanged = !(oldWrapper.getName().equals(newWrapper.getName()));
        if (nameChanged && oldWrapper.getScope().equals("local")){
            setLocationForNewWrapper(newWrapper);
        }
        try {
            saveWrapper(newWrapper);
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Error saving wrapper.", e));
            throw e;
        }
        boolean fileChanged = !oldWrapper.getFileLocation().equals(newWrapper.getFileLocation());
        if (fileChanged && oldWrapper.getScope().equals("local")){
            File oldWrapperFile = new File(oldWrapper.getFileLocation());
            oldWrapperFile.delete();
        }
        wrappers.add(newWrapper);
        wrappers.remove(oldWrapper);
    }

    public void deleteWrapper(DecompilerWrapperInformation wrapperInformation){
        wrappers.remove(wrapperInformation);

        if (wrapperInformation.getScope().equals("local")){
            new File(wrapperInformation.getFileLocation()).delete();
            new File(flipWrapperExtension(wrapperInformation.getFileLocation())).delete();
        }
    }

    public void setLocationForNewWrapper(DecompilerWrapperInformation wrapperInformation) {
        File file = new File(Directories.getPluginDirectory() + "/" + wrapperInformation.getName().replaceAll(" ", "_") + ".json");
        int i = 1;
        while (file.exists()) {
            file = new File(Directories.getPluginDirectory() + "/" + wrapperInformation.getName() + '(' + i + ')' + ".json");
            i++;
        }
        wrapperInformation.setFileLocation(file.getAbsolutePath());
    }

    /**
     * Validating the @param plugin using compilation
     * @param plugin - plugin to validate
     * @return error message or null
     */
    public String validatePlugin(DecompilerWrapperInformation plugin) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        //preparing data for the compiler
        List<URLExpandable> dependencyURLs = plugin.getDependencyURLs();
        ArrayList<String> compileStringA = new ArrayList<>();
        compileStringA.add("-d");
        compileStringA.add(System.getProperty("java.io.tmpdir"));

        if (dependencyURLs != null) {
            compileStringA.add("-cp");

            StringBuilder dependencyS = new StringBuilder();
            for (URLExpandable dependency : dependencyURLs) {
                dependencyS.append(":").append(dependency.getPath());
            }
            compileStringA.add(dependencyS.toString());
        }

        compileStringA.add(plugin.getWrapperURL().getPath());
        String[] compileString = compileStringA.toArray(new String[0]);
        //compiling and getting error from the compiler
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        int errLevel = compiler.run(null, null, errStream, compileString);
        //cleaning after compilation
        String fileName = plugin.getWrapperURL().getFile().getName();
        File fileToRemove = new File(System.getProperty("java.io.tmpdir") +"/"+ fileName.substring(0,fileName.length()-4) + "class");

        if (fileToRemove.exists())
            fileToRemove.delete();
        return errLevel != 0 ? new String(errStream.toByteArray()): null;
    }

    public DecompilerWrapperInformation createWrapper(){
        DecompilerWrapperInformation newWrapper = new DecompilerWrapperInformation();
        newWrapper.setName("unnamed");
        setLocationForNewWrapper(newWrapper);
        createUserPluginDir();
        File plugin_json_file = new File(newWrapper.getFileLocation());
        try {
            plugin_json_file.createNewFile();
        } catch (IOException e) {
            OutputController.getLogger().log("Plugin wrapper json configuration file could not be loaded");
        }
        wrappers.add(newWrapper);
        return newWrapper;
    }

    public void saveWrapper(DecompilerWrapperInformation wrapper) throws IOException {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DecompilerWrapperInformation.class, new DecompilerWrapperInformationSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        final String json = gson.toJson(wrapper);
        if (wrapper.getScope().equals("local")){
            createUserPluginDir();
        }
        try (PrintWriter out = new PrintWriter(wrapper.getFileLocation())) {
            out.println(json);
        }
    }

    public static void createUserPluginDir() {
        File pluginDir = new File(Directories.getPluginDirectory());
        if (!pluginDir.exists()) {
            pluginDir.mkdir();
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
    private String URLListToCSV(List<URLExpandable> list, char delimeter) {
        String out = "";
        for (URLExpandable url : list) {
            out += url.getPath() + delimeter;
        }
        if (out.length() == 0) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

    public static String flipWrapperExtension(String filePath){
        if(filePath.endsWith(".json")){
            return filePath.replace(".json", ".java");
        } else if(filePath.endsWith(".java")){
            return filePath.replace(".java", ".json");
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Incorrect plugin wrapper path: " + filePath));
            return filePath;
        }
    }
}
