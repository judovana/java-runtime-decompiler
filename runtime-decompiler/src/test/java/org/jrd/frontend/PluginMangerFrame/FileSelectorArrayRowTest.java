package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.data.Directories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.Paths;

import static org.jrd.frontend.PluginMangerFrame.FileSelectorArrayRow.fallback;
import static org.junit.jupiter.api.Assertions.*;

class FileSelectorArrayRowTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testFallback() { //Windows
        final File inputNormal = new File(Directories.getPluginDirectory());
        final File inputNormal2 = new File("C:\\Windows");
        final File inputNoParent = new File("C:\\");
        final File inputNoParentOver = new File("C:\\Usrs");
        final File inputNonExistent = new File(Directories.getXdgJrdBaseDir() + File.separator + "plugans");
        final File inputNonExistent2 = new File("Z:\\");

        final File expectedNormal = new File(Directories.getPluginDirectory());
        final File expectedNormal2 = new File("C:\\Windows");
        final File expectedNoParent = new File("C:\\");
        final File expectedNoParentOver = new File("C:\\");
        final File expectedNonExistent = new File(Directories.getXdgJrdBaseDir());
        final File expectedNonExistent2 = new File("Z:\\");

        assertEquals(expectedNormal, fallback(inputNormal));
        assertEquals(expectedNormal2, fallback(inputNormal2));
        assertEquals(expectedNoParent, fallback(inputNoParent));
        assertEquals(expectedNoParentOver, fallback(inputNoParentOver));
        assertEquals(expectedNonExistent, fallback(inputNonExistent));
        assertEquals(expectedNonExistent2, fallback(inputNonExistent2));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testFallbackLinux() {
        final File inputNormal = new File(Directories.getPluginDirectory());
        final File inputNormal2 = new File("/sys/kernel");
        final File inputNoParent = new File("/");
        final File inputNoParentOver = new File("/sys");
        final File inputNonExistent = new File(Directories.getXdgJrdBaseDir() + File.separator + "plugans");
        final File inputNonExistent2 = Paths.get("./complete/nonsense").normalize().toAbsolutePath().toFile();
        final File inputCwd = Paths.get(".").normalize().toAbsolutePath().toFile();

        final File expectedNormal = new File(Directories.getPluginDirectory());
        final File expectedNormal2 = new File("/sys/kernel");
        final File expectedNoParent = new File("/");
        final File expectedNoParentOver = new File("/sys");
        final File expectedNonExistent = new File(Directories.getXdgJrdBaseDir());
        final File expectedNonExistent2 = Paths.get(".").normalize().toAbsolutePath().toFile();
        final File expectedCwd = new File(System.getProperty("user.dir"));

        assertEquals(expectedNormal, fallback(inputNormal));
        assertEquals(expectedNormal2, fallback(inputNormal2));
        assertEquals(expectedNoParent, fallback(inputNoParent));
        assertEquals(expectedNoParentOver, fallback(inputNoParentOver));
        assertEquals(expectedNonExistent, fallback(inputNonExistent));
        assertEquals(expectedNonExistent2, fallback(inputNonExistent2));
        assertEquals(expectedCwd, fallback(inputCwd));
    }
}