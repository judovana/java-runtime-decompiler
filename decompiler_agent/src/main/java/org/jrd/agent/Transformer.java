package org.jrd.agent;

import org.jrd.agent.api.ClassClassLoaderMap;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                b = overrides.getStrict(nameWithoutSlashes, AgentLogger.classLoaderId(loader));
            }
            if (b != null) {
                results.put(nameWithoutSlashes, b, AgentLogger.classLoaderId(loader));
                return b;
            } else {
                results.put(nameWithoutSlashes, classfileBuffer, AgentLogger.classLoaderId(loader));
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

    public List<String[]> getOverriddenFqnPairs() {
        return Collections.unmodifiableList(new ArrayList<>(overrides.keySetPairs()));
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

    public synchronized List<String[]> cleanOverrides(String patterns) {
        List<String[]> fqns = getOverriddenFqnPairs();
        List<String[]> removed = new ArrayList<>();

        String fqn = ".*";
        String loader = ".*";

        if (patterns.contains(":")) {
            fqn = patterns.split(":")[0];
            loader = patterns.replace(fqn.toString() + ":", "");
        } else {
            fqn = patterns;
        }

        for (String[] fqnAndLoader : fqns) {
            if (Main.equalsOrMatching(fqnAndLoader[0], fqn) && Main.equalsOrMatching(fqnAndLoader[1], loader)) {
                String cl = ClassClassLoaderMap.unknownToNullClasslaoder(fqnAndLoader[1]);
                removeOverride(fqnAndLoader[0], cl);
                removed.add(new String[]{fqnAndLoader[0], cl});
                AgentLogger.getLogger().log("Removed  override " + fqnAndLoader[0] + ":" + fqnAndLoader[1]);
            }
        }
        return removed;
    }
}
