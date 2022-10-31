package org.jrd.frontend.frame.main.popup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

class DiffPopupTest {

    @Test
    void getFilesFromPatchLng1() throws IOException {
        InputStream p1 = this.getClass().getResourceAsStream("/org/jrd/frontend/frame/main/popup/testPatch1");
        List<String> patch1 = Arrays.asList(new String(p1.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
        verifyIdeaHadNotDestroyedTestPatches(patch1);
        List<SingleFilePatch> r = DiffPopup.getIndividualPatches(patch1);
        Assertions.assertEquals(2, r.size());
        Assertions.assertEquals(12, r.get(0).getStart());
        Assertions.assertEquals(38, r.get(0).getEnd());
        Assertions.assertEquals(41, r.get(1).getStart());
        Assertions.assertEquals(51, r.get(1).getEnd());
    }

    @Test
    void getFilesFromPatchLng2() throws IOException {
        InputStream p2 = this.getClass().getResourceAsStream("/org/jrd/frontend/frame/main/popup/testPatch2");
        List<String> patch2 = Arrays.asList(new String(p2.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
        verifyIdeaHadNotDestroyedTestPatches(patch2);
        List<SingleFilePatch> r = DiffPopup.getIndividualPatches(patch2);
        Assertions.assertEquals(2, r.size());
        Assertions.assertEquals(2, r.get(0).getStart());
        Assertions.assertEquals(28, r.get(0).getEnd());
        Assertions.assertEquals(31, r.get(1).getStart());
        Assertions.assertEquals(41, r.get(1).getEnd());
    }

    private void verifyIdeaHadNotDestroyedTestPatches(List<String> patch1) {
        int emptySpaces = 0;
        for (String s : patch1) {
            if (" ".equals(s)) {
                emptySpaces++;
            }
        }
        if (emptySpaces <= 0) {
            throw new RuntimeException("Ide have remoes single sapces starting empty lines, and thus killed your patch!");
        }
    }

    @Test
    void getFilesFromPatch() {
        List<SingleFilePatch> r = DiffPopup
                .getIndividualPatches(Arrays.asList("+++ file1", "--- file2", " line1", " line2", "-line3", "+line4", " line5", " line6"));
        Assertions.assertEquals(1, r.size());
        Assertions.assertEquals(0, r.get(0).getStart());
        Assertions.assertEquals(7, r.get(0).getEnd());
    }

    @Test
    void parseClassFromHeader() {
        String s = DiffPopup.parseClassFromHeader("+++ file1");
        Assertions.assertEquals("file1", s);
        s = DiffPopup.parseClassFromHeader("+++ b/jenkins/job_dsl/tests/Constants.groovy");
        Assertions.assertEquals("Constants.groovy", s);
        s = DiffPopup.parseClassFromHeader("--- a/jenkins/job_dsl/tests/Constants.groovy");
        Assertions.assertEquals("Constants.groovy", s);
        s = DiffPopup.parseClassFromHeader("--- a/jenkins/job_dsl/tests/mandrel_linux_amd64_perfcheck_tests.groovy");
        Assertions.assertEquals("mandrel_linux_amd64_perfcheck_tests.groovy", s);
        s = DiffPopup.parseClassFromHeader("+++ b/jenkins/job_dsl/tests/mandrel_linux_amd64_perfcheck_tests.groovy");
        Assertions.assertEquals("mandrel_linux_amd64_perfcheck_tests.groovy", s);
        s = DiffPopup.parseClassFromHeader("--- Additional source/org.jrd.backend.data.Config");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
        s = DiffPopup.parseClassFromHeader("+++ Additional source buffer/org.jrd.backend.data.Config");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
        s = DiffPopup.parseClassFromHeader("--- Binary buffer/org.jrd.backend.data.Config.class");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
        s = DiffPopup.parseClassFromHeader("+++ Additional binary buffer/org.jrd.backend.data.Config");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
        s = DiffPopup.parseClassFromHeader("--- Source buffer/org.jrd.backend.data.Config.java");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
        s = DiffPopup.parseClassFromHeader("+++ Additional source buffer/org.jrd.backend.data.Config");
        Assertions.assertEquals("org.jrd.backend.data.Config", s);
    }

    @Test
    void parseDevNull() {
        boolean b;
        b = DiffPopup.isAddDevNull("+++ file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isAddDevNull("--- file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isAddDevNull("+++ /dev/null");
        Assertions.assertTrue(b);
        b = DiffPopup.isAddDevNull("--- /dev/null");
        Assertions.assertFalse(b);

        b = DiffPopup.isRemoveDevNull("+++ file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isRemoveDevNull("--- file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isRemoveDevNull("+++ /dev/null");
        Assertions.assertFalse(b);
        b = DiffPopup.isRemoveDevNull("--- /dev/null");
        Assertions.assertTrue(b);

        b = DiffPopup.isAddFile("+++ file1");
        Assertions.assertTrue(b);
        b = DiffPopup.isAddFile("--- file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isAddFile("+++ /dev/null");
        Assertions.assertFalse(b);
        b = DiffPopup.isAddFile("--- /dev/null");
        Assertions.assertFalse(b);

        b = DiffPopup.isRemoveFile("+++ file1");
        Assertions.assertFalse(b);
        b = DiffPopup.isRemoveFile("--- file1");
        Assertions.assertTrue(b);
        b = DiffPopup.isRemoveFile("+++ /dev/null");
        Assertions.assertFalse(b);
        b = DiffPopup.isRemoveFile("--- /dev/null");
        Assertions.assertFalse(b);
    }
}
