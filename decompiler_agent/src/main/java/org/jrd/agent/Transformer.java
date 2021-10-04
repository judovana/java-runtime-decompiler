package org.jrd.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class represent our transformer for retrieving bytecode.
 *
 * @author pmikova
 */
public class Transformer implements ClassFileTransformer {

    private boolean allowToSaveBytecode = false;
    private Map<String, byte[]> results = new HashMap<>();
    private Map<String, byte[]> overrides = new HashMap<>();

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> clazz, ProtectionDomain domain, byte[] classfileBuffer
    ) throws IllegalClassFormatException {
        if (allowToSaveBytecode) {
            byte[] b = null;
            //some parts of instrumentation works on p/k/g/class some on p.l.g.class, lets unify that
            String nameWithoutSlashes = clazz.getName().replace("/", ".");
            synchronized (this) {
                b = overrides.get(nameWithoutSlashes);
            }
            if (b != null) {
                results.put(nameWithoutSlashes, b);
                return b;
            } else {
                results.put(nameWithoutSlashes, classfileBuffer);
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

    public List<String> getOverrides() {
        return Collections.unmodifiableList(new ArrayList<>(overrides.keySet()));
    }

    /**
     * Resets the map with results to empty map
     */
    public void resetLastValidResult() {
        results = new HashMap<>();
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

    synchronized void removeOverride(String clazz) {
        overrides.remove(clazz);
    }

    public synchronized int cleanOverrides(Pattern pattern) {
        int removed = 0;
        List<String> keys = getOverrides();
        for (String key : keys) {
            if (pattern.matcher(key).matches()) {
                removeOverride(key);
                removed++;
                AgentLogger.getLogger().log("Removed " + key + " override");
            }
        }
        return removed;
    }
}
