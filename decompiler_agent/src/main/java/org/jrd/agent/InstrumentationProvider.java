package org.jrd.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
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

    private byte[] getClassBody(Class clazz) throws UnmodifiableClassException {
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
    public void getClassesNames(BlockingQueue<String> queue, Boolean abort) throws InterruptedException {
        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class loadedClass : loadedClasses) {
            queue.put(loadedClass.getName());
            if (abort) {
                break;
            }
        }
        queue.put("---END---");
    }

    public void getOverrides(BlockingQueue<String> queue) throws InterruptedException {
        for (String override : transformer.getOverrides()) {
            queue.put(override);
        }
        queue.put("---END---");
    }

    public int cleanOverrides(String pattern) {
        return transformer.cleanOverrides(Pattern.compile(pattern));
    }
}
