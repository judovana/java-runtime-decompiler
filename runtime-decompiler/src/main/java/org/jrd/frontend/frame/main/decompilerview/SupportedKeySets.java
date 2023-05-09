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
    public static final SupportedKeySets JRD_KEY_SETS =
            new SupportedKeySets(new JrdApiKeywords(), new BytemanKeywords(), new BytecodeKeywordsWithHelp(), new JavaKeywordsWithHelp());

    public static final CompletionSettings JRD_DEFAULT =
            new CompletionSettings(JRD_KEY_SETS.sets[0], CompletionSettings.OP.SPARSE, false, true);

    private final CompletionItem.CompletionItemSet[] sets;

    public SupportedKeySets(CompletionItem.CompletionItemSet... sets) {
        this.sets = sets;
    }

    public CompletionItem.CompletionItemSet[] getSets() {
        return Arrays.copyOf(sets, sets.length);
    }

    public List<CompletionItem.CompletionItemSet> recognize(String textWithKeywords) {
        CompletionSettings.RecognitionResult[] rr = CompletionSettings.recognize(textWithKeywords, sets);
        List<CompletionItem.CompletionItemSet> result = new ArrayList<>();
        for (CompletionSettings.RecognitionResult r : rr) {
            Logger.getLogger().log(Logger.Level.DEBUG, sets[r.getIndex()].toString());
            Logger.getLogger().log(Logger.Level.DEBUG, r.toString());
            if (r.getPercent() > 5 && result.size() < 2) {
                result.add(r.getSet());
                Logger.getLogger().log(Logger.Level.DEBUG, "accepted");
            }
        }
        return result;
    }

    public boolean isByteman(CompletionItem.CompletionItemSet set) {
        return set.toString().equals(sets[1].toString());
    }

    public boolean isJasm(CompletionItem.CompletionItemSet set) {
        return set.toString().equals(sets[2].toString());
    }

    public boolean isJava(CompletionItem.CompletionItemSet set) {
        return set.toString().equals(sets[3].toString());
    }
}
