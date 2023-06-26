package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

public interface CanCompile {

    static void save(Collection<IdentifiedBytecode> result, File save) throws IOException {
        for(IdentifiedBytecode ib: result){
            Files.write(new File(save, ib.getClassIdentifier().getFullName()+".class").toPath(), ib.getFile());
        }
    }

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
        //we run on static methods only now, othwrwise there woudl be messing with constructor
        //Object main = clz.newInstance();
        String[] methodnameAndSignature = execute.trim().split("\\s+");
        String methodName = methodnameAndSignature[0];
        Class[] signature = new Class[methodnameAndSignature.length - 1];
        Object[] values = new Object[methodnameAndSignature.length - 1];
        for (int x = 1; x < methodnameAndSignature.length; x++) {
            signature[x - 1] = Class.forName(methodnameAndSignature[x]);
            values[x - 1] = null;
        }
        Method test = clz.getMethod(methodName, signature);
        //test = clz.getMethod("main", new Class[]{Class.forName("[Ljava.lang.String;")});
        //test.invoke(null, new Object[]{null});
        test.invoke(null, values);
    }
}
