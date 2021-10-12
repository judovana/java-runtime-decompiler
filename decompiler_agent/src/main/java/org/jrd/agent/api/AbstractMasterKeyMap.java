package org.jrd.agent.api;

import java.util.Map;

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

    public Object setNoReplace(T key, String name, Object value) throws Variables.NoSuchFakeVariableException {
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

    public Object create(T key, String name, Object defaultValue) throws Variables.NoSuchFakeVariableException {
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
}
