package org.jrd.backend.decompiling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.Directories;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Executes manages external decompiler wrapper plugins.
 * Wrapper plugins are stored as .java files along with .json file containing classname, wrapper url and dependencies url.
 * When user calls decompile method with decompiler name and bytecode, wrapper plugin gets compiled and executed.
 */
public class PluginManager {

    private List<DecompilerWrapper> wrappers;

    public List<DecompilerWrapper> getWrappers() {
        return Collections.unmodifiableList(wrappers);
    }

    Gson gson;

    public static final String UNDECOMPILABLE_LAMBDA = "$$Lambda";
    public static final Pattern LAMBDA_FORM = Pattern.compile("java.lang.invoke.LambdaForm\\$.*/.*0x.*");

    public PluginManager() {
        wrappers = new LinkedList<>();

        gson = new GsonBuilder()
                .registerTypeAdapter(DecompilerWrapper.class, new DecompilerWrapperDeserializer())
                .create();

        loadConfigs();

        if (wrappers.isEmpty()) { // no wrappers found anywhere == fresh installation
            List<URL> classpathWrappers = ImportUtils.getWrappersFromClasspath();

            for (URL pluginUrl : classpathWrappers) {
                ImportUtils.importOnePlugin(
                        pluginUrl,
                        ImportUtils.filenameFromUrl(pluginUrl)
                );
            }

            loadConfigsFromLocation(Directories.getPluginDirectory());
        }

        wrappers.add(DecompilerWrapper.getJavap());
        wrappers.add(DecompilerWrapper.getJavapVerbose());

        sortWrappers();
    }

    /**
     * Searches plugin configuration locations and calls loadConfig(file) on files.
     */
    public void loadConfigs() {
        // keep all three locations - for default system scope, user shared scope and user-only scope
        String[] configLocations = new String[]{
                "/etc/java-runtime-decompiler/plugins/",
                "/usr/share/java/java-runtime-decompiler/plugins",
                Directories.getPluginDirectory()
        };

        for (String location : configLocations) {
            loadConfigsFromLocation(location);
        }
    }

