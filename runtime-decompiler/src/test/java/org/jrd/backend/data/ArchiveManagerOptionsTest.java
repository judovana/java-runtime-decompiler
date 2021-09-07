package org.jrd.backend.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ArchiveManagerOptionsTest {

    @Test
    void isInnerDefault() {
        ArchiveManagerOptions a1 = new ArchiveManagerOptions();
        Assertions.assertTrue(a1.isInner("some/file/filer.zip"));
        Assertions.assertTrue(a1.isInner("filer.WAR"));
    }

    @Test
    void isNotInnerDefault() {
        ArchiveManagerOptions a1 = new ArchiveManagerOptions();
        Assertions.assertFalse(a1.isInner("some/file/filerzip"));
        Assertions.assertFalse(a1.isInner("filerWAR"));
    }

    @Test
    void isInnerCustom() {
        ArchiveManagerOptions a1 = new ArchiveManagerOptions();
        a1.setExtension(List.of("ZIP", "WAR"));
        Assertions.assertTrue(a1.isInner("some/file/filer.zip"));
        Assertions.assertTrue(a1.isInner("filer.WAR"));
    }

    @Test
    void isNotInnerCustom() {
        ArchiveManagerOptions a1 = new ArchiveManagerOptions();
        a1.setExtension(List.of("Xzip", "xWAR"));
        Assertions.assertFalse(a1.isInner("some/file/filerzip"));
        Assertions.assertFalse(a1.isInner("filerWAR"));
    }
}
