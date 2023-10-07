package org.kcc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.completion.ClassesAndMethodsProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("NestedIfDepth") // un-refactorable
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
    CompletionItem[] narrowSuggestions(
            String currentKeyword, CompletionItem[] currentSet, String[] beforeLines, String[] afterLines, CompletionSettings settings
    );

    class DebugNarrower implements ContextSuggestionsNarrower {
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
        public CompletionItem[] narrowSuggestions(
                String currentKeyword, CompletionItem[] currentSet, String[] beforeLines, String[] afterLines, CompletionSettings settings
        ) {
            System.err.println("###########");
            for (int x = 0; x < beforeLines.length - 1; x++) {
                System.err.println(beforeLines[x]);
            }
            System.err.print(beforeLines[beforeLines.length - 1]);
            System.err.print("!" + currentKeyword + "!");
            System.err.println(afterLines[0]);
            for (int x = 1; x < afterLines.length - 0; x++) {
                System.err.println(afterLines[x]);
            }
            System.err.println("***********");
            return currentSet;
        }
    }

    class ClassesAndMethodsEnforcingNarrower implements ContextSuggestionsNarrower {

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "unimportant")
        private static final CompletionItem[] COMPLETION_ITEMS_HELP = {
                new CompletionItem("you can select any running, remote ot FS vm to provide class/methods sugestions"),
                new CompletionItem("You can fill additional CP in settings to provide standalone global classes/methods suggestions")};

        private final ClassesAndMethodsProvider provider;

        public ClassesAndMethodsEnforcingNarrower(ClassesAndMethodsProvider classesAndMethodsProvider) {
            this.provider = classesAndMethodsProvider;
        }

        @Override
        public int getBeforeContextLinesCount() {
            return 5;
        }

        @Override
        public int getAfterContextLinesCount() {
            return 0;
        }

        public CompletionItem[] narrowSuggestions(
                String currentKeyword, CompletionItem[] currentSet, String[] beforeLines, String[] afterLines, CompletionSettings settings
        ) {
            if (provider != null && beforeLines.length > 0) {
                String currentTrimedLine = beforeLines[beforeLines.length - 1].trim();
                String[] currentLineTokens = currentTrimedLine.split("\\s+");
                if (currentLineTokens[currentLineTokens.length - 1].equals("CLASS")) {
                    if (provider.isMissingVmInfo()) {
                        return COMPLETION_ITEMS_HELP;
                    }
                    return Arrays.stream(provider.getClasses(settings)).map(a -> new CompletionItem(a)).collect(Collectors.toList())
                            .toArray(new CompletionItem[0]);
                } else if (currentLineTokens[currentLineTokens.length - 1].equals("METHOD")) {
                    for (int x = beforeLines.length - 1; x >= 0; x--) {
                        String[] secondaryLineTokens = beforeLines[x].split("\\s+");
                        for (int y = secondaryLineTokens.length - 2; y >= 0; y--) {
                            if (secondaryLineTokens[y].equals("CLASS")) {
                                if (provider.isMissingVmInfo()) {
                                    return COMPLETION_ITEMS_HELP;
                                }
                                String fqn = secondaryLineTokens[y + 1];
                                return Arrays.stream(provider.getWhateverFromClass(settings, fqn)).map(a -> new CompletionItem(a))
                                        .collect(Collectors.toList()).toArray(new CompletionItem[0]);
                            }
                        }
                    }
                    return new CompletionItem[]{new CompletionItem("no CLASS declaration found. You are on yor own", "", "no-op")};
                }
            }
            return currentSet;
        }
    }
}
