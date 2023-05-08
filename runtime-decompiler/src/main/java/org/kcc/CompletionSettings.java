package org.kcc;

import org.kcc.wordsets.ConnectedKeywords;

import java.util.Arrays;
import java.util.regex.Pattern;

public class CompletionSettings {
    public enum OP {
        STARTS, CONTAINS, SPARSE, MAYHEM
    }

    private final boolean caseSensitive;
    private final boolean showHelp;
    private final OP op;

    private final CompletionItem.CompletionItemSet set;

    public CompletionSettings(CompletionItem.CompletionItemSet set, OP op, boolean caseSensitive, boolean showHelp) {
        this.set = set;
        this.op = op;
        this.showHelp = showHelp;
        this.caseSensitive = caseSensitive;
    }

    public CompletionItem.CompletionItemSet getSet() {
        return set;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public OP getOp() {
        return op;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public static class RecognitionResult implements  Comparable<RecognitionResult>{
        private final int words;
        private final int hits;
        private final int percent;
        private final int index;
        private final CompletionItem.CompletionItemSet set;

        public RecognitionResult(int words, int hits, int percent, int index, CompletionItem.CompletionItemSet set)  {
            this.words = words;
            this.hits = hits;
            this.percent = percent;
            this.index = index;
            this.set = set;
        }

        public int getWords() {
            return words;
        }

        public int getHits() {
            return hits;
        }

        public int getPercent() {
            return percent;
        }

        public int getIndex() {
            return index;
        }

        public CompletionItem.CompletionItemSet getSet() {
            return set;
        }

        @Override
        public String toString() {
            return "RecognitionResult{" +
                    "words=" + words +
                    ", hits=" + hits +
                    ", percent=" + percent +
                    ", set index=" + index +
                    '}';
        }

        @Override
        public int compareTo(RecognitionResult recognitionResult) {
            return recognitionResult.percent-percent;
        }
    }


    public static RecognitionResult[] recognize(String stext, CompletionItem.CompletionItemSet... sets) {
        char[] text = stext.toCharArray();
        int[] h = new int[sets.length];
        int[] w = new int[sets.length];
        int[] p = new int[sets.length];
        for (int i = 0; i < sets.length; i++) {
            CompletionItem.CompletionItemSet set = sets[i];
            Pattern nondelimiter = set.getRecommendedDelimiterSet();
            h[i] = 0;
            w[i] = 0;
            p[i] = 0;
            StringBuilder currentWord = new StringBuilder();
            for (int x = 0; x < text.length; x++) {
                String currenChar = String.valueOf(text[x]);
                if (!nondelimiter.matcher(currenChar).matches()) {
                    if (currentWord.length() > 1) {
                        w[i]++;
                        if (isInSet(currentWord.toString(), set)) {
                            h[i]++;
                        }
                    }
                    currentWord = new StringBuilder();
                } else {
                    currentWord.append(currenChar);
                }
            }
            long percent = 0;
            if (w[i]>0) {
                percent = (int) (((long) h[i] * 100l) / w[i]);
            }
            p[i] = (int)percent;
        }
        RecognitionResult[] r= new RecognitionResult[sets.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = new RecognitionResult(w[i], h[i], p[i], i, sets[i]);
        }
        Arrays.sort(r);
        return r;
    }


    private static boolean isInSet(String word, CompletionItem.CompletionItemSet set) {
        for (CompletionItem item : set.getItemsArray()) {
            if (item.getKey().length() > 1 && word.length() > 1 && item.getKey().equals(word)) {
                return true;
            }
        }
        return false;
    }


}
