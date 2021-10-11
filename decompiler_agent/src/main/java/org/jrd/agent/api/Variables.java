package org.jrd.agent.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Variables {

    /**
     * We have to have all internal classes initialized so any later classes can use them without a manual init.
     */
    public static void init() {
        FakeVariableException.init();
        NoSuchFakeVariableException.init();
        FakeVariableAlreadyDeclaredException.init();
        Global.init();
        Local.init();
    }

    protected Variables() {
    }

    public static class FakeVariableException extends RuntimeException {

        public FakeVariableException() {
        }

        public FakeVariableException(String s) {
            super(s);
        }

        public static void init() {
        }

    }

    public static class NoSuchFakeVariableException extends FakeVariableException {

        public NoSuchFakeVariableException() {
        }

        public NoSuchFakeVariableException(String s) {
            super(s);
        }

        public static void init() {
        }
    }

    public static class FakeVariableAlreadyDeclaredException extends RuntimeException {

        public FakeVariableAlreadyDeclaredException() {
        }

        public FakeVariableAlreadyDeclaredException(String s) {
            super(s);
        }

        public static void init() {
        }
    }

    public static class Global {

        protected Global() {
        }

        // todo, remove synchronisedMap, and create second, synchronised api
        private static final Map<String, Object> GLOBALS = Collections.synchronizedMap(new HashMap<>());

        public static void init() {
        }

        public static Object set(String name, Object value) {
            Object old = GLOBALS.put(name, value);
            if ("jrd.debug".equals(old)) {
                System.err.println("We do not care about old.");
            }
            return value;
        }

        public static Object setNoReplace(String name, Object value) throws NoSuchFakeVariableException {
            if (GLOBALS.containsKey(name)) {
                throw new FakeVariableAlreadyDeclaredException();
            } else {
                Object old = GLOBALS.put(name, value);
                if (old != null) {
                    throw new IllegalStateException("Map is broken!");
                }
                return value;
            }
        }

        public static Object get(String name) throws NoSuchFakeVariableException {
            if (GLOBALS.containsKey(name)) {
                return GLOBALS.get(name);
            } else {
                throw new NoSuchFakeVariableException();
            }
        }

        public static Object getOrCreate(String name, Object defaultValue) {
            if (GLOBALS.containsKey(name)) {
                return GLOBALS.get(name);
            } else {
                Object old = GLOBALS.put(name, defaultValue);
                if (old != null) {
                    throw new IllegalStateException("Map is broken!");
                }
                return defaultValue;
            }
        }

        public static Object create(String name, Object defaultValue) throws NoSuchFakeVariableException {
            if (GLOBALS.containsKey(name)) {
                throw new FakeVariableAlreadyDeclaredException();
            } else {
                return GLOBALS.put(name, defaultValue);
            }
        }

        public static Object remove(String name) throws NoSuchFakeVariableException {
            if (GLOBALS.containsKey(name)) {
                return GLOBALS.remove(name);
            } else {
                throw new NoSuchFakeVariableException();
            }
        }

        public static void removeAll() {
            GLOBALS.clear();
        }
    }

    public static class Local {

        protected Local() {
        }

        // todo, remove synchronisedMap, and create second, synchronised api
        private static final Map<Object, Map<String, Object>> LOCALS = Collections.synchronizedMap(new HashMap<>());

        public static void init() {
        }

        private static synchronized Map<String, Object> getCreateIfNecessary(Object owner) {
            Map<String, Object> thisOnes = LOCALS.get(owner);
            if (thisOnes == null) {
                thisOnes = new HashMap<>();
                LOCALS.put(owner, thisOnes);
            }
            return thisOnes;
        }

        public static Object set(Object owner, String name, Object value) {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            Object old = thisOnes.put(name, value);
            if ("jrd.debug".equals(old)) {
                System.err.println("We do not care about old.");
            }
            return value;
        }

        public static Object setNoReplace(Object owner, String name, Object value) throws NoSuchFakeVariableException {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            if (thisOnes.containsKey(name)) {
                throw new FakeVariableAlreadyDeclaredException();
            } else {
                Object old = thisOnes.put(name, value);
                if (old != null) {
                    throw new IllegalStateException("Map is broken!");
                }
                return value;
            }
        }

        public static Object get(Object owner, String name) throws NoSuchFakeVariableException {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            if (thisOnes.containsKey(name)) {
                return thisOnes.get(name);
            } else {
                throw new NoSuchFakeVariableException();
            }
        }

        public static Object getOrCreate(Object owner, String name, Object defaultValue) {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            if (thisOnes.containsKey(name)) {
                return thisOnes.get(name);
            } else {
                Object old = thisOnes.put(name, defaultValue);
                if (old != null) {
                    throw new IllegalStateException("Map is broken!");
                }
                return defaultValue;
            }
        }

        public static Object create(Object owner, String name, Object defaultValue) throws NoSuchFakeVariableException {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            if (thisOnes.containsKey(name)) {
                throw new FakeVariableAlreadyDeclaredException();
            } else {
                return thisOnes.put(name, defaultValue);
            }
        }

        public static Object remove(Object owner, String name) throws NoSuchFakeVariableException {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            if (thisOnes.containsKey(name)) {
                return thisOnes.remove(name);
            } else {
                throw new NoSuchFakeVariableException();
            }
        }

        public static void removeAll(Object owner) {
            Map<String, Object> thisOnes = getCreateIfNecessary(owner);
            thisOnes.clear();
            LOCALS.remove(owner);
        }

        public static void removeAll() {
            LOCALS.clear();
        }
    }
}
