package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BytemanKeywords  implements  CompletionItem.CompletionItemSet{
    private static final CompletionItem[] BYTEMAN_KEYWORDS = {
            new CompletionItem("RULE"),
            new CompletionItem("CLASS"),
            new CompletionItem("METHOD"),
            new CompletionItem("BIND"),
            new CompletionItem("IF"),
            new CompletionItem("DO"),
            new CompletionItem("ENDRULE"),
            new CompletionItem("AT"),
            new CompletionItem("EXCEPTION"),
            new CompletionItem("EXIT"),
            new CompletionItem("ENTRY"),
            new CompletionItem("INVOKE"),
            new CompletionItem("IF"),
            new CompletionItem("AFTER"),
            new CompletionItem("WRITE"),
            new CompletionItem("READ"),
            new CompletionItem("ALL"),
            new CompletionItem("SYNCHRONIZE"),
            new CompletionItem("SYNCHRONIZE"),
            new CompletionItem("traceln"),
            new CompletionItem("traceStack"),
            new CompletionItem("flag")
    };

    @Override
    public CompletionItem[] getItemsArray() {
        CompletionItem[] r = Arrays.copyOf(BYTEMAN_KEYWORDS, BYTEMAN_KEYWORDS.length);
        Arrays.sort(r);
        return r;
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = Arrays.asList(BYTEMAN_KEYWORDS);
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        return CompletionItem.CompletionItemSet.delimiterWordSet();
    }

    @Override
    public String toString() {
        return "Byteman keywords - good to concat with java";
    }
}
