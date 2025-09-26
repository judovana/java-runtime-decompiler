package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

//Warning. Whne working on this file, keep backup. It is often wrongly reformated during mvn build
//ig it is not worked on and formatted, you can  git checkout runtime-decompiler/src/main/java/org/kcc/wordsets/JrdApiKeywords.java
public class JrdApiKeywords implements CompletionItem.CompletionItemSet {
    private static final String INTRO = "Unless you are running DCEVM jdk, most JVMs will not allow you to add field or methods.\n" +
            "To help with this issue, JRD have appi where you can store instances of objects (eg runnable:) or boxed primitive). " +
            "and then reuse the.\n" +
            "You put them to the api via name and value, sometimes with parent class or instance, dependnign on api.\n" +
            "In implementation, those are simple hashmaps. But still it is 200% replacement for\n" +
            "Byteman's LinkMaps, CountDowns, Flags, Counters and Timers.\n" +
            "When hesitating, feel free to decompile content of org.jrd.agent.api package.\n\n" +
            "You you usually need only SET and  GEToRcREATE methods og Local/Global scope. Others are for special cases.\n\n";
    private static final String SAFE = "This operation is thread-safe (synchronised).\n\n";
    private static final String UNSAFE = "This operation is NOT thread-safe. Oterwise same as its safe alignment\n";
    private static final String LOCAL = "Local fields and methods are bound to instacne of class - object.\n" +
            "They represents Class' public fields/methods, so you can access them globally.\n" +
            "Theirs main reason is as you would expect - to have field/method per instance.\n" +
            "Thats why al those methods have Object object as first parameter - the owner object.\n\n";
    private static final String CLAZZS = "Clazzs fields and methods are bound to class.\n" +
            "They represents Class' public static fields/methods, so you can access them globally.\n" +
            "Theirs main reason is if you really needs two methods/fields of same name in several classes.\n" +
            "Thats why al those methods have Class clazz as first parameter - the owner class.\n" +
            "Note, that this is usually not used, and you are usually ok with `Global` field/method.\n\n";
    private static final String CLAZZS_FINAL = " Unlike global, it is bound to class. Unlike Local, which is bound to instance of " +
            "class, this one is bound to Class declaration. Otherwise same as Local/Global fake fields/methods\n";
    private static final String CLAZZS_UNBOUND =
            "\nThis call is lacking initial Class, and is trying to detect it." + "It is not always what you require\n";
    private static final String GLOBAL = "Global fields and methods are unbound.\n" +
            "They represents C/Pascal const wittou namesapce so you can access them globally.\n" +
            "It is simple pair name/value.\n\n";

