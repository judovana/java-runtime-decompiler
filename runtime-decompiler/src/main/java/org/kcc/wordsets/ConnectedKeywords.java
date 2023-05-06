package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ConnectedKeywords implements CompletionItem.CompletionItemSet {
    private final CompletionItem.CompletionItemSet[] originalSets;

    public ConnectedKeywords(CompletionItem.CompletionItemSet... originalSets) {
        this.originalSets = originalSets;
    }

    @Override
    public CompletionItem[] getItemsArray() {
        return getItemsList().toArray(new CompletionItem[0]);
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = new ArrayList<>();
        for (CompletionItem.CompletionItemSet item : originalSets) {
            l.addAll(item.getItemsList());
        }
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < originalSets.length; i++) {
            CompletionItem.CompletionItemSet item = originalSets[i];
            sb.append("(").append(item.getRecommendedDelimiterSet().toString()).append(")");
            if (i < originalSets.length - 1) {
                sb.append("|");
            }
        }
        return Pattern.compile(sb.toString());
    }
}
