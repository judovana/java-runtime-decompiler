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

        public FakeVariableException(Exception ex) {
            super(ex);
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

        private static final AbstractMasterKeyMap<String> GLOBALS = new AbstractMasterKeyMap<>() {

            //globals do not have any granularity, so there is just one key to rule them all
            private static final String THE_KEY = "THE_KEY";

            @Override
            protected synchronized Map<String, Map<String, Object>> createMainMap() {
                Map<String, Map<String, Object>> mapWithSingleMaster = Collections.synchronizedMap(new HashMap<>());
                mapWithSingleMaster.put(THE_KEY, new HashMap<>());
                return mapWithSingleMaster;
            }

            @Override
            protected synchronized Map<String, Object> getSubMap(String mainKey) {
                return values.get(THE_KEY);
            }
        };

        public static void init() {
        }

        public static Object set(String name, Object value) {
            return GLOBALS.set(null, name, value);
        }

        public static Object setNoReplace(String name, Object value) throws NoSuchFakeVariableException {
            return GLOBALS.setNoReplace(null, name, value);
        }

        public static Object get(String name) throws NoSuchFakeVariableException {
            return GLOBALS.get(null, name);
        }

        public static Object getOrCreate(String name, Object defaultValue) {
            return GLOBALS.getOrCreate(null, name, defaultValue);
        }

        public static Object create(String name, Object defaultValue) throws NoSuchFakeVariableException {
            return GLOBALS.create(null, name, defaultValue);
        }

        public static Object remove(String name) throws NoSuchFakeVariableException {
            return GLOBALS.remove(null, name);
        }

        public static void removeAll() {
            GLOBALS.removeAll(null);
        }
    }

    public static class Local {

        protected Local() {
        }

        // todo, remove synchronisedMap, and create second, synchronised api
        private static final AbstractMasterKeyMap<Object> LOCALS = new AbstractMasterKeyMap<>() {

            @Override
            protected synchronized Map<Object, Map<String, Object>> createMainMap() {
                return Collections.synchronizedMap(new HashMap<>());
            }

            @Override
            protected synchronized Map<String, Object> getSubMap(Object owner) {
                Map<String, Object> thisOnes = values.get(owner);
                if (thisOnes == null) {
                    thisOnes = new HashMap<>();
                    values.put(owner, thisOnes);
                }
                return thisOnes;
            }
        };

        public static void init() {
        }

        public static Object set(Object owner, String name, Object value) {
            return LOCALS.set(owner, name, value);
        }

        public static Object setNoReplace(Object owner, String name, Object value) throws NoSuchFakeVariableException {
            return LOCALS.setNoReplace(owner, name, value);
        }

        public static Object get(Object owner, String name) throws NoSuchFakeVariableException {
            return LOCALS.get(owner, name);
        }

        public static Object getOrCreate(Object owner, String name, Object defaultValue) {
            return LOCALS.getOrCreate(owner, name, defaultValue);
        }

        public static Object create(Object owner, String name, Object defaultValue) throws NoSuchFakeVariableException {
            return LOCALS.getOrCreate(owner, name, defaultValue);
        }

        public static Object remove(Object owner, String name) throws NoSuchFakeVariableException {
            return LOCALS.remove(owner, name);
        }

        public static void removeAll(Object owner) {
            LOCALS.removeAll(owner);
        }

        public static void destroy() {
            LOCALS.destroy();
        }
    }


}
