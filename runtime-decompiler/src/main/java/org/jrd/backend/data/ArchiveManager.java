package org.jrd.backend.data;

import org.jrd.backend.communication.FsAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveManager {
    File c;
    String tmpdir = System.getProperty("java.ioo.tmpdir");
    ArrayList<String> currentJars = new ArrayList<>();

    public ArchiveManager(File c) {
        this.c = c;
    }

    public boolean isClassInFile(String clazz) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(c));
        if (!currentJars.isEmpty()) {
            currentJars = new ArrayList<>();
        }
        currentJars.add(c.getName());
        var ret = findClazz(zis, clazz);
        if (!ret) {
            currentJars.remove(c.getName());
        }
        return ret;
    }

    private boolean findClazz(ZipInputStream zis, String clazz) throws IOException {
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip")) {
                    currentJars.add(entry.getName());
                    if(findClazz(new ZipInputStream(zis), clazz)) {
                        return true;
                    }
                    currentJars.remove(entry.getName());
                } else {
                    String clazzInJar = FsAgent.toClass(entry.getName());
                    if (clazzInJar.equals(clazz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean needExtract() {
        return !(currentJars.size() == 1);
    }
}
