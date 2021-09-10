package org.jrd.backend.decompiling;

import org.jrd.backend.data.Directories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ExpandableUrlTest {
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsOsWindowsOnWindows() {
        assertTrue(Directories.isOsWindows());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testIsOsWindowsOnLinux() {
        assertFalse(Directories.isOsWindows());
    }

    private String collapseWithTestPaths(String pathToCollapse) {
        return ExpandableUrl.collapseEnvVars(pathToCollapse, "home_path", "config_path", "jrd_path");
    }

    // env vars
    @Test
    void testCollapseEnvVars() {
        assertEquals("${HOME}", collapseWithTestPaths("home_path"));
        assertEquals("${XDG_CONFIG_HOME}", collapseWithTestPaths("config_path"));
        assertEquals("${JRD}", collapseWithTestPaths("jrd_path"));
        assertEquals("${HOME}", collapseWithTestPaths("${HOME}"));
        assertEquals("different_path", collapseWithTestPaths("different_path"));
        assertEquals("/${HOME}", collapseWithTestPaths("/home_path"));
    }

    @Test
    void testExpandEnvVars() {
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home")),
                ExpandableUrl.expandEnvVars("${HOME}")
        );

        assertEquals(
                ExpandableUrl.unifySlashes(Directories.getXdgJrdBaseDir()),
                ExpandableUrl.expandEnvVars("${XDG_CONFIG_HOME}")
        );

        assertEquals(
                ExpandableUrl.unifySlashes(Directories.getJrdLocation()),
                ExpandableUrl.expandEnvVars("${JRD}")
        );
    }

    // slashes
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testUnifySlashesOnWindows() {
        assertEquals("/path", ExpandableUrl.unifySlashes("path"));
        assertEquals("/path", ExpandableUrl.unifySlashes("/path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("longer/path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("longer\\path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("/longer/path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("/longer\\path"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testUnifySlashesOnLinux() {
        assertEquals("path", ExpandableUrl.unifySlashes("path"));
        assertEquals("/path", ExpandableUrl.unifySlashes("/path"));
        assertEquals("longer/path", ExpandableUrl.unifySlashes("longer/path"));
        assertEquals("longer/path", ExpandableUrl.unifySlashes("longer\\path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("/longer/path"));
        assertEquals("/longer/path", ExpandableUrl.unifySlashes("/longer\\path"));
    }

    // createFromPath

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreateFromPathWindowsActualPathLeadingSlash() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("/" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromPathRealPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath(System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromPathMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawUrl()
        );
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    // createFromStringURL
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreateFromStringUrlWindowsRealPathOneLeadingSlash() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:/" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreateFromStringUrlWindowsRealPathThreeLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:///" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }


    @Test
    void testCreateFromStringUrlRealPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreateFromStringUrlLinuxRealPathTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());
    }

    @Test
    void testCreateFromStringUrlMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromStringUrlLinuxMacroPathTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(
                ExpandableUrl.unifySlashes(System.getProperty("user.home"), false),
                expandableUrl.getExpandedPath()
        );
        assertEquals("file:${HOME}", expandableUrl.getRawUrl());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedUrl());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }
}
