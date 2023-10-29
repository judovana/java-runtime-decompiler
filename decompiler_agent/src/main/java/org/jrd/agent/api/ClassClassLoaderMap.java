package org.jrd.agent.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (submap == null) {
            //FIXME is this correct?
            return;
        }
        submap.remove(classloader);
        if (submap.isEmpty()) {
            map.remove(clazz);
        }
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
        byte[] targettedReturn = classes.get(classlaoder);
        if (targettedReturn == null && !classes.isEmpty()) {
            return new ArrayList<>(classes.values()).get(0);
        } else {
            return targettedReturn;
        }
    }

    public byte[] getStrict(String classname, String classlaoder) {
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
        return keySetPairs().stream().map(a -> a[0] + ":" + a[1]).collect(Collectors.toList());
    }

    public List<String[]> keySetPairs() {
        List<String[]> r = new ArrayList<>();
        for (Map.Entry<String, Map<String, byte[]>> fqn : map.entrySet()) {
            for (String classloader : fqn.getValue().keySet()) {
                r.add(new String[]{fqn.getKey(), nullClassloaderToUnknown(classloader)});
            }
        }
        return r;
    }

    public static String nullClassloaderToUnknown(String classloader) {
        return classloader == null ? "unknown" : classloader;
    }

    public static String unknownToNullClasslaoder(String unknown) {
        return "unknown".equals(unknown) ? null : unknown;
    }
}
