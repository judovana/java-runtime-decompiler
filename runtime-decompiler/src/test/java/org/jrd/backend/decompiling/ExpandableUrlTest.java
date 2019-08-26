package org.jrd.backend.decompiling;

import org.jrd.backend.data.Directories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.net.MalformedURLException;
import java.net.URL;

import static org.jrd.backend.data.Directories.getJrdLocation;
import static org.jrd.backend.decompiling.ExpandableUrl.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpandableUrlTest {
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsOsWindowsOnWindows() {
        assertTrue(isOsWindows());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testIsOsWindowsOnLinux() {
        assertFalse(isOsWindows());
    }

    @Test
    void testCollapseEnvVars() {
        assertEquals("${HOME}", collapseEnvVars("home_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${XDG_CONFIG_HOME}", collapseEnvVars("config_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${JRD}", collapseEnvVars("jrd_path", "home_path", "config_path", "jrd_path"));
        assertEquals("${HOME}", collapseEnvVars("${HOME}", "home_path", "config_path", "jrd_path"));
        assertEquals("different_path", collapseEnvVars("different_path", "home_path", "config_path", "jrd_path"));
        assertEquals("/${HOME}", collapseEnvVars("/home_path", "home_path", "config_path", "jrd_path"));
    }

    @Test
    void testExpandEnvVars() {
        assertEquals(unifySlashes(System.getProperty("user.home")), expandEnvVars("${HOME}"));
        assertEquals(unifySlashes(Directories.getXdgJrdBaseDir()), expandEnvVars("${XDG_CONFIG_HOME}"));
        assertEquals(unifySlashes(getJrdLocation()), expandEnvVars("${JRD}"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testUnifySlashesOnWindows(){
        assertEquals("/path", unifySlashes("path"));
        assertEquals("/path", unifySlashes("/path"));
        assertEquals("/longer/path", unifySlashes("longer/path"));
        assertEquals("/longer/path", unifySlashes("longer\\path"));
        assertEquals("/longer/path", unifySlashes("/longer/path"));
        assertEquals("/longer/path", unifySlashes("/longer\\path"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testUnifySlashesOnLinux(){
        assertEquals("path", unifySlashes("path"));
        assertEquals("/path", unifySlashes("/path"));
        assertEquals("longer/path", unifySlashes("longer/path"));
        assertEquals("longer/path", unifySlashes("longer\\path"));
        assertEquals("/longer/path", unifySlashes("/longer/path"));
        assertEquals("/longer/path", unifySlashes("/longer\\path"));
    }


    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromPathOnWindowsFromRealPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath(System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromPathOnWindowsFromRealPathWithOneLeadingSlash() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("/" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromPathOnWindowsFromMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }


    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreateFromPathOnLinuxFromActualPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath(System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreateFromPathOnLinuxFromMacroPath() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromPath("${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }


    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromStringUrlOnWindowsFromRealPathWithNoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromStringUrlOnWindowsFromRealPathWithOneLeadingSlash() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:/" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromStringUrlOnWindowsFromRealPathWithThreeLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:///" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromStringUrlOnWindowsFromMacroPathWithNoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCreationFromStringUrlOnWindowsFromMacroPathWithTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(unifySlashes(System.getProperty("user.home")), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:/" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }


    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreationFromStringUrlOnLinuxFromRealPathWithNoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreationFromStringUrlOnLinuxFromRealPathWithTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://" + System.getProperty("user.home"));
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreationFromStringUrlOnLinuxFromMacroPathWithNoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file:${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testCreationFromStringUrlOnLinuxFromMacroPathWithTwoLeadingSlashes() throws MalformedURLException {
        ExpandableUrl expandableUrl = ExpandableUrl.createFromStringUrl("file://${HOME}");
        assertEquals("${HOME}", expandableUrl.getRawPath());
        assertEquals(System.getProperty("user.home"), expandableUrl.getExpandedPath());
        assertEquals("file:${HOME}", expandableUrl.getRawURL());
        assertEquals(new URL("file:" + System.getProperty("user.home")), expandableUrl.getExpandedURL());
    }
}