package com.redhat.thermostat.vm.decompiler.decompiling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.soc.directories.BaseDirectories;

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

    public PluginManager() {
        loadConfigs();
    }

    /**
     * Loads information about available decompilers into List <DecompilerWrapperInformation> Wrapper.
     */
    private void loadConfigs() {
        wrappers = new LinkedList<>();
        String[] configLocations = new String[]{"/etc/java-runtime-decompiler/plugins/"
                , "/usr/share/java/java-runtime-decompiler/plugins"
                , BaseDirectories.get().configDir + "/java-runtime-decompiler/plugins/"};
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DecompilerWrapperInformation.class, new DecompilerWrapperInformationDeserializer());
        Gson gson = gsonBuilder.create();
        for (String location : configLocations) {
            File[] files = new File(location).listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    try {
                        DecompilerWrapperInformation wrapper = gson.fromJson(new FileReader(file.getAbsolutePath()), DecompilerWrapperInformation.class);
                        if (!wrapper.isMalformedURL()) {
                            wrappers.add(wrapper);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Iterates over wrappers returning instance of DecompilerWrapperInformation when supplied decompilerName matches
     *
     * @param decompilerName
     * @return
     */
    private DecompilerWrapperInformation getDecompilerWrapperInformation(String decompilerName) {
        for (DecompilerWrapperInformation wrapperInformation : wrappers) {
            if (decompilerName.equals(wrapperInformation.getName())) {
                return wrapperInformation;
            }
        }
        return null;
    }

    /**
     * @param decompilerName decompiler used for decompiling
     * @param bytecode       bytecode to be decompiled
     * @return Decompiled bytecode or exception String.
     */
    public synchronized String decompile(String decompilerName, byte[] bytecode) throws Exception {
        DecompilerWrapperInformation wrapper = getDecompilerWrapperInformation(decompilerName);
        if (wrapper == null) {
            throw new NullPointerException("Decompiler \"" + decompilerName + "\" not found in list of loaded Decompilers");
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
