package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class NullClassesProvider implements ClassesProvider {
    @Override
    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... classIdentifiers) {
        return new ArrayList<>(0);
    }

    @Override
    public List<String> getClassPathListing() {
        return new ArrayList<String>(0);
    }
}
