package org.jrd.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class JarTest {

    public static class JarTool {
        private Manifest manifest = new Manifest();

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

        public static void addFile(JarOutputStream target, byte[] body, String fqn) throws IOException {
            String remaining = "";
            String name = fqn.replace('.', '/') + ".class";
            JarEntry entry = new JarEntry(name);
            entry.setTime(new Date().getTime());
            target.putNextEntry(entry);
            target.write(body);
            target.closeEntry();
        }
    }

    public static class Driver {
        public void main(String[] args) throws IOException {
            JarTool tool = new JarTool();
            tool.startManifest();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JarOutputStream target = tool.openJar(bos);

            JarTool.addFile(target, new byte[]{0, 1, 2}, "my.clazz");
            target.close();
            File f = File.createTempFile("jrd", "test.zip");
            f.deleteOnExit();
            Files.write(f.toPath(), bos.toByteArray());
            JarFile jf = new JarFile(f);
            boolean one = false;
            boolean second = false;
            int c = 0;
            for (Enumeration list = jf.entries(); list.hasMoreElements();) {
                c++;
                ZipEntry entry = (ZipEntry) list.nextElement();
                if (entry.getName().equals("my/clazz.class")) {
                    one = true;
                    Assertions.assertEquals(3, entry.getSize());
                }
                if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                    second = true;
                    Assertions.assertTrue(entry.getSize() > 10);
                }
            }
            jf.close();
            Assertions.assertTrue(one);
            Assertions.assertTrue(second);
            Assertions.assertEquals(2, c);
        }
    }

    @Test
    public void testJar() throws IOException {
        new Driver().main(null);
    }

}
