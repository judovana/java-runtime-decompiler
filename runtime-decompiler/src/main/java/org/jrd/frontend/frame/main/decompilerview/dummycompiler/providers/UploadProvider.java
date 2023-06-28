package org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers;

public interface UploadProvider {

    ClasspathProvider getTarget();

    boolean isUploadEnabled();

    void resetUpload();

    boolean isBoot();

}
