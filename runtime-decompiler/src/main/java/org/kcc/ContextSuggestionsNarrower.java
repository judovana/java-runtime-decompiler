package org.kcc;

public interface ContextSuggestionsNarrower {

    int getBeforeContextLinesCount();
    int getAfterContextLinesCount();
    CompletionItem[] narrowSuggestions(String currentKeyword, CompletionItem[] currentSet, String[] beforeLines, String[] afterLines, boolean caseSensitive);

}
