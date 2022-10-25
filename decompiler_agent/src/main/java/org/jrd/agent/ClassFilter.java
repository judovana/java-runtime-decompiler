package org.jrd.agent;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

public class ClassFilter {

    private final Pattern nameFilter;
    private final String bodySubstring;

    public ClassFilter(Optional<String> bodySubstring, Optional<String> nameFilter) {
        this.nameFilter = Pattern.compile(nameFilter.orElseGet(() -> ".*"));
        this.bodySubstring = bodySubstring.orElseGet(() -> "");
    }

    public static Optional<ClassFilter> create(String substringAndRegexLine) {
        if (substringAndRegexLine == null) {
            return Optional.empty();
        }
        final String[] substringAndRegex = substringAndRegexLine.trim().split("\\s+");
        if (substringAndRegex.length == 0) {
            return Optional.empty();
        }
        if (substringAndRegex.length == 1) {
            return Optional.of(new ClassFilter(Optional.of(substringAndRegex[0]), Optional.empty()));
        }
        return Optional.of(new ClassFilter(Optional.of(substringAndRegex[0]), Optional.of(substringAndRegex[1])));
    }

    public boolean match(InstrumentationProvider instrumentationProvider, Class loadedClass) {
        try {
            if (nameFilter.matcher(loadedClass.getName()).matches()) {
                byte[] b = instrumentationProvider.getClassBody(loadedClass);
                String ascii = new String(b, StandardCharsets.US_ASCII);
                if (ascii.contains(bodySubstring)) {
                    return true;
                } else {
                    String utf8 = new String(b, StandardCharsets.UTF_8);
                    if (utf8.contains(bodySubstring)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
