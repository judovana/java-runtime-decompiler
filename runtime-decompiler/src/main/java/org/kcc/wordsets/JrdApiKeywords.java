package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JrdApiKeywords  implements  CompletionItem.CompletionItemSet{
    //FIXME! write help!
    private static final CompletionItem[] JRDAPI_KEYWORDS = new CompletionItem[]{
            new CompletionItem("org.jrd.agent.api.Variables.NoSuchFakeVariableException", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.create(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.get(Object, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.getOrCreate(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.remove(Object, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.set(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.setNoReplace(Object, String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.Local.dump());", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Local.destroy();", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Local.removeAll(Object);", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.create(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.get(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.getOrCreate(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.remove(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.set(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.setNoReplace(String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.Global.dump());", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Global.removeAll();", ""),
            new CompletionItem("org.jrd.agent.api.Variables.FakeVariableException", ""),
            new CompletionItem("org.jrd.agent.api.Variables.FakeVariableAlreadyDeclaredException", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.create(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.create(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.create(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.get(Class, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.get(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.get(String, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.remove(Class, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.remove(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.remove(String, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.set(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.set(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.set(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(String, String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.Clazzs.dump());", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.destroy();", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll();", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll(Class);", ""),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll(String);", ""),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.dumpAll());", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.create(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.get(Object, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.getOrCreate(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.remove(Object, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.set(Object, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.setNoReplace(Object, String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.UnsafeVariables.Local.dump());", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Local.destroy();", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Local.removeAll(Object);", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.create(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.get(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.getOrCreate(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.remove(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.set(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Global.setNoReplace(String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.UnsafeVariables.Global.dump());", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Global.removeAll();", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(Class, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(String, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(Class, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(String, String));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(String, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(Class, String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(String, Object));", ""),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(String, String, Object));", ""),
            new CompletionItem("(String)(org.jrd.agent.api.UnsafeVariables.Clazzs.dump());", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Clazzs.destroy();", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll();", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll(Class);", ""),
            new CompletionItem("org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll(String);", ""),
            new CompletionItem("(String)(org.jrd.agent.api.UnsafeVariables.dumpAll());", ""),
    };


    @Override
    public CompletionItem[] getItemsArray() {
        CompletionItem[] r = Arrays.copyOf(JRDAPI_KEYWORDS, JRDAPI_KEYWORDS.length);
        Arrays.sort(r);
        return r;
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = Arrays.asList(JRDAPI_KEYWORDS);
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        return CompletionItem.CompletionItemSet.delimiterSet();
    }

    @Override
    public String toString() {
        return "JRD runtime modification api. Good to connect wit java.";
    }
}
