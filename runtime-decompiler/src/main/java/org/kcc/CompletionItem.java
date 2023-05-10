package org.kcc;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CompletionItem implements Comparable<CompletionItem> {

    public interface CompletionItemSet {

        CompletionItem[] getItemsArray();

        List<CompletionItem> getItemsList();

        Pattern getRecommendedDelimiterSet();

        static Pattern delimiterSet() {
            return Pattern.compile("[a-zA-Z0-9_\\-\\.\\(\\)]");
        }

        static Pattern delimiterStrictSet() {
            return Pattern.compile("[a-zA-Z0-9_]");
        }

        static Pattern delimiterWordSet() {
            return Pattern.compile("[a-zA-Z]");
        }

        static Pattern delimiterWordAndNumberSet() {
            return Pattern.compile("[a-zA-Z0-9]");
        }
    }

    private final String key;
    private final String description;
    private final String realReplacement;

    public CompletionItem(String key) {
        this(key, "", "");
    }

    public CompletionItem(String key, String description) {
        this(key, description, "");
    }

    public CompletionItem(String key, String description, String realReplacement) {
        this.key = key;
        this.description = description;
        this.realReplacement = realReplacement;
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

    public String getRealReplacement() {
        if (realReplacement.isEmpty()) {
            return key;
        } else {
            return realReplacement;
        }
    }

    @Override
    public int compareTo(CompletionItem o) {
        if (o == null) {
            return 1;
        }
        return this.getKey().compareTo(o.getKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompletionItem that = (CompletionItem) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
