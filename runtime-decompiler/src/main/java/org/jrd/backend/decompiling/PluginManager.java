package org.jrd.backend.decompiling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.Directories;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jrd.backend.decompiling.ExpandableUrl.prependFileProtocol;

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

    public static final String UNDECOMPILABLE_LAMBDA = "$$Lambda";

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
    private void loadConfig(File file) {
        if (file.getName().endsWith(".json")) {
            DecompilerWrapperInformation wrapper;
            try (FileReader fileReader = new FileReader(file.getAbsolutePath())){
                wrapper = gson.fromJson(fileReader, DecompilerWrapperInformation.class);
            } catch (IOException | NullPointerException e) {
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

    private boolean isDecompilableInnerClass(String baseClass, String currentClass) {
        return !currentClass.contains(UNDECOMPILABLE_LAMBDA) && (currentClass.startsWith(baseClass + "$") ||
                        currentClass.startsWith(baseClass.replaceAll("__init$", "") + "$"));
    }

    /**
     * @param wrapper   decompiler used for decompiling
     * @param name      optional name for decompielrs supporting inner classes
     * @param bytecode  bytecode to be decompiled
     * @param options   decompile options
     * @param vmInfo    otional vminfo to find inner classes
     * @param vmManager otional vmmanager to find inner classes
     * @return Decompiled bytecode or exception String
     * @throws Exception exception String
     */
    public synchronized String decompile(DecompilerWrapperInformation wrapper, String name, byte[] bytecode, String[] options, VmInfo vmInfo, VmManager vmManager) throws Exception {
        if (wrapper == null) {
            return "No valid decompiler selected. Unable to decompile. \n " +
                    "If there is no decompiler selected, you need to set paths to decompiler in" +
                    "decompiler wrapper";
        }
        if (!wrapper.haveDecompilerMethod()) {
            InitializeWrapper(wrapper);
        }
        if (wrapper.getDecompileMethodWithInners() != null && name != null && vmInfo != null && vmManager != null) {
            String[] allClasses = Cli.obtainClasses(vmInfo, vmManager);
            Map<String, byte[]> innerClasses = new HashMap<>();
            for (String clazz : allClasses) {
                if (isDecompilableInnerClass(name, clazz)) {
                    innerClasses.put(clazz, Base64.getDecoder().decode(Cli.obtainClass(vmInfo, clazz, vmManager).getLoadedClassBytes()));
                }
            }
            return (String) wrapper.getDecompileMethodWithInners().invoke(wrapper.getInstance(), name, bytecode, innerClasses, options);
        } else if (wrapper.getDecompileMethodNoInners() != null) {
            return (String) wrapper.getDecompileMethodNoInners().invoke(wrapper.getInstance(), bytecode, options);
        } else {
            throw new RuntimeException("This decompiler have no suitable decompile method for give parameters");
        }
    }

    public synchronized boolean haveCompiler(DecompilerWrapperInformation wrapper) throws Exception {
        if (wrapper == null) {
            throw new RuntimeException("No valid decompiler selected. Current-Buffer may not be usable");
        }
        if (!wrapper.haveDecompilerMethod()) { //compile method may remian null
            InitializeWrapper(wrapper);
        }
        if (wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_NAME) || wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_VERBOSE_NAME)) {
            throw new RuntimeException("Javap can not back-compiled. Current-Buffer may not be usable");
        }
        return wrapper.getCompileMethod() != null;
    }

    /**
     * Compiles wrapper plugin, loads it into JVM and stores it for later.
     */
    private void InitializeWrapper(DecompilerWrapperInformation wrapper) {
        if (wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_NAME)) {
            try {
                wrapper.setInstance(new JavapDisassemblerWrapper(""));
                wrapper.setDecompileMethodNoInners(JavapDisassemblerWrapper.class.getMethod("decompile", byte[].class, String[].class));
            } catch (NoSuchMethodException e) {
                OutputController.getLogger().log("Could not find decompile method in org/jrd/backend/decompiling/JavapDisassemblerWrapper");
            }
        } else if (wrapper.getName().equals(DecompilerWrapperInformation.JAVAP_VERBOSE_NAME)) {
            try {
                wrapper.setInstance(new JavapDisassemblerWrapper("-v"));
                wrapper.setDecompileMethodNoInners(JavapDisassemblerWrapper.class.getMethod("decompile", byte[].class, String[].class));
            } catch (NoSuchMethodException e) {
                OutputController.getLogger().log("Could not find decompile method in org/jrd/backend/decompiling/JavapDisassemblerWrapper");
            }
        } else {
            try {
                // Compile Wrapper
                compileWrapper(wrapper, null);
                // Load wrapper
                URL tempDirURL = new URL(prependFileProtocol(System.getProperty("java.io.tmpdir")) + "/"); // trailing slash just in case
                List<ExpandableUrl> sTemp = new LinkedList(wrapper.getDependencyURLs());
                List<URL> s = new LinkedList<>();
                for (ExpandableUrl u : sTemp) {
                    s.add(u.getExpandedURL());
                }
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
                try {
                    wrapper.setDecompileMethodNoInners(DecompilerClass.getMethod("decompile", byte[].class, String[].class));
                } catch (Exception e) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "No custom deompile method (without inner classes): " + e.getMessage());
                }
                try {
                    wrapper.setDecompileMethodWithInners(DecompilerClass.getMethod("decompile", String.class, byte[].class, Map.class, String[].class));
                } catch (Exception e) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "No custom compile method (with inner classes): " + e.getMessage());
                }
                if (!wrapper.haveDecompilerMethod()) {
                    throw new InstantiationException("Decompiler " + wrapper.getName() + " do not have decompile method(s)");
                }
                try {
                    wrapper.setCompileMethod(DecompilerClass.getMethod("compile", Map.class, String[].class, Object.class));
                } catch (Exception e) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "No custom compile method: " + e.getMessage());
                }
            } catch (Exception e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "Decompiler wrapper could not be loaded. " + e.getMessage());
                OutputController.getLogger().log(e);
            } finally {
                // Delete compiled class
                new File(System.getProperty("java.io.tmpdir") + "/" + wrapper.getFullyQualifiedClassName() + ".class").delete();
            }
        }
    }

    public void replace(DecompilerWrapperInformation oldWrapper, DecompilerWrapperInformation newWrapper) throws IOException {
        if (oldWrapper == null || newWrapper == null) {
            return;
        }
        boolean nameChanged = !(oldWrapper.getName().equals(newWrapper.getName()));
        if (nameChanged && oldWrapper.getScope().equals("local")) {
            setLocationForNewWrapper(newWrapper);
        }
        try {
            saveWrapper(newWrapper);
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Error saving wrapper.", e));
            throw e;
        }
        boolean fileChanged = !oldWrapper.getFileLocation().equals(newWrapper.getFileLocation());
        if (fileChanged && oldWrapper.getScope().equals("local")) {
            File oldWrapperFile = new File(oldWrapper.getFileLocation());
            oldWrapperFile.delete();
        }
        wrappers.add(newWrapper);
        wrappers.remove(oldWrapper);
    }

    public void deleteWrapper(DecompilerWrapperInformation wrapperInformation) {
        wrappers.remove(wrapperInformation);

        if (wrapperInformation.getScope().equals("local")) {
            try {
                Files.delete(Path.of(wrapperInformation.getFileLocation()));
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }

            try {
                Files.delete(Path.of(flipWrapperExtension(wrapperInformation.getFileLocation())));
            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
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

    private int compileWrapper(DecompilerWrapperInformation wrapper, ByteArrayOutputStream errStream) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        return compiler.run(null, null, errStream,
                "-d", System.getProperty("java.io.tmpdir"),
                "-cp", URLListToCSV(wrapper.getDependencyURLs(), System.getProperty("path.separator")),
                wrapper.getWrapperURL().getExpandedPath()
        );
    }

    /**
     * Validating the @param plugin using compilation
     *
     * @param plugin - plugin to validate
     * @return error message or null
     */
    public String validatePlugin(DecompilerWrapperInformation plugin) {
        //compiling and getting error from the compiler
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        int errLevel = compileWrapper(plugin, errStream);
        //cleaning after compilation
        String fileName = plugin.getWrapperURL().getFile().getName();

        try {
            Files.delete(Path.of(System.getProperty("java.io.tmpdir"), fileName.substring(0, fileName.length() - 4) + "class"));
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
        }

        return errLevel != 0 ? errStream.toString() : null;
    }

    public DecompilerWrapperInformation createWrapper() {
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
        if (wrapper.getScope().equals("local")) {
            createUserPluginDir();
        }
        try (PrintWriter out = new PrintWriter(wrapper.getFileLocation())) {
            out.println(json);
        }
    }

    public static void createUserPluginDir() {
        File pluginDir = new File(Directories.getPluginDirectory());
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
    }

    /**
     * Converts list of URLs to CSV String<br>
     * example: (list){URL1,URL2,URL3} -> (String)URL1:URL2:URL3
     */
    private String URLListToCSV(List<ExpandableUrl> list, String delimeter) {
        if (list == null) {
            return "";
        }

        String out = "";
        for (ExpandableUrl url : list) {
            out += url.getExpandedPath() + delimeter;
        }

        if (out.length() == 0) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

    public static String flipWrapperExtension(String filePath) {
        if (filePath.endsWith(".json")) {
            return filePath.replace(".json", ".java");
        } else if (filePath.endsWith(".java")) {
            return filePath.replace(".java", ".json");
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Incorrect plugin wrapper path: " + filePath));
            return filePath;
        }
    }
}
