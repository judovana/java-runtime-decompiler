package com.redhat.thermostat.vm.decompiler.core;

import com.redhat.thermostat.shared.locale.Translate;

/**
 * Localized messages for error messages while processing the request.
 */
public enum LocaleResources {
    
    REQUEST_FAILED_AUTH_ISSUE,
    REQUEST_FAILED_UNKNOWN_ISSUE,
    ERROR_UNKNOWN_RESPONSE,
    ;
    
    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.vm.decompiler.core.strings";
    
    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}
