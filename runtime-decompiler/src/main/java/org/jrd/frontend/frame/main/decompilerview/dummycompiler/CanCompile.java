package org.jrd.frontend.frame.main.decompilerview.dummycompiler;

import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;

import java.util.Collection;

public interface CanCompile {

    Collection<IdentifiedBytecode> compile(String s, PluginManager pluginManager);

    DecompilerWrapper getWrapper();
}
