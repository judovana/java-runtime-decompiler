package org.jrd.backend.data.cli;

import java.io.File;

public class FqnAndClassToJar {
    private final String fqn;
    private final File file;

    public FqnAndClassToJar(String fqn, File file) {
        this.fqn = fqn;
        this.file = file;
    }

    public String getFqn() {
        return fqn;
    }

    public File getFile() {
        return file;
    }
}
