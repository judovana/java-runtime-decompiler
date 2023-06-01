package org.jrd.agent.api;

import org.jrd.agent.AgentLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jrd.agent.api.Variables.NoSuchFakeVariableException;
import static org.jrd.agent.api.Variables.FakeVariableException;
import static org.jrd.agent.api.Variables.FakeVariableAlreadyDeclaredException;

public class UnsafeVariables {

    /**
     * unsafe variabels are nto synchronised at all. Use with care!
     */
    public static void init() {
        Global.init();
        Local.init();
        Clazzs.init();
    }

    public static String dumpAll() {
        StringBuilder sb = new StringBuilder();
        sb.append(Global.dump());
        sb.append(Local.dump());
        sb.append(Clazzs.dump());
        return sb.toString();
    }

    protected UnsafeVariables() {
    }

    public static class Global {

        private static final AbstractMasterKeyMap<String> GLOBALS = new AbstractMasterKeyMap<String>() {

            //globals do not have any granularity, so there is just one key to rule them all
            private static final String THE_KEY = "GLOBALS_UNSAFE";

            @Override
            protected Map<String, Map<String, Object>> createMainMap() {
                Map<String, Map<String, Object>> mapWithSingleMaster = new HashMap<>();
                mapWithSingleMaster.put(THE_KEY, new HashMap<>());
                return mapWithSingleMaster;
            }

            @Override
            protected Map<String, Object> getSubMap(String mainKey) {
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

        public static Object create(String name, Object defaultValue) throws FakeVariableAlreadyDeclaredException {
            return GLOBALS.create(null, name, defaultValue);
        }

        public static Object remove(String name) throws NoSuchFakeVariableException {
            return GLOBALS.remove(null, name);
        }

        public static void removeAll() {
            GLOBALS.removeAll(null);
        }

        public static String dump() {
            return GLOBALS.dump();
        }
    }

    public static class Local {

        protected Local() {
        }

        private static final AbstractMasterKeyMap<Object> LOCALS = new AbstractMasterKeyMap<Object>() {

            @Override
            protected Map<Object, Map<String, Object>> createMainMap() {
                return new HashMap<>();
            }

            @Override
            protected Map<String, Object> getSubMap(Object owner) {
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

        public static Object create(Object owner, String name, Object defaultValue) throws FakeVariableAlreadyDeclaredException {
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

        public static String dump() {
            return LOCALS.dump();
        }
    }

    public static class Clazzs {

        protected Clazzs() {
        }

        private static final AbstractMasterKeyMap<Class> CLAZZS = new AbstractMasterKeyMap<Class>() {

            @Override
            protected Map<Class, Map<String, Object>> createMainMap() {
                return new HashMap<>();
            }

            @Override
            protected Map<String, Object> getSubMap(Class owner) {
                if (owner == null) {
                    owner = findCaller();
                }
                Map<String, Object> thisOnes = values.get(owner);
                if (thisOnes == null) {
                    thisOnes = new HashMap<>();
                    values.put(owner, thisOnes);
                }
                return thisOnes;
            }

            private Class findCaller() throws FakeVariableException {
                List<String> classes =
                        Arrays.stream(Thread.currentThread().getStackTrace()).map(a -> a.getClassName()).collect(Collectors.toList());
                //0 is alwways java.lang.Thread
                for (int x = 1; x < classes.size(); x++) {
                    String clazz = classes.get(x);
                    if (!clazz.startsWith("org.jrd.agent.api.")) {
                        AgentLogger.getLogger().log("Found: " + clazz);
                        return classForName(clazz);
                    }
                }
                String clazz = classes.get(classes.size() - 1);
                AgentLogger.getLogger().log("Notfound, fallback: " + clazz);
                return classForName(clazz);
            }
        };

        public static void init() {
        }

        private static Class classForName(String clazz) throws FakeVariableException {
            try {
                return Class.forName(clazz);
            } catch (ClassNotFoundException ex) {
                throw new FakeVariableException(ex);
            }
        }

        private static Class nullOrClass(String fqn) throws FakeVariableException {
            if (fqn == null) {
                return null;
            }
            return classForName(fqn);
        }

        public static Object set(String name, Object value) throws FakeVariableException {
            return set((Class) null, name, value);
        }

        public static Object set(String owner, String name, Object value) throws FakeVariableException {
            return set(nullOrClass(owner), name, value);
        }

        public static Object set(Class owner, String name, Object value) {
            return CLAZZS.set(owner, name, value);
        }

        public static Object setNoReplace(String name, Object value) throws FakeVariableException {
            return setNoReplace((Class) null, name, value);
        }

        public static Object setNoReplace(String owner, String name, Object value) throws FakeVariableException {
            return setNoReplace(nullOrClass(owner), name, value);
        }

        public static Object setNoReplace(Class owner, String name, Object value) throws NoSuchFakeVariableException {
            return CLAZZS.setNoReplace(owner, name, value);
        }

        public static Object get(String name) throws FakeVariableException {
            return get((Class) null, name);
        }

        public static Object get(String owner, String name) throws FakeVariableException {
            return get(nullOrClass(owner), name);
        }

        public static Object get(Class owner, String name) throws NoSuchFakeVariableException {
            return CLAZZS.get(owner, name);
        }

        public static Object getOrCreate(String name, Object defaultValue) throws FakeVariableException {
            return getOrCreate((Class) null, name, defaultValue);
        }

        public static Object getOrCreate(String owner, String name, Object defaultValue) throws FakeVariableException {
            return getOrCreate(nullOrClass(owner), name, defaultValue);
        }

        public static Object getOrCreate(Class owner, String name, Object defaultValue) {
            return CLAZZS.getOrCreate(owner, name, defaultValue);
        }

        public static Object create(String name, Object defaultValue) throws FakeVariableAlreadyDeclaredException {
            return create((Class) null, name, defaultValue);
        }

        public static Object create(String owner, String name, Object defaultValue) throws FakeVariableAlreadyDeclaredException {
            return create(nullOrClass(owner), name, defaultValue);
        }

        public static Object create(Class owner, String name, Object defaultValue) throws FakeVariableAlreadyDeclaredException {
            return CLAZZS.create(owner, name, defaultValue);
        }

        public static Object remove(String name) throws NoSuchFakeVariableException {
            return remove((Class) null, name);
        }

        public static Object remove(String owner, String name) throws NoSuchFakeVariableException {
            return remove(nullOrClass(owner), name);
        }

        public static Object remove(Class owner, String name) throws NoSuchFakeVariableException {
            return CLAZZS.remove(owner, name);
        }

        public static void removeAll() throws FakeVariableException {
            removeAll((Class) null);
        }

        public static void removeAll(String owner) throws FakeVariableException {
            removeAll(nullOrClass(owner));
        }

        public static void removeAll(Class owner) {
            CLAZZS.removeAll(owner);
        }

        public static void destroy() {
            CLAZZS.destroy();
        }

        public static String dump() {
            return CLAZZS.dump();
        }
    }

}
