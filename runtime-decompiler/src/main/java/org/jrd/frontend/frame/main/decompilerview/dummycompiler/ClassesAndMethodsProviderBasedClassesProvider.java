package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.GlobalConsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class ClassesAndMethodsProviderBasedClassesProvider implements ClassesProvider {
    private final ClassesAndMethodsProvider classesAndMethodsProvider;

    public ClassesAndMethodsProviderBasedClassesProvider(ClassesAndMethodsProvider classesAndMethodsProvider) {
        this.classesAndMethodsProvider = classesAndMethodsProvider;
    }

    @Override
    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... classIdentifiers) {
        List<IdentifiedBytecode> result = new ArrayList<>(classIdentifiers.length);
        for (ClassIdentifier ci : classIdentifiers) {
            try {
                byte[] b = classesAndMethodsProvider.getClassItself(null, classIdentifiers[0].getFullName());
                result.add(new IdentifiedBytecode(ci, b));
            }catch (Exception ex) {
                Logger.getLogger().log(ex);
            }
        }
        return result;
    }

    @Override
    public List<String> getClassPathListing() {
        return Arrays.stream(classesAndMethodsProvider.getClasses(null)).collect(Collectors.toList());
    }
}
