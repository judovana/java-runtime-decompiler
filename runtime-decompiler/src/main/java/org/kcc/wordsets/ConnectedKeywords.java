package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            l.addAll(
                    item.getItemsList().stream()
                            .map(a -> new CompletionItem(a.getKey(), a.getDescription() + "\n from: " + item.toString(), a.getRealReplacement()))
                            .collect(Collectors.toList())
            );
        }
        ArrayList<CompletionItem> r = new ArrayList<>(new HashSet<>(l));
        Collections.sort(r);
        return r;
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

    @Override
    public String toString() {
        return Arrays.stream(originalSets).map(a -> a.toString()).collect(Collectors.joining("; "));
    }

    public CompletionItem.CompletionItemSet[] getOriginalSets() {
        return Arrays.copyOf(originalSets, originalSets.length);
    }
}
