/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.decompiler.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.LinkedBlockingQueue;

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

    private byte[] getClassBody(Class clazz) throws UnmodifiableClassException {
        transformer.allowToSaveBytecode();
        byte[] result;
        try {
            transformer.allowToSaveBytecode();
            instrumentation.retransformClasses(clazz);
            String nameWithSlashes = clazz.getName().replace(".", "/");
            result = transformer.getResult(nameWithSlashes);
        } catch (RuntimeException ex) {
            throw new RuntimeException(ex);
        }

        transformer.denyToSaveBytecode();
        transformer.resetLastValidResult();
        return result;
    }

    /**
     * Finds class object corresponding to the class name and returns its
     * bytecode.
     *
     * @param className name of class we want to get
     * @return bytecode of given class
     * @throws UnmodifiableClassException if the class can not be retransformed
     */
    public byte[] findClassBody(String className) throws UnmodifiableClassException {
        return getClassBody(findClass(className));

    }

    private Class findClass(String className) {
        Class[] classes = instrumentation.getAllLoadedClasses();
        for (Class classe : classes) {
            if (classe.getName().equals(className)) {
                return classe;
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
     */
    public void getClassesNames(LinkedBlockingQueue<String> queue, Boolean abort) throws InterruptedException {
        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class loadedClass : loadedClasses) {
            queue.put(loadedClass.getName());
            if (abort) {
                break;
            }
        }
        queue.put("---END---");
    }
}
