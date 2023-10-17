package org.jrd.agent;

import org.jrd.agent.api.ClassClassLoaderMap;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represent our transformer for retrieving bytecode.
 *
 * @author pmikova
 */
public class Transformer implements ClassFileTransformer {

    private boolean allowToSaveBytecode = false;
    private ClassClassLoaderMap results = new ClassClassLoaderMap();
    private ClassClassLoaderMap overrides = new ClassClassLoaderMap();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> clazz, ProtectionDomain domain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        if (allowToSaveBytecode) {
            byte[] b = null;
            //some parts of instrumentation works on p/k/g/class some on p.l.g.class, lets unify that
            String nameWithoutSlashes = clazz.getName().replace("/", ".");
            synchronized (this) {
                b = overrides.get(nameWithoutSlashes, loader.toString());
            }
            if (b != null) {
                results.put(nameWithoutSlashes, b, loader.toString());
                return b;
            } else {
                results.put(nameWithoutSlashes, classfileBuffer, loader.toString());
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
    public byte[] getResult(String name, String classloader) {
        return results.get(name, classloader);
    }

    public void setOverride(String name, byte[] body, String classloader) {
        overrides.put(name, body, classloader);
    }

    public List<String> getOverriddenFqns() {
        return Collections.unmodifiableList(new ArrayList<>(overrides.keySet()));
    }

    /**
     * Resets the map with results to empty map
     */
    public void resetLastValidResult() {
        results.reset();
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

    synchronized void removeOverride(String clazz, String classloader) {
        overrides.remove(clazz, classloader);
    }

    public synchronized List<String> cleanOverrides(Pattern cleanPattern) {
        List<String> fqns = getOverriddenFqns();
        List<String> removed = new ArrayList<>();

        for (String fqn : fqns) {
            if (cleanPattern.matcher(fqn).matches()) {
                removeOverride(fqn);
                removed.add(fqn);
                AgentLogger.getLogger().log("Removed " + fqn + " override");
            }
        }
        return removed;
    }
}
