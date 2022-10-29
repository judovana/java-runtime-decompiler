package org.jrd.backend.data.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class InMemoryJarTest {

    @Test
    public void testJarFile() throws IOException {
        InMemoryJar jar = new InMemoryJar();
        jar.addFile(new byte[]{0, 1, 2}, "my.clazz");
        jar.close();
        File f = jar.save();
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

    @Test
    public void testJarStream() throws IOException {
        InMemoryJar jar = new InMemoryJar();
        jar.addFile(new byte[]{0, 1, 2}, "my.clazz");
        jar.close();
        JarInputStream jf = new JarInputStream(new ByteArrayInputStream(jar.toBytes()));
        boolean one = false;
        boolean second = false;
        int c = 0;
        JarEntry entry;
        while ((entry = jf.getNextJarEntry()) != null) {
            c++;
            if (entry.getName().equals("my/clazz.class")) {
                one = true;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (true) {
                    int qwe = jf.read();
                    if (qwe == -1) {
                        break;
                    }
                    baos.write(qwe);
                }
                Assertions.assertEquals(3, entry.getSize());
            }
            if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                second = true;
                Assertions.assertTrue(entry.getSize() > 10);
            }
        }
        jf.close();
        Assertions.assertTrue(one);
        Assertions.assertFalse(second);
        Assertions.assertEquals(1, c);
        //unable to read manifest:(
        //unable to verify if it is here...
        //obviously is, see file
    }
}
