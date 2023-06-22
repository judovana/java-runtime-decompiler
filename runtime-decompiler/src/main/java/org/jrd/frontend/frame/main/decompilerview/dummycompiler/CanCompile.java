package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

public interface CanCompile {

    Collection<IdentifiedBytecode> compile(String s, PluginManager pluginManager, String execute);

    DecompilerWrapper getWrapper();

    static void run(String fqn, Collection<IdentifiedBytecode> result, String execute)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public Class findClass(String name) throws ClassNotFoundException {
                for (IdentifiedBytecode ib : result) {
                    if (ib.getClassIdentifier().getFullName().equals(name)) {
                        return defineClass(name, ib.getFile(), 0, ib.getFile().length);
                    }
                }
                return loadClass(name);
            }

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return super.loadClass(name);
            }
            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                return super.loadClass(name, resolve);
            }

        };
        Class<?> clz = classLoader.loadClass(fqn);
        //we run on static methods oly now, othwrwise there woudl be messing with constructor
        //Object main = clz.newInstance();
        String methodnameAndSignature = execute.trim();
        if (methodnameAndSignature.isEmpty()) {
            methodnameAndSignature = "start";
        }
        Method test = clz.getMethod(methodnameAndSignature);
        //test.invoke(main);
        test.invoke(null);
    }
}
