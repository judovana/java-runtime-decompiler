package org.jrd.backend.data;

import org.jrd.backend.core.OutputController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MetadataProperties {
    private final Properties properties;

    private static final String PROPERTY_FILE_RESOURCE = "/metadata.prop";
    private static final String GROUP_ID_KEY = "groupId";
    private static final String VERSION_KEY = "version";
    private static final String TIMESTAMP_KEY = "timestamp";

    private static class PropertiesHolder {
        private static final MetadataProperties INSTANCE = new MetadataProperties();
    }

    private MetadataProperties() {
        properties = new java.util.Properties();

        try (InputStream stream = getClass().getResourceAsStream(PROPERTY_FILE_RESOURCE)) {
            properties.load(stream);
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, "Unable to read property file '" + PROPERTY_FILE_RESOURCE + "'.");
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

    @Override
    public String toString() {
        return String.join(" - ", getGroup(), "JRD", getVersion(), getTimestamp());
    }
}
