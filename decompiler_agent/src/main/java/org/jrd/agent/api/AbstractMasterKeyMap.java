package org.jrd.agent.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMasterKeyMap<T> {

    protected final Map<T, Map<String, Object>> values;

    public AbstractMasterKeyMap() {
        this.values = createMainMap();
    }

    protected abstract Map<T, Map<String, Object>> createMainMap();

    protected abstract Map<String, Object> getSubMap(T mainKey);

    public Object set(T key, String name, Object value) {
        Map<String, Object> subMap = getSubMap(key);
        Object old = subMap.put(name, value);
        if ("jrd.debug".equals(old)) {
            System.err.println("We do not care about old.");
        }
        return value;
    }

    public Object setNoReplace(T key, String name, Object value) throws Variables.FakeVariableAlreadyDeclaredException {
        Map<String, Object> subMap = getSubMap(key);
        if (subMap.containsKey(name)) {
            throw new Variables.FakeVariableAlreadyDeclaredException();
        } else {
            Object old = subMap.put(name, value);
            if (old != null) {
                throw new IllegalStateException("Map is broken!");
            }
            return value;
        }
    }

    public Object get(T key, String name) throws Variables.NoSuchFakeVariableException {
        Map<String, Object> subMap = getSubMap(key);
        if (subMap.containsKey(name)) {
            return subMap.get(name);
        } else {
            throw new Variables.NoSuchFakeVariableException();
        }
    }

    public Object getOrCreate(T key, String name, Object defaultValue) {
        Map<String, Object> subMap = getSubMap(key);
        if (subMap.containsKey(name)) {
            return subMap.get(name);
        } else {
            Object old = subMap.put(name, defaultValue);
            if (old != null) {
                throw new IllegalStateException("Map is broken!");
            }
            return defaultValue;
        }
    }

    public Object create(T key, String name, Object defaultValue) throws Variables.FakeVariableAlreadyDeclaredException {
        Map<String, Object> subMap = getSubMap(key);
        if (subMap.containsKey(name)) {
            throw new Variables.FakeVariableAlreadyDeclaredException();
        } else {
            return subMap.put(name, defaultValue);
        }
    }

    public Object remove(T key, String name) throws Variables.NoSuchFakeVariableException {
        Map<String, Object> subMap = getSubMap(key);
        if (subMap.containsKey(name)) {
            return subMap.remove(name);
        } else {
            throw new Variables.NoSuchFakeVariableException();
        }
    }

    public void removeAll(T key) {
        Map<String, Object> subMap = getSubMap(key);
        subMap.clear();
    }

    public void destroy() {
        values.clear();
    }

    public String dump(T... selection) {
        Set<Map.Entry<T, Map<String, Object>>> mainFull = values.entrySet();
        Set<Map.Entry<T, Map<String, Object>>> main = new HashSet<>();
        for (Map.Entry<T, Map<String, Object>> subtable : mainFull) {
            for (T select : selection) {
                if (select == null && subtable.getKey() == null) {
                    main.add(subtable);
                } else {
                    if (subtable.getKey() != null && subtable.getKey().equals(select)) {
                        main.add(subtable);
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder(
                "table " + this.getClass().getName() + ": " + main.size() + " required groups " + selection.length + " found " + main.size()
        );
        sb.append("\n");
        iterateMainToSb(main, sb);
        sb.append("end " + this.getClass().getName());
        sb.append("\n");
        return sb.toString();
    }

    public String dump() {
        Set<Map.Entry<T, Map<String, Object>>> main = values.entrySet();
        StringBuilder sb = new StringBuilder("table " + this.getClass().getName() + ": " + main.size() + " groups");
        sb.append("\n");
        iterateMainToSb(main, sb);
        sb.append("end " + this.getClass().getName());
        sb.append("\n");
        return sb.toString();
    }

    private void iterateMainToSb(Set<Map.Entry<T, Map<String, Object>>> main, StringBuilder sb) {
        for (Map.Entry<T, Map<String, Object>> subtable : main) {
            sb.append("  " + dumpKey(subtable.getKey()) + ": " + subtable.getValue().entrySet().size() + " items");
            sb.append("\n");
            for (Map.Entry<String, Object> leaf : subtable.getValue().entrySet()) {
                sb.append("    " + dumpKey(subtable.getKey()) + "/" + leaf.getKey() + "=" + leaf.getValue());
                sb.append("\n");
            }
        }
    }

    protected String dumpKey(T key) {
        if (key == null) {
            return null;
        }
        if (key.toString().length() > 50) {
            return key.getClass().getSimpleName() + "@" + key.hashCode();
        } else {
            return key.toString();
        }
    }

}
