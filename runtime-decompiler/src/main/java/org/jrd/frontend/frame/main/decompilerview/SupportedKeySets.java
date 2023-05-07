package org.jrd.frontend.frame.main.decompilerview;

import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.BytemanKeywords;
import org.kcc.wordsets.JavaKeywordsWithHelp;
import org.kcc.wordsets.JrdApiKeywords;

import java.util.ArrayList;
import java.util.List;

public class SupportedKeySets {
    public static final SupportedKeySets JrdKeySets = new SupportedKeySets(
            new JrdApiKeywords(),
            new BytemanKeywords(),
            new BytecodeKeywordsWithHelp(),
            new JavaKeywordsWithHelp());

    public static final CompletionSettings JrdDefault = new CompletionSettings(JrdKeySets.sets[0], CompletionSettings.OP.SPARSE, false, true);


    private final CompletionItem.CompletionItemSet[] sets;

    public SupportedKeySets(CompletionItem.CompletionItemSet... sets) {
        this.sets = sets;
    }

    public CompletionItem.CompletionItemSet[] getSets() {
        return sets;
    }

    public List<CompletionItem.CompletionItemSet> recognize(String textWithKeywords) {
        //FIXME implement
        //implement by static method in kcc; taking CompletionItem.CompletionItemSet[] parameter,
        //returning int[] % array of how much inputs matched
        //then return all whic mathced with 30%+(?) sorted...?
        //they will be merged, so meybe not...
        return new ArrayList<>();
    }
}
