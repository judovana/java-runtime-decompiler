package org.jrd.frontend.frame.plugins;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.data.Directories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.Paths;

@SuppressFBWarnings(
        value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
        justification = "Hardcoded paths aren't used for manipulating with an actual filesystem."
)
class FileSelectorArrayRowTest {
    @BeforeAll
    static void setup() {
        Directories.createPluginDirectory();
    }

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

        Assertions.assertEquals(expectedNormal, FileSelectorArrayRow.fallback(inputNormal));
        Assertions.assertEquals(expectedNormal2, FileSelectorArrayRow.fallback(inputNormal2));
        Assertions.assertEquals(expectedNoParent, FileSelectorArrayRow.fallback(inputNoParent));
        Assertions.assertEquals(expectedNoParentOver, FileSelectorArrayRow.fallback(inputNoParentOver));
        Assertions.assertEquals(expectedNonExistent, FileSelectorArrayRow.fallback(inputNonExistent));
        Assertions.assertEquals(expectedNonExistent2, FileSelectorArrayRow.fallback(inputNonExistent2));
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

        Assertions.assertEquals(expectedNormal, FileSelectorArrayRow.fallback(inputNormal));
        Assertions.assertEquals(expectedNormal2, FileSelectorArrayRow.fallback(inputNormal2));
        Assertions.assertEquals(expectedNoParent, FileSelectorArrayRow.fallback(inputNoParent));
        Assertions.assertEquals(expectedNoParentOver, FileSelectorArrayRow.fallback(inputNoParentOver));
        Assertions.assertEquals(expectedNonExistent, FileSelectorArrayRow.fallback(inputNonExistent));
        Assertions.assertEquals(expectedNonExistent2, FileSelectorArrayRow.fallback(inputNonExistent2));
        Assertions.assertEquals(expectedCwd, FileSelectorArrayRow.fallback(inputCwd));
    }
}
