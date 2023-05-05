package org.kcc;

public class CompletionItem {

    private final String key;
    private final String description;

    public CompletionItem(String key) {
        this(key, "");
    }
    public CompletionItem(String key, String description) {
        this.key = key;
        this.description = description;
    }

    @Override
    public String toString() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }
}
