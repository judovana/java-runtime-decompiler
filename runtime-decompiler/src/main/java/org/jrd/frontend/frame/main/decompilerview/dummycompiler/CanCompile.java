package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public interface CanCompile {

    Collection<IdentifiedBytecode> compile(String s, PluginManager pluginManager, String execute);

    DecompilerWrapper getWrapper();

    @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "safety is not an goal here")
    static void run(String fqn, Collection<IdentifiedBytecode> result, String execute, ClassesProvider classesProvider)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        // without parent, all classes form rt.jar/jdk's modules have to be find there.
        // loadClass are there just for debugging
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public Class findClass(String name) throws ClassNotFoundException {
                for (IdentifiedBytecode ib : result) {
                    if (ib.getClassIdentifier().getFullName().equals(name)) {
                        if (ib.getFile().length == 0) {
                            throw new ClassNotFoundException(name);
                        }
                        return defineClass(name, ib.getFile(), 0, ib.getFile().length);
                    }
                }
                Collection<IdentifiedBytecode> classloaderSpecificClasses = classesProvider.getClass(new ClassIdentifier(name));
                if (classloaderSpecificClasses.size() == 1) {
                    IdentifiedBytecode clazz = new ArrayList<>(classloaderSpecificClasses).get(0);
                    if (clazz.getFile().length == 0) {
                        throw new ClassNotFoundException(name);
                    }
                    return defineClass(clazz.getClassIdentifier().getFullName(), clazz.getFile(), 0, clazz.getFile().length);
                }
                throw new ClassNotFoundException(name);
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
            methodnameAndSignature = "start"; //fallback to main?
        }
        Method test = clz.getMethod(methodnameAndSignature);
        //test.invoke(main);
        test.invoke(null);
    }
}
