package org.jrd.backend.data;

import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.frontend.frame.main.LoadingDialogProvider;
import org.jrd.frontend.frame.main.ModelProvider;

import java.util.Base64;
import java.util.Collection;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

public class DependenciesReader {

    private final ModelProvider provider;
    private final LoadingDialogProvider gui;

    public DependenciesReader(ModelProvider model, LoadingDialogProvider gui) {
        this.provider = model;
        this.gui = gui;
    }

    public Collection<String> resolve(String clazz, String base64body) {
        byte[] bbytes = Base64.getDecoder().decode(base64body);
        Collection<String> deps = io.github.mkoncek.classpathless.util.BytecodeExtractor
                .extractDependencies(new IdentifiedBytecode(new ClassIdentifier(clazz), bbytes), getClassesProvider());
        return deps;
    }

    public VmInfo getVmInfo() {
        return provider.getVmInfo();
    }

    public VmManager getVmManager() {
        return provider.getVmManager();
    }

    public RuntimeCompilerConnector.JrdClassesProvider getClassesProvider() {
        return provider.getClassesProvider();
    }

    public LoadingDialogProvider getGui() {
        return gui;
    }
}
