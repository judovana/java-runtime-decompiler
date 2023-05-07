package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.Logger;
import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.BytemanKeywords;
import org.kcc.wordsets.JavaKeywordsWithHelp;
import org.kcc.wordsets.JrdApiKeywords;

import java.util.ArrayList;
import java.util.Arrays;
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
        CompletionSettings.RecognitionResult[] rr = CompletionSettings.recognize(textWithKeywords, sets);
        List<CompletionItem.CompletionItemSet> result = new ArrayList<>();
        for (CompletionSettings.RecognitionResult r : rr) {
            Logger.getLogger().log(Logger.Level.DEBUG, sets[r.getIndex()].toString());
            Logger.getLogger().log(Logger.Level.DEBUG, r.toString());
            if (r.getPercent() > 5 && result.size() < 2) {
                result.add(sets[r.getIndex()]);
                Logger.getLogger().log(Logger.Level.DEBUG, "accepted");
            }
        }
        return result;
    }
}
