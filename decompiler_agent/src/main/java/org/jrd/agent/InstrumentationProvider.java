package org.jrd.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

/**
 * This class stores instrumentation and transformer objects and handles the
 * transformation, retrieval of bytecode and class names.
 *
 * @author pmikova
 */
public class InstrumentationProvider {

    private final Transformer transformer;
    private final Instrumentation instrumentation;

    private static final String INFO_DELIMITER = "|";

    InstrumentationProvider(Instrumentation inst, Transformer transformer) {
        this.transformer = transformer;
        this.instrumentation = inst;
    }

    public void setClassBody(String cname, byte[] nwBody) throws UnmodifiableClassException {
        Class clazz = findClass(cname);
        transformer.allowToSaveBytecode();
        try {
            transformer.setOverride(clazz.getName(), nwBody);
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Throwable ex) {
                transformer.removeOverride(clazz.getName());
                throw ex;
            }
        } finally {
            transformer.denyToSaveBytecode();
            transformer.resetLastValidResult();
        }
    }

    byte[] getClassBody(Class clazz) throws UnmodifiableClassException {
        byte[] result;
        transformer.allowToSaveBytecode();
        try {
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Throwable ex) {
                transformer.removeOverride(clazz.getName());
            }
            result = transformer.getResult(clazz.getName());
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
    public byte[] findClassBody(String className) throws UnmodifiableClassException {
        return getClassBody(findClass(className));

    }

    private Class findClass(String className) {
        Class[] classes = instrumentation.getAllLoadedClasses();
        for (Class clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        throw new RuntimeException("Class " + className + " not found in loaded classes.");
    }

    /**
     * Inserts names of classes into queue.
     * Stops execution when it receives abort signal.
     *
     * @param queue output queue
     * @param abort abort signal
     * @throws InterruptedException interrupted exception
     */
    public void getClasses(BlockingQueue<String> queue, Boolean abort, boolean doGetInfo, Optional<ClassFilter> filter)
            throws InterruptedException {
        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class loadedClass : loadedClasses) {
            String className = loadedClass.getName();
            boolean found = false;
            if (filter.isPresent()) {
                found = filter.get().match(this, loadedClass);
            } else {
                found = true;
            }
            if (found) {
                if (doGetInfo) {
                    String location;
                    try {
                        location = loadedClass.getProtectionDomain().getCodeSource().getLocation().getPath();
                    } catch (Exception ex) {
                        location = "unknown";
                    }

                    String classLoader;
                    try {
                        classLoader = loadedClass.getClassLoader().toString();
                    } catch (Exception ex) {
                        classLoader = "unknown";
                    }

                    queue.put(className + INFO_DELIMITER + location + INFO_DELIMITER + classLoader);
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

    public void getOverrides(BlockingQueue<String> queue) throws InterruptedException {
        for (String override : transformer.getOverriddenFqns()) {
            queue.put(override);
        }
        queue.put("---END---");
    }

    public int cleanOverrides(String pattern) {
        List<String> removed = transformer.cleanOverrides(Pattern.compile(pattern));
        try {
            instrumentation.retransformClasses(removed.stream().map(this::findClass).toArray(Class[]::new));
        } catch (RuntimeException | UnmodifiableClassException e) {
            AgentLogger.getLogger().log(e);
        }

        return removed.size();
    }

    public void detach() {
        cleanOverrides(".*"); //optional?
        instrumentation.removeTransformer(transformer);
        Main.setFirstTime(true);
        int loader = Integer.parseInt(System.getProperty(Main.JRD_AGENT_LOADED, "0")) - 1;
        System.setProperty(Main.JRD_AGENT_LOADED, String.valueOf(loader));
        AgentLogger.getLogger().log("done");
    }

    /**
     * Tis was originally solved by custom classloader which had map(string, byte[]) and map(string, clazz).
     * However such class, although visible in class listing and by decompilers,
     * was not usable in tunrime compilation, nor eg initialized by class.forName.
     * The class.forName is good test or the  class being correctly loaded
     *
     * The reflection could be avoided by doing fake classlaoder which was publishing defineClass method and friends,
     * but they are final now
     */
    public void addClass(String className, byte[] b)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {

        Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        m.setAccessible(true);
        Object futureClazz = m.invoke(this.getClass().getClassLoader(), className, b, 0, b.length);
        System.err.println("JRD Agent added " + futureClazz);
        Class.forName(className);
    }
}
