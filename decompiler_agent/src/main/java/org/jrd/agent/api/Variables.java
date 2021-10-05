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
        FakeVariableAlreadyDeclared.init();
        Global.init();
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

    public static class FakeVariableAlreadyDeclared extends RuntimeException {

        public FakeVariableAlreadyDeclared() {
        }

        public FakeVariableAlreadyDeclared(String s) {
            super(s);
        }

        public static void init() {
        }
    }

    public static class Global {

        protected Global() {
        }

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
                throw new FakeVariableAlreadyDeclared();
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
                throw new FakeVariableAlreadyDeclared();
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

        public static void removeAll() throws NoSuchFakeVariableException {
            GLOBALS.clear();
        }
    }
}
