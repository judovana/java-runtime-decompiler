package org.kcc;

import java.util.List;
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

    @Override
    public int compareTo(CompletionItem o) {
        if (o == null) {
            return 1;
        }
        return -o.getKey().compareTo(this.getKey());
    }
}
