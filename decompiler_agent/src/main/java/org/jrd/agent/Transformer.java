/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jrd.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;

/**
 * This class represent our transformer for retrieving bytecode.
 *
 * @author pmikova
 */
public class Transformer implements ClassFileTransformer {

    private boolean allowToSaveBytecode = false;
    private HashMap<String, byte[]> results = new HashMap<>();
    private HashMap<String, byte[]> overrides = new HashMap<>();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (allowToSaveBytecode) {
            results.put(className, classfileBuffer);
            byte[] b = overrides.get(className);
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    /**
     * Returns bytecode of transformed class.
     *
     * @param name name of class we want to get
     * @return bytes of given class
     */
    public byte[] getResult(String name) {
        return results.get(name);
    }
    
    public void setOverride(String name, byte[] body) {
        overrides.put(name, body);
    }

    /**
     * Resets the map with results to empty map
     */
    public void resetLastValidResult() {
        results = new HashMap<>();
    }
    
    public void resetOverrides() {
        overrides = new HashMap<>();
    }

    /**
     * This method allows saving of bytecode
     */
    public void allowToSaveBytecode() {
        allowToSaveBytecode = true;
    }

    /**
     * This method denies the bytecode to be saved during transformation.
     */
    public void denyToSaveBytecode() {
        allowToSaveBytecode = false;
    }
}
