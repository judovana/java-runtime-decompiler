package org.jrd.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * This class stores instrumentation and transformer objects and handles the
 * transformation, retrieval of bytecode and class names.
 *
 * @author pmikova
 */
public class InstrumentationProvider {

    private final Transformer transformer;
    private final Instrumentation instrumentation;
    private final String loneliness;
    private final String origArgs;
    private static final String INFO_DELIMITER = "|";

    InstrumentationProvider(Instrumentation inst, Transformer transformer, String loneliness, String origArgs) {
        this.transformer = transformer;
        this.instrumentation = inst;
        this.loneliness = loneliness;
        this.origArgs = origArgs;
    }

    public void setClassBody(String cname, byte[] nwBody, String classloader) throws UnmodifiableClassException {
        Class clazz = findClass(cname, classloader);
        transformer.allowToSaveBytecode();
        try {
            transformer.setOverride(clazz.getName(), nwBody, AgentLogger.classLoaderId(clazz.getClassLoader()));
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Throwable ex) {
                transformer.removeOverride(clazz.getName(), AgentLogger.classLoaderId(clazz.getClassLoader()));
                throw ex;
            }
        } finally {
            transformer.denyToSaveBytecode();
            transformer.resetLastValidResult();
        }
    }

    byte[] getClassBody(Class clazz, String classloader) throws UnmodifiableClassException {
        byte[] result;
        transformer.allowToSaveBytecode();
        try {
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Throwable ex) {
                transformer.removeOverride(clazz.getName(), classloader);
            }
            result = transformer.getResult(clazz.getName(), classloader);
        } finally {
            transformer.denyToSaveBytecode();
            transformer.resetLastValidResult();
        }
        return result;
    }

    /**
     * Finds class object corresponding to the class name and returns its
     * bytecode.
     *
     * @param className name of class we want to get
     * @return bytecode of given class
     * @throws UnmodifiableClassException if the class can not be re-transformed
     */
    public byte[] findClassBody(String className, String classloader) throws UnmodifiableClassException {
        return getClassBody(findClass(className, classloader), classloader);

    }

    private Class findClass(String className, String classLoader) {
        Class[] classes = instrumentation.getAllLoadedClasses();
        if (classLoader == null || "unknown".equals(classLoader)) {
            for (Class clazz : classes) {
                if (clazz.getName().equals(className)) {
                    return clazz;
                }
            }
        } else {
            for (Class clazz : classes) {
                if (clazz.getName().equals(className) && Main.equalsOrMatching(clazz.getClassLoader(), classLoader)) {
                    return clazz;
                }
            }
        }
        throw new RuntimeException("Class " + className + " not found in loaded classes. In classloader " + classLoader);
    }

    /**
     * Inserts names of classes into queue.
     * Stops execution when it receives abort signal.
     *
     * @param queue output queue
     * @param abort abort signal
     * @throws InterruptedException interrupted exception
     */
    public void getClasses(BlockingQueue<String> queue, Boolean abort, boolean doGetInfo, Optional<ClassFilter> filter, String classlaoder)
            throws InterruptedException {
        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class loadedClass : loadedClasses) {
            String className = loadedClass.getName();
            boolean found = false;
            if (filter.isPresent()) {
                found = classloaderMatches(classlaoder, loadedClass);
                if (found) {
                    found = filter.get().match(this, loadedClass, classlaoder);
                }
            } else {
                found = classloaderMatches(classlaoder, loadedClass);
            }
            if (found) {
                if (doGetInfo) {
                    String location;
                    String module;
                    String moduleloader;
                    try {
                        location = loadedClass.getProtectionDomain().getCodeSource().getLocation().getPath();
                    } catch (Throwable ex) {
                        location = "unknown";
                    }
                    try {
                        module = AgentLogger.moduleId(loadedClass.getModule());
                    } catch (Throwable ex) {
                        module = "unknown";
                    }
                    try {
                        if (loadedClass.getModule() != null) {
                            moduleloader = AgentLogger.classLoaderId(loadedClass.getModule().getClassLoader());
                        } else {
                            moduleloader = "unknown";
                        }
                    } catch (Throwable ex) {
                        moduleloader = "unknown";
                    }
                    String classLoader;
                    classLoader = AgentLogger.classLoaderId(loadedClass.getClassLoader());
                    queue.put(
                            className + INFO_DELIMITER + location + INFO_DELIMITER + classLoader + INFO_DELIMITER + module +
                                    INFO_DELIMITER + moduleloader
                    );
                } else {
                    queue.put(className);
                }
            }
            if (abort) {
                break;
            }
        }
        queue.put("---END---");
    }

    private static boolean classloaderMatches(String classlaoder, Class loadedClass) {
        boolean found;
        if (classlaoder == null) {
            found = true;
        } else {
            if ("unknown".equals(classlaoder)) {
                found = loadedClass.getClassLoader() == null;
            } else {
                found = loadedClass.getClassLoader() != null && Main.equalsOrMatching(loadedClass.getClassLoader(), classlaoder);
            }
        }
        return found;
    }

    public void getOverrides(BlockingQueue<String> queue) throws InterruptedException {
        for (String override : transformer.getOverriddenFqns()) {
            queue.put(override);
        }
        queue.put("---END---");
    }

    public int cleanOverrides(String pattern) {
        List<String[]> removed = transformer.cleanOverrides(pattern);
        try {
            instrumentation.retransformClasses(removed.stream().map(a -> {
                return this.findClass(a[0], a[1]);
            }).toArray(Class[]::new));
        } catch (RuntimeException | UnmodifiableClassException e) {
            AgentLogger.getLogger().log(e);
        }

        return removed.size();
    }

    public void detach() {
        Main.deregister(loneliness, origArgs);
        cleanOverrides(".*"); //optional?
        instrumentation.removeTransformer(transformer);
        int loader = Integer.parseInt(System.getProperty(Main.JRD_AGENT_LOADED, "0")) - 1;
        System.setProperty(Main.JRD_AGENT_LOADED, String.valueOf(loader));
        AgentLogger.getLogger().log("done");
    }

    /**
     * Tis was originally solved by custom classloader which had map(string, byte[]) and map(string, clazz).
     * However such class, although visible in class listing and by decompilers,
     * was not usable in tunrime compilation, nor eg initialized by class.forName.
     * The class.forName is good test or the  class being correctly loaded
     * <p>
     * <p>
     * The reflection could be avoided by doing fake classlaoder which was publishing defineClass method and friends,
     * but they are final now
     * <p>
     * Waring! Do not work with target process JDK17, that would eed --add-opens java.base or similar:(
     */
    public void addClass(String className, byte[] b)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        m.setAccessible(true);
        Object futureClazz = m.invoke(this.getClass().getClassLoader(), className, b, 0, b.length);
        AgentLogger.getLogger().log("JRD Agent added " + futureClazz);
        Class.forName(className);
    }

    public void addJar(String jarOrigName, byte[] decoded) throws IOException {
        File tmp = File.createTempFile("jrdagent", ".jar");
        Files.write(tmp.toPath(), decoded);
        tmp.deleteOnExit();
        AgentLogger.getLogger().log("Jrd agent added client's " + jarOrigName + " as " + tmp.getAbsolutePath());
        if (jarOrigName.startsWith("BOOT/")) {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(tmp));
        } else {
            instrumentation.appendToSystemClassLoaderSearch(new JarFile(tmp));
        }
        JarFile jf = new JarFile(tmp);
        int loaded = 0;
        int skipped = 0;
        int failed = 0;
        try {
            for (Enumeration list = jf.entries(); list.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) list.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().endsWith("/module-info.class") &&
                        !"module-info.class".equals(entry.getName())) {
                    String clazz = entry.getName().replace('/', '.').replaceAll("\\.class$", "");
                    try {
                        Class.forName(clazz); //throws error
                        loaded++;
                    } catch (Throwable ex) {
                        AgentLogger.getLogger().log("Failed to load: " + clazz + " - " + ex.getMessage());
                        failed++;
                    }
                } else {
                    skipped++;
                }
            }
        } finally {
            jf.close();
        }
        AgentLogger.getLogger().log("total classes loaded: " + loaded + "; skipped: " + skipped + "; failed to load:" + failed);
    }
}
