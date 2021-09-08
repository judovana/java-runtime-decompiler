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

    // env vars
    @Test
    void testCollapseEnvVars() {
        assertEquals("${HOME}", ExpandableUrl.collapseEnvVars("home_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${XDG_CONFIG_HOME}", ExpandableUrl.collapseEnvVars("config_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${JRD}", ExpandableUrl.collapseEnvVars("jrd_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${HOME}", ExpandableUrl.collapseEnvVars("${HOME}", "home_path", "config_path", "jrd_path"));
        assertEquals("different_path", ExpandableUrl.collapseEnvVars("different_path", "home_path", "config_path", "jrd_path"));
        assertEquals("/${HOME}", ExpandableUrl.collapseEnvVars("/home_path", "home_path", "config_path", "jrd_path"));
    }

    @Test
    void testExpandEnvVars() {
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home")), ExpandableUrl.expandEnvVars("${HOME}"));
        assertEquals(ExpandableUrl.unifySlashes(Directories.getXdgJrdBaseDir()), ExpandableUrl.expandEnvVars("${XDG_CONFIG_HOME}"));
        assertEquals(ExpandableUrl.unifySlashes(Directories.getJrdLocation()), ExpandableUrl.expandEnvVars("${JRD}"));
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
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromPathRealPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath(System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromPathMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    // createFromStringURL
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreateFromStringUrlWindowsRealPathOneLeadingSlash() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:/" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreateFromStringUrlWindowsRealPathThreeLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:///" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }


    @Test
    void testCreateFromStringUrlRealPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreateFromStringUrlLinuxRealPathTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://" + System.getProperty("user.home"));

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    void testCreateFromStringUrlMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }

    @Test
    void testCreateFromStringUrlLinuxMacroPathTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://${HOME}");

        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(ExpandableUrl.unifySlashes(System.getProperty("user.home"), false), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());

        assertDoesNotThrow(() -> Paths.get(expandableUrl.getExpandedPath()));
    }
}
