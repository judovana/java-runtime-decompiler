package org.jrd.backend.core;

import java.util.Objects;
import java.util.regex.Pattern;

public class ClassInfo {
    private String name;
    private String location;
    private String classLoader;
    private String module;
    private String moduleLoader;

    private static final String INFO_DELIMITER_STRING = "|";
    private static final Pattern INFO_DELIMITER_PATTERN = Pattern.compile("\\" + INFO_DELIMITER_STRING);

    public ClassInfo(String name, String location, String classLoader, String module, String moduleLoader) {
        this.name = name;
        this.location = location;
        this.classLoader = classLoader;
        this.module = module;
        this.moduleLoader = moduleLoader;
    }

    public ClassInfo(String classString) {
        String[] splitClassString = INFO_DELIMITER_PATTERN.split(classString);
        this.name = splitClassString[0];

        if (splitClassString.length >= 2 && !splitClassString[1].trim().isEmpty()) {
            this.location = splitClassString[1];
        } else { // backwards compatibility
            this.location = "unknown";
        }

        if (splitClassString.length >= 3 && !splitClassString[2].trim().isEmpty()) {
            this.classLoader = splitClassString[2];
        } else { // backwards compatibility
            this.classLoader = "unknown";
        }
        if (splitClassString.length >= 4 && !splitClassString[3].trim().isEmpty()) {
            this.module = splitClassString[3];
        } else { // backwards compatibility
            this.module = "unknown";
        }
        if (splitClassString.length >= 5 && !splitClassString[4].trim().isEmpty()) {
            this.moduleLoader = splitClassString[4];
        } else { // backwards compatibility
            this.moduleLoader = "unknown";
        }
    }

    public String toAgentLikeAnswer() {
        return name + INFO_DELIMITER_STRING + deNull(location) + INFO_DELIMITER_STRING + deNull(classLoader);
    }

    private String deNull(String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getClassLoader() {
        return classLoader;
    }

    public String getModule() {
        return module;
    }

    public String getModuleLoader() {
        return moduleLoader;
    }

    public String getSearchableString(boolean isLocationVisible) {
        return isLocationVisible ? (name + " " + location + " " + classLoader) : name;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Location: " + location + ", Classloader: " + classLoader;
    }

    // only the name is used to keep track of a given class between class loads, because the additional info changes
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClassInfo classInfo = (ClassInfo) o;
        return name.equals(classInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String toPrint(boolean details) {
        if (!details) {
            return name;
        } else {
            return name + "\n" + "  Location: " + location + "\n" + "  Class loader: " + classLoader;
        }
    }
}
