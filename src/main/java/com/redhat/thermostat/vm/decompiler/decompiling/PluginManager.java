package com.redhat.thermostat.vm.decompiler.decompiling;

import io.github.soc.directories.BaseDirectories;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Executes manages external decompiler wrapper plugins.
 * Wrapper plugins are stored as .java files along with .info file containing classname, wrapper url and decompiler url.
 * When user calls decompile method with decompiler name and bytecode, wrapper plugin gets compiled and executed.
 */
public class PluginManager {

    private List<DecompilerWrapperInformation> Wrappers;

    public PluginManager() {
        loadConfigs();
    }

    /**
     * Loads information about available decompilers into List <DecompilerWrapperInformation> Wrapper.
     */
    private void loadConfigs() {
        Wrappers = new LinkedList<>();
        String[] configLocations = new String[]{"/etc/java-runtime-decompiler/plugins/"
                , "/usr/share/java/java-runtime-decompiler/plugins"
                , BaseDirectories.get().configDir + "/java-runtime-decompiler/plugins/"};
        for (String location : configLocations) {
            File[] files = new File(location).listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (file.getName().endsWith(".info")) {
                    try {
                        BufferedReader infoFile = new BufferedReader(new FileReader(file.getAbsolutePath()));
                        String[] parms = infoFile.readLine().split(";");
                        Wrappers.add(new DecompilerWrapperInformation(parms[0], parms[1], parms[2], parms[3], location));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Iterates over Wrappers returning instance of DecompilerWrapperInformation when supplied decompilerName matches
     *
     * @param decompilerName
     * @return
     */
    private DecompilerWrapperInformation getDecompilerWrapperInformation(String decompilerName) {
        for (DecompilerWrapperInformation wrapperInformation : Wrappers) {
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
                    "-cp", wrapper.getDecompilerURL().getPath(), "-d", System.getProperty("java.io.tmpdir"));
            // Load wrapper
            URL url = wrapper.getDecompilerURL();
            URL url2 = new URL("file://" + System.getProperty("java.io.tmpdir") + "/");
            ClassLoader loader = URLClassLoader.newInstance(
                    new URL[]{url, url2},
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

}