    private static final CompletionItem[] BASE_JRDAPI_KEYWORDS = new CompletionItem[] {
            new CompletionItem(
                    "org.jrd.agent.api.Variables.NoSuchFakeVariableException",
                    "exception cast from " + "eg methods which are getting/setting/removing new field/method but are not allowed to create.\n" +
                            "Extends FakeVariableException"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.create(Object, String, Object));",
                    INTRO + LOCAL + SAFE + "Allows you to create local field/method in object(1st param) of String name of " +
                            "Object(third param)" + "initial value. Will throw FakeVariableAlreadyDeclaredException if that variable " +
                            "already exists.\n" + "it returns freshly created field (the 3rd argument)"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.get(Object, String));",
                    INTRO + LOCAL + SAFE + "get the value of stored local field/method in object(1st param) of String name of " +
                            "Object(second param). Will throw NoSuchFakeVariableException if that variable " + "do not already exists."),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.getOrCreate(Object, String, Object));",
                    INTRO + LOCAL + SAFE + "Allows you to adjust/create local field/method in object(1st param) of String name of " +
                            "Object(third param)" + "initial value. If that variable already exists, will be rewritten.\n" +
                            "it returns freshly created field (the 3rd argument)"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.remove(Object, String));",
                    INTRO + LOCAL + SAFE + "Will remove the declared fake method/variable " + "of name String from instance of Object"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.set(Object, String, Object));", INTRO + LOCAL + SAFE +
                    "Will set the variable/method of String name to instance of Object (first param) to value of " +
                    "Object (3rd param). Unlike create, it do not throw exception if the field is already created. " +
                    "If field is not created, it will be.  Returns freshly inserted value."),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Local.setNoReplace(Object, String, Object));",
                    INTRO + LOCAL + SAFE + "Will set the variable/method of String name to instance of Object (first param) to value of " +
                            "Object (3rd param). Unlike create, it do not throw exception if the field is already created. " +
                            "But unlike set, if it already exists, it throws FakeVariableAlreadyDeclaredException if the " +
                            "variable/method is already set. Returns freshly inserted value."),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.Variables.Local.dump());",
                    "Will dump to String all safe, local variables/methods currently declared"),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.Variables.Local.dump(Object...));",
                    "Will dump to String all safe, local variables/methods currently " + "declared in selected Object...s"),
            new CompletionItem("org.jrd.agent.api.Variables.Local.destroy();",
                    "Will remove all safe, local variables/methods currently declared"),
            new CompletionItem(
                    "org.jrd.agent.api.Variables.Local.removeAll(Object);",
                    "Will remove all safe, local variables/methods bound to given Object"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Global.create(String, Object));",
                    INTRO + GLOBAL + SAFE + "Allows you to create global field/method of String name of Object(second param)" +
                            "initial value. Will throw FakeVariableAlreadyDeclaredException if that variable " + "already exists.\n" +
                            "it returns freshly created field (the 2nf argument)"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Global.get(String));",
                    INTRO + GLOBAL + SAFE + "get the value of stored global field/method of String name" +
                            "). Will throw NoSuchFakeVariableException if that variable " + "do not already exists."),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Global.getOrCreate(String, Object));",
                    INTRO + GLOBAL + SAFE + "Allows you to adjust/create global field/method of String name of Object(seconf param)" +
                            "initial value. If that variable already exists, will be rewritten.\n" +
                            "it returns freshly created field (the 2nd argument)"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Global.remove(String));",
                    INTRO + GLOBAL + SAFE + "Will remove the declared fake method/variable " + "of name String from global scope"),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Global.set(String, Object));",
                    INTRO + GLOBAL + SAFE + "Will set the global variable/method of String name to value of " +
                            "Object (2nd param). Unlike create, it do not throw exception if the field is already created. " +
                            "If field is not created, it will be.  Returns freshly inserted value."),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Global.setNoReplace(String, Object));",
                    INTRO + GLOBAL + SAFE + "Will set the global variable/method of String name to value of " +
                            "Object (2nd param). Unlike create, it do not throw exception if the field is already created. " +
                            "But unlike set, if it already exists, it throws FakeVariableAlreadyDeclaredException if the " +
                            "variable/method is already set. Returns freshly inserted value."),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.Variables.Global.dump());",
                    INTRO + GLOBAL + SAFE + "Will dump to String all safe, global variables/methods currently declared"),
            new CompletionItem(
                    "org.jrd.agent.api.Variables.Global.removeAll();",
                    INTRO + GLOBAL + SAFE + "Will remove all safe, global variables/methods." +
                            "It is moreover equal to destroy method of Clazzs anf Locals."),
            new CompletionItem(
                    "org.jrd.agent.api.Variables.FakeVariableException", "Generic forefather for all fake values exceptions"),
            new CompletionItem(
                    "org.jrd.agent.api.Variables.FakeVariableAlreadyDeclaredException",
                    "exception cast from " + "eg methods which are creating/setting new field/method but are not allowed to replace.\n" +
                            "Extends FakeVariableException"),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.create(Class, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.create(String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.create(String, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.get(Class, String));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.get(String));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.get(String, String));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(Class, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.getOrCreate(String, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.remove(Class, String));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.remove(String));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.remove(String, String));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.set(Class, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.set(String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.set(String, String, Object));", INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(Class, String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL + CLAZZS_UNBOUND),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.Variables.Clazzs.setNoReplace(String, String, Object));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.Clazzs.dump());",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("(String)(org.jrd.agent.api.Variables.Clazzs.dump(Class...));",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.destroy();",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll();",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll(Class);",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL),
            new CompletionItem("org.jrd.agent.api.Variables.Clazzs.removeAll(String);",
                    INTRO + CLAZZS + SAFE + CLAZZS_FINAL + "This calls classForName on String."),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.Variables.dumpAll());", "Will dump to String all safe variables currently declared"),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.create(Object, String, Object));", UNSAFE),
            new CompletionItem("(Object)(org.jrd.agent.api.UnsafeVariables.Local.get(Object, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Local.getOrCreate(Object, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Local.remove(Object, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Local.set(Object, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Local.setNoReplace(Object, String, Object));", UNSAFE),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.UnsafeVariables.Local.dump());", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Local.destroy();", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Local.removeAll(Object);", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.create(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.get(String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.getOrCreate(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.remove(String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.set(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Global.setNoReplace(String, Object));", UNSAFE),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.UnsafeVariables.Global.dump());", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Global.removeAll();", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(Class, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.create(String, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(Class, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.get(String, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(Class, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.getOrCreate(String, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(Class, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.remove(String, String));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(Class, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.set(String, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(Class, String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(String, Object));", UNSAFE),
            new CompletionItem(
                    "(Object)(org.jrd.agent.api.UnsafeVariables.Clazzs.setNoReplace(String, String, Object));", UNSAFE),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.UnsafeVariables.Clazzs.dump());", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Clazzs.destroy();", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll();", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll(Class);", UNSAFE),
            new CompletionItem(
                    "org.jrd.agent.api.UnsafeVariables.Clazzs.removeAll(String);", UNSAFE),
            new CompletionItem(
                    "(String)(org.jrd.agent.api.UnsafeVariables.dumpAll());", "Will dump to String all unsafe variables currently declared")};

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