    private void loadConfigsFromLocation(String location) {
        File[] files = new File(location).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            loadConfig(file);
        }
    }

    /**
     * Loads information decompiler json file into List<DecompilerWrapper>Wrapper.
     */
    private void loadConfig(File file) {
        if (file.getName().endsWith(".json")) {
            DecompilerWrapper wrapper;
            try (FileReader fileReader = new FileReader(file.getAbsolutePath(), StandardCharsets.UTF_8)) {
                wrapper = gson.fromJson(fileReader, DecompilerWrapper.class);
            } catch (IOException | NullPointerException e) {
                Logger.getLogger().log(Logger.Level.DEBUG, e);
                wrapper = null;
            }
            if (wrapper == null) {
                wrapper = new DecompilerWrapper(file.getName());
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
     * @param name      optional name for decompilers supporting inner classes
     * @param bytecode  bytecode to be decompiled
     * @param options   decompile options
     * @param vmInfo    optional vmInfo to find inner classes
     * @param vmManager optional vmManager to find inner classes
     * @return Decompiled bytecode or exception String
     * @throws Exception the exception String
     */
    public synchronized String decompile(
            DecompilerWrapper wrapper,
            String name,
            byte[] bytecode,
            String[] options,
            VmInfo vmInfo,
            VmManager vmManager
    ) throws Exception {
        if (wrapper == null) {
            return "No valid decompiler selected. Unable to decompile. \n " +
                    "If there is no decompiler selected, you need to set paths to decompiler in" +
                    "decompiler wrapper";
        }

        if (!wrapper.haveDecompilerMethod()) {
            initializeWrapper(wrapper);
        }

        if (wrapper.getDecompileMethodWithInners() != null && name != null && vmInfo != null && vmManager != null) {
            String[] allClasses = Cli.obtainClasses(vmInfo, vmManager);
            Map<String, byte[]> innerClasses = new HashMap<>();

            for (String clazz : allClasses) {
                if (isDecompilableInnerClass(name, clazz)) {
                    innerClasses.put(
                            clazz,
                            Base64.getDecoder().decode(Cli.obtainClass(vmInfo, clazz, vmManager).getLoadedClassBytes())
                    );
                }
            }

            return (String) wrapper.getDecompileMethodWithInners().invoke(
                    wrapper.getInstance(), name, bytecode, innerClasses, options
            );
        } else if (wrapper.getDecompileMethodNoInners() != null) {
            return (String) wrapper.getDecompileMethodNoInners().invoke(wrapper.getInstance(), bytecode, options);
        } else {
            throw new RuntimeException("This decompiler has no suitable decompile method for give parameters");
        }
    }

    public synchronized boolean hasBundledCompiler(DecompilerWrapper wrapper) {
        if (wrapper == null) {
            throw new RuntimeException("No valid decompiler selected. Current-Buffer may not be usable");
        }

        if (!wrapper.haveDecompilerMethod()) { //compile method may remain null
            initializeWrapper(wrapper);
        }

        if (wrapper.isJavap() || wrapper.isJavapVerbose()) {
            throw new RuntimeException("Javap can not back-compiled. Current-Buffer may not be usable");
        }

        return wrapper.getCompileMethod() != null;
    }

    /**
     * Compiles wrapper plugin, loads it into JVM and stores it for later.
     */
    private void initializeWrapper(DecompilerWrapper wrapper) {
        if (wrapper.isJavap() || wrapper.isJavapVerbose()) {
            try {
                wrapper.setInstance(new JavapDisassemblerWrapper(wrapper.isJavap() ? "" : "-v"));
                wrapper.setDecompileMethodNoInners(JavapDisassemblerWrapper.class.getMethod(
                        "decompile", byte[].class, String[].class
                ));
            } catch (NoSuchMethodException e) {
                Logger.getLogger().log("Could not find decompile method in org/jrd/backend/decompiling/JavapDisassemblerWrapper");
            }

            return;
        }

        try {
            // Compile Wrapper
            compileWrapper(wrapper, null);

            // Load wrapper
            List<URL> classPathList = new LinkedList<>();
            for (ExpandableUrl url : wrapper.getDependencyUrls()) {
                classPathList.add(url.getExpandedUrl());
            }
            classPathList.add(new URL(
                    ExpandableUrl.prependFileProtocol(System.getProperty("java.io.tmpdir")) + "/"
            )); // trailing slash just in case

            // Reflect classes & methods and store them in DecompilerWrapper for later use
            ClassLoader loader = URLClassLoader.newInstance(
                    classPathList.toArray(new URL[0]),
                    getClass().getClassLoader()
            );
            Class<?> decompilerClass = loader.loadClass(wrapper.getFullyQualifiedClassName());
            Constructor<?> constructor = decompilerClass.getConstructor();
            wrapper.setInstance(constructor.newInstance());

            try {
                wrapper.setDecompileMethodNoInners(decompilerClass.getMethod(
                        "decompile", byte[].class, String[].class)
                );
            } catch (Exception e) {
                Logger.getLogger().log(Logger.Level.DEBUG, "No custom decompile method (without inner classes): " + e.getMessage());
            }

            try {
                wrapper.setDecompileMethodWithInners(decompilerClass.getMethod(
                        "decompile", String.class, byte[].class, Map.class, String[].class
                ));
            } catch (Exception e) {
                Logger.getLogger().log(Logger.Level.DEBUG, "No custom decompile method (with inner classes): " + e.getMessage());
            }

            if (!wrapper.haveDecompilerMethod()) {
                throw new InstantiationException("Decompiler '" + wrapper.getName() + "' does not have any decompile methods!");
            }

            try {
                wrapper.setCompileMethod(decompilerClass.getMethod("compile", Map.class, String[].class, Object.class));
            } catch (Exception e) {
                Logger.getLogger().log(Logger.Level.DEBUG, "No custom compile method: " + e.getMessage());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                NoSuchMethodException | ClassNotFoundException | MalformedURLException e) {
            Logger.getLogger().log(Logger.Level.ALL, "Decompiler wrapper could not be loaded. " + e.getMessage());
            Logger.getLogger().log(e);
        } finally { // delete compiled class
            Directories.deleteWithException(
                    System.getProperty("java.io.tmpdir") + "/" + wrapper.getFullyQualifiedClassName() + ".class"
            );
        }
    }

    public void replace(DecompilerWrapper oldWrapper, DecompilerWrapper newWrapper) throws IOException {
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
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Error saving wrapper.", e));
            throw e;
        }
        boolean fileChanged = !oldWrapper.getFileLocation().equals(newWrapper.getFileLocation());
        if (fileChanged && oldWrapper.getScope().equals("local")) {
            Directories.deleteWithException(oldWrapper.getFileLocation());
        }

        wrappers.remove(oldWrapper);
        wrappers.add(newWrapper);
        sortWrappers();
    }

    public void deleteWrapper(DecompilerWrapper wrapper) {
        wrappers.remove(wrapper);

        if (wrapper.getScope().equals("local")) {
            Directories.deleteWithException(wrapper.getFileLocation());
            Directories.deleteWithException(ImportUtils.flipWrapperExtension(wrapper.getFileLocation()));
        }
    }

    public void setLocationForNewWrapper(DecompilerWrapper wrapper) {
        File file = new File(Directories.getPluginDirectory() + "/" + wrapper.getName().replaceAll(" ", "_") + ".json");
        int i = 1;
        while (file.exists()) {
            file = new File(Directories.getPluginDirectory() + "/" + wrapper.getName() + '(' + i + ')' + ".json");
            i++;
        }
        wrapper.setFileLocation(file.getAbsolutePath());
    }

    private int compileWrapper(DecompilerWrapper wrapper, ByteArrayOutputStream errStream) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        return compiler.run(null, null, errStream,
                "-d", System.getProperty("java.io.tmpdir"),
                "-cp", urlListToCsv(wrapper.getDependencyUrls(), System.getProperty("path.separator")),
                wrapper.getWrapperUrl().getExpandedPath()
        );
    }

    /**
     * Validating the @param plugin using compilation
     *
     * @param plugin - plugin to validate
     * @return error message or null
     */
    public String validatePlugin(DecompilerWrapper plugin) {
        //compiling and getting error from the compiler
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        int errLevel = compileWrapper(plugin, errStream);
        //cleaning after compilation
        String fileName = plugin.getWrapperUrl().getFile().getName();
        Directories.deleteWithException(
                System.getProperty("java.io.tmpdir") + fileName.substring(0, fileName.length() - 4) + "class"
        );

        return errLevel != 0 ? errStream.toString(StandardCharsets.UTF_8) : null;
    }

    public DecompilerWrapper createWrapper() {
        DecompilerWrapper newWrapper = new DecompilerWrapper();
        newWrapper.setName("unnamed");
        setLocationForNewWrapper(newWrapper);
        Directories.createPluginDirectory();
        File pluginJsonFile = new File(newWrapper.getFileLocation());
        try {
            pluginJsonFile.createNewFile();
        } catch (IOException e) {
            Logger.getLogger().log("Plugin wrapper json configuration file could not be loaded");
        }

        wrappers.add(newWrapper);
        sortWrappers();

        return newWrapper;
    }

    public void saveWrapper(DecompilerWrapper wrapper) throws IOException {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DecompilerWrapper.class, new DecompilerWrapperSerializer());
        gsonBuilder.setPrettyPrinting();
        final String json = gsonBuilder.create().toJson(wrapper);
        if (wrapper.getScope().equals("local")) {
            Directories.createPluginDirectory();
        }
        try (PrintWriter out = new PrintWriter(wrapper.getFileLocation(), StandardCharsets.UTF_8)) {
            out.println(json);
        }
    }

    private void sortWrappers() {
        wrappers.sort(Comparator
                .comparing(DecompilerWrapper::getScope)
                .reversed() // reversed so that javap is always the bottom
                .thenComparing(DecompilerWrapper::getName)
        );
    }

    /**
     * Converts list of URLs to CSV String<br>
     * example: (list){URL1,URL2,URL3} -> (String)URL1:URL2:URL3
     */
    private static String urlListToCsv(List<ExpandableUrl> list, String delimiter) {
        if (list == null) {
            return "";
        }

        String out = "";
        for (ExpandableUrl url : list) {
            out += url.getExpandedPath() + delimiter;
        }

        if (out.length() == 0) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

    public void addWrapper(DecompilerWrapper wrapper) {
        wrappers.add(wrapper);
    }
}
