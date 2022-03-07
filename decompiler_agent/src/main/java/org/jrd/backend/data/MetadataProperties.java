/**
 * THIS IS COPYPASTED FROM runtime-decompiler
 * There have to never be any differences!!
 * Thisfiel msut be identical as the one from  runtime-decompiler except:
 *  - this headr
 *  - logger replaced by printStackTrace
 *
 *  Any development shoudl happen in runtime-decompiler and then copy here.
 *  This serves ony one purpose - this returns toString to runtime-decompiler, and is comapred with its toString. They should match
 */

package org.jrd.backend.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class MetadataProperties {
    private final Properties properties;

    private static final Pattern UNPROPAGATED_VALUE = Pattern.compile("\\$\\{.*}");
    private static final String PROPERTY_FILE_RESOURCE = "/org/jrd/backend/data/metadata.prop";
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
            e.printStackTrace();
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
