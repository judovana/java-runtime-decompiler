package org.jrd.backend.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArchiveManagerOptions {

    private static class ArchiveManagerOptionsHolder {
        private static final ArchiveManagerOptions INSTANCE = new ArchiveManagerOptions();
    }

    public static ArchiveManagerOptions getInstance() {
        return ArchiveManagerOptionsHolder.INSTANCE;
    }

    private List<String> extensions = new ArrayList<>();
    private static final List<String> DEFAULTS = List.of(".zip", ".jar", ".war", ".ear");

    public void setExtension(List<String> s) {
        extensions = Collections.unmodifiableList(s);
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    public boolean isInner(String n) {
        String name = n.toLowerCase();

        if (areExtensionsEmpty()) {
            return oneEnds(DEFAULTS, name);
        } else {
            return oneEnds(extensions, name);
        }
    }

    private boolean areExtensionsEmpty() {
        return extensions == null || extensions.isEmpty() ||
                extensions.size() == 1 && extensions.get(0).trim().isEmpty();
    }

    private boolean oneEnds(List<String> suffixes, String name) {
        for (String suffix : suffixes) {
            if (name.endsWith(suffix.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
