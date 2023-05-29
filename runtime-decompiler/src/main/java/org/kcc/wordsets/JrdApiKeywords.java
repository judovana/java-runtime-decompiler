package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JrdApiKeywords implements CompletionItem.CompletionItemSet {
    private static final String INTRO = "Unless you are running DCEVM jdk, most JVMs will not allow you to add field or methods.\n" +
            "To help with this issue, JRD have appi where you can store instances of objects (eg runnable:) or boxed primitive). " +
            "and then reuse the.\n" +
            "You put them to the api via name and value, sometimes with parent class or instance, dependnign on api.\n" +
            "In implementation, those are simple hashmaps. But still it is 200% replacement for\n" +
            "Byteman's LinkMaps, CountDowns, Flags, Counters and Timers.\n" +
            "When hesitating, feel free to decompile content of org.jrd.agent.api pacakge\n";
    private static final String SAFE = "This operation is thread-safe (synchronised),\n";
    private static final String UNSAFE = "This operation is NOT thread-safe,\n";
    private static final String LOCAL = "Local fields and methods are bound to instacne of class - object.\n" +
            "They represents Class' public fields/methods, so you can access them globally/\n" +
            "Theirs main reason is as you would expect - to have field/method per instance.\n" +
            "Thats why al those methods have Object object as first parameter - the owner object.\n";
    private static final String CLAZZS = "Clazzs fields and methods are bound to class.\n" +
            "They represents Class' public static fields/methods, so you can access them globally/\n" +
            "Theirs main reason is if you really needs two methods/fields of same name in several classes.\n" +
            "Thats why al those methods have Class clazz as first parameter - the owner class.\n";
    //FIXME! write help!
    private static final CompletionItem[] BASE_JRDAPI_KEYWORDS =
            new CompletionItem[]{new CompletionItem(
                    "org.jrd.agent.api.Variables.NoSuchFakeVariableException",
                    "exception cast from " +
                            "eg methods which are getting/setting/creating new field/method but are not allowed to create or replace.\n" +
                            "Extends FakeVariableException"),
                    new CompletionItem("(Object)(org.jrd.agent.api.Variables.Local.create(Object, String, Object));",
                    INTRO + LOCAL + SAFE +
                            "Allows you to create local field/method in object(1st param) of String name of " +
                            "Object(third param)" +
                            "initial value. Will throw NoSuchFakeVariableException if that variable " +
                            "already exists.\n" +
                            "it returns freshly created field (the 3rd argument)"),
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
                    new CompletionItem("(String)(org.jrd.agent.api.UnsafeVariables.dumpAll());", "")};

    private static final CompletionItem[] JRDAPI_KEYWORDS =
            JavaKeywordsWithHelp.concatWithArrayCopy(JavaKeywordsWithHelp.EXT_JAVA_KEYWORDS, BASE_JRDAPI_KEYWORDS);

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
