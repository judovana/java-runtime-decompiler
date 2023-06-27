package org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers;

import org.jrd.backend.completion.ClassesAndMethodsProvider;

public interface UploadProvider {

    ClasspathProvider getTarget();

    boolean isUploadEnabled();
    void resetUpload();


}
