package org.kcc;

public class CompletionSettings {

    private final CompletionItem.CompletionItemSet set;

    public CompletionSettings(CompletionItem.CompletionItemSet set) {
        this.set = set;
    }

    public CompletionItem.CompletionItemSet getSet() {
        return set;
    }
}
