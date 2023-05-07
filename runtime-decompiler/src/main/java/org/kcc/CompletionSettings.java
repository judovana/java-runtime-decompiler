package org.kcc;

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
}
