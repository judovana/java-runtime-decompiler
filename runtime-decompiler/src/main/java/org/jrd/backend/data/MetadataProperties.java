package org.jrd.backend.data;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.core.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public final class MetadataProperties {
    private final Properties properties;

    private static final Pattern UNPROPAGATED_VALUE = Pattern.compile("\\$\\{.*}");
    private static final String PROPERTY_FILE_RESOURCE = "/metadata.prop";
    private static final String GROUP_ID_KEY = "groupId";
    private static final String VERSION_KEY = "version";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String NAME_KEY = "name";

    private static class PropertiesHolder {
        private static final MetadataProperties INSTANCE = new MetadataProperties();
    }

    private MetadataProperties() {
        properties = new FromFileProperties(UNPROPAGATED_VALUE);

        try (InputStream stream = getClass().getResourceAsStream(PROPERTY_FILE_RESOURCE)) {
            properties.load(stream);
        } catch (IOException e) {
            Logger.getLogger().log(Logger.Level.ALL, "Unable to read property file '" + PROPERTY_FILE_RESOURCE + "'.");
        }
    }

    public static MetadataProperties getInstance() {
        return PropertiesHolder.INSTANCE;
    }

    public String getGroup() {
        return properties.getProperty(GROUP_ID_KEY, "group.notfound");
    }

    public String getVersion() {
        return properties.getProperty(VERSION_KEY, "version.notfound");
    }

    public String getTimestamp() {
        return properties.getProperty(TIMESTAMP_KEY, "build.timestamp.notfound");
    }

    public String getName() {
        return properties.getProperty(NAME_KEY, "Runtime-Decompiler");
    }

    @Override
    public String toString() {
        return String.join(" - ", getGroup(), "JRD", getVersion(), getTimestamp());
    }

    /**
     * Properties that also use the default value passed to {@link Properties#getProperty(String, String) getProperty()}
     * if the property was found, but matched the regex passed at initialization.
     */
    @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Not necessary to override equals here.")
    private static class FromFileProperties extends Properties {
        private final Pattern unpropagatedValuePattern;

        FromFileProperties(Pattern pattern) {
            this.unpropagatedValuePattern = pattern;
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            String value = super.getProperty(key, defaultValue);

            if (unpropagatedValuePattern.matcher(value).matches()) {
                return defaultValue;
            }

            return value;
        }
    }
}
