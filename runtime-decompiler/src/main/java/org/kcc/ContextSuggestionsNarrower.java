package org.kcc;

public interface ContextSuggestionsNarrower {

    /*
     * How many lines before current position it should provide. Note, that last line is always the incomplete
     * current one
     */
    int getBeforeContextLinesCount();

    /*
     * How many lines after current position it should provide. Note, that first line is always the incomplete
     * current one
     */
    int getAfterContextLinesCount();

    /*
     * before lines + word + after lines should be complete surrounding
     * More precisly:
     *   beforeLines[0]   + "\n" +
     *   beforeLines[1]   + "\n" + ... +
     *   beforeLines[n-1] + "\n" +
     *   beforeLines[n]   + currentKeyword + afterLines[0] + "\n"
     *   afterLines[1]    + "\n" + ... +
     *   afterLines[n-1]  + "\n"
     *   afterLines[n]    + "\n"
     *   == whole surrounding
     */
    CompletionItem[] narrowSuggestions(String currentKeyword, CompletionItem[] currentSet, String[] beforeLines,
                                       String[] afterLines, boolean caseSensitive);

    static class DebugNarrower implements ContextSuggestionsNarrower {
        private final int before;
        private final int after;

        public DebugNarrower(int befor, int after) {
            this.before = befor;
            this.after = after;
        }

        @Override
        public int getBeforeContextLinesCount() {
            return before;
        }

        @Override
        public int getAfterContextLinesCount() {
            return after;
        }

        @Override
        public CompletionItem[] narrowSuggestions(String currentKeyword, CompletionItem[] currentSet,
                                                  String[] beforeLines, String[] afterLines, boolean caseSensitive) {
            System.err.println("###########");
            for (int x = 0; x < beforeLines.length; x++) {
                System.err.println(beforeLines[x]);
            }
            System.err.println("!" + currentKeyword + "!");
            for (int x = 0; x < afterLines.length; x++) {
                System.err.println(afterLines[x]);
            }
            System.err.println("***********");
            return  currentSet;
        }
    }

}
