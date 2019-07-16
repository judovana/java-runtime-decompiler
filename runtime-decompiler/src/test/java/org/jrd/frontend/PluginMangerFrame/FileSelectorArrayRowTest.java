package org.jrd.frontend.PluginMangerFrame;

import org.jrd.backend.data.Directories;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.jrd.frontend.PluginMangerFrame.FileSelectorArrayRow.fallback;
import static org.junit.jupiter.api.Assertions.*;

class FileSelectorArrayRowTest {

    @Test
    void testFallback() {
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
}