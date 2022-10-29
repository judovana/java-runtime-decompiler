package org.jrd.backend.data.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class InMemoryJar {
    private Manifest manifest = new Manifest();
    ByteArrayOutputStream bos;
    JarOutputStream target;

    public InMemoryJar() {
        startManifest();
    }

    public void startManifest() {
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        addToManifest("Can-Redefine-Classes", "true");
    }

    public void setMainClass(String mainFqn) {
        if (mainFqn != null && !"".equals(mainFqn)) {
            addToManifest(Attributes.Name.MAIN_CLASS.toString(), mainFqn);
        }
    }

    public void addToManifest(String key, String value) {
        manifest.getMainAttributes().put(new Attributes.Name(key), value);
    }

    public JarOutputStream openJar(OutputStream os) throws IOException {
        return new JarOutputStream(os, manifest);
    }

    private static void addFile(JarOutputStream target, byte[] body, String fqn) throws IOException {
        String name = fqn.replace('.', '/') + ".class";
        JarEntry entry = new JarEntry(name);
        entry.setTime(new Date().getTime());
        target.putNextEntry(entry);
        target.write(body);
        target.closeEntry();
    }

    public void addFile(byte[] body, String fqn) throws IOException {
        if (target == null) {
            open();
        }
        InMemoryJar.addFile(target, body, fqn);
    }

    public void open() throws IOException {
        bos = new ByteArrayOutputStream();
        target = this.openJar(bos);
    }

    public void close() throws IOException {
        if (target != null) {
            target.flush();
            target.close();
            target = null;
        }
    }

    public File save() throws IOException {
        File f = File.createTempFile("jrd", "test.zip");
        f.deleteOnExit();
        save(f);
        return f;
    }

    public void save(File f) throws IOException {
        if (target != null) {
            throw new RuntimeException("Not closed!");
        }
        Files.write(f.toPath(), bos.toByteArray());
    }

    public byte[] toBytes() throws IOException {
        if (target != null) {
            throw new RuntimeException("Not closed!");
        }
        return bos.toByteArray();
    }
}
