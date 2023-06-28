package org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers;

public class MainProviders {

    private final ClasspathProvider classpathProvider;
    private final ExecuteMethodProvider execute;
    private final SaveProvider save;
    private final UploadProvider uploadProvider;

    public MainProviders(
            ClasspathProvider classpathProvider, ExecuteMethodProvider execute, SaveProvider save, UploadProvider uploadProvider
    ) {
        this.classpathProvider = classpathProvider;
        this.execute = execute;
        this.save = save;
        this.uploadProvider = uploadProvider;
    }

    public ClasspathProvider getClasspathProvider() {
        return classpathProvider;
    }

    public ExecuteMethodProvider getExecute() {
        return execute;
    }

    public SaveProvider getSave() {
        return save;
    }

    public UploadProvider getUploadProvider() {
        return uploadProvider;
    }
}
