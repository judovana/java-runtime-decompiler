package org.jrd.backend.completion;

import org.jrd.frontend.frame.main.decompilerview.SupportedKeySets;
import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;

public class JrdCompletionSettings extends CompletionSettings {

    private final boolean dynamicClasses;
    private final boolean configAdditionalClasses;
    private final boolean methodNames;
    private final boolean methodFullSignatures;

    public static JrdCompletionSettings getDefault(ClassesAndMethodsProvider isDynamic) {
        return getDefault(isDynamic(isDynamic));
    }

    public static JrdCompletionSettings getDefault(boolean dynamicClasses) {
        if (dynamicClasses) {
            return new JrdCompletionSettings(
                    SupportedKeySets.getDefaultSet(), CompletionSettings.OP.SPARSE, false, true, true, false, true, true
            );
        } else {
            return new JrdCompletionSettings(
                    SupportedKeySets.getDefaultSet(), CompletionSettings.OP.SPARSE, false, true, false, true, true, true
            );
        }
    }

    public JrdCompletionSettings(
            CompletionItem.CompletionItemSet set, CompletionSettings.OP op, boolean caseSensitive, boolean showHelp, boolean dynamicClasses,
            boolean configAdditionalClasses, boolean methodNames, boolean methodFullSignatures
    ) {
        super(set, op, caseSensitive, showHelp);
        this.dynamicClasses = dynamicClasses;
        this.configAdditionalClasses = configAdditionalClasses;
        this.methodNames = methodNames;
        this.methodFullSignatures = methodFullSignatures;
    }

    public static boolean isDynamic(ClassesAndMethodsProvider isDynamic) {
        return !(isDynamic instanceof ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider);
    }

    public boolean isDynamicClasses() {
        return dynamicClasses;
    }

    public boolean isConfigAdditionalClasses() {
        return configAdditionalClasses;
    }

    public boolean isMethodNames() {
        return methodNames;
    }

    public boolean isMethodFullSignatures() {
        return methodFullSignatures;
    }
}
