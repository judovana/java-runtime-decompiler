package org.jrd.agent.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassClassLoaderMap {
    // class->classlaoder->byte[]
    private Map<String, Map<String, byte[]>> map = new HashMap<>();

    public void reset() {
        map = new HashMap<>();
    }

    public void remove(String clazz) {
        map.remove(clazz);
    }

    public void remove(String clazz, String classloader) {
        Map<String, byte[]> submap = map.get(clazz);
        submap.remove(classloader);
    }

    public byte[] get(String classname) {
        Map<String, byte[]> classes = map.get(classname);
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        byte[] nullLoader = classes.get(null);
        if (nullLoader != null) {
            return nullLoader;
        }
        return new ArrayList<byte[]>(classes.values()).get(0);
    }

    public byte[] get(String classname, String classlaoder) {
        Map<String, byte[]> classes = map.get(classname);
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        return classes.get(classlaoder);
    }

    public void put(String nameWithoutSlashes, byte[] classfileBuffer, String loader) {
        Map<String, byte[]> classes = map.get(nameWithoutSlashes);
        if (classes == null) {
            classes = new HashMap<>();
            map.put(nameWithoutSlashes, classes);
        }
        classes.put(loader, classfileBuffer);
    }

    public List<String> keySet() {
        return new ArrayList<>();
    }
}
