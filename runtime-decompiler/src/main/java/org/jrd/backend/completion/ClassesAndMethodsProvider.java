package org.jrd.backend.completion;

import org.jrd.backend.data.Config;
import org.jrd.backend.decompiling.JavapDisassemblerWrapper;
import org.kcc.CompletionSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ClassesAndMethodsProvider {

    public String cpTextInfo();

    String[] getClasses(CompletionSettings settings);

    String[] getWhateverFromClass(CompletionSettings settings, String fqn);

    byte[] getClassItself(CompletionSettings settings, String fqn);

    class SettingsClassesAndMethodsProvider implements ClassesAndMethodsProvider {

        @Override
        public String cpTextInfo() {
            String s = Config.getConfig().getAdditionalCP();
            if (s.length()>300){
                return "local cp; items count:" + s.split(System.getProperty("path.separator")).length;
            }
            return s;
        }

        @Override
        public String[] getClasses(CompletionSettings settings) {
            return Config.getConfig().getAdditionalClassPathListing();
        }

        @Override
        public String[] getWhateverFromClass(CompletionSettings settings, String fqn) {
            return getMethodsFromAdditionalClassPath(settings, fqn);
        }

        @Override
        public byte[] getClassItself(CompletionSettings settings, String fqn) {
            return Config.getConfig().getAdditionalClassPathBytes(fqn);
        }

    }

    static String[] getMethodsFromAdditionalClassPath(CompletionSettings settings, String fqn) {
        byte[] b = Config.getConfig().getAdditionalClassPathBytes(fqn);
        String[] l = bytesToMethods(settings, b);
        if (l.length == 0) {
            return new String[]{"Not found " + fqn + " or no methods in it"};
        } else {
            return l;
        }
    }

    static String[] bytesToMethods(CompletionSettings settings, byte[] b) {
        boolean fqns = true;
        boolean names = true;
        if (settings instanceof JrdCompletionSettings) {
            fqns = ((JrdCompletionSettings) settings).isMethodFullSignatures();
            names = ((JrdCompletionSettings) settings).isMethodNames();
        }
        JavapDisassemblerWrapper javap = new JavapDisassemblerWrapper("");
        String code = javap.decompile(b, new String[0]);
        String[] lines = code.split("\n");
        List<String> r = new ArrayList<>(lines.length);
        Set<String> shortened = new HashSet<>();
        for (String s : lines) {
            if (s.startsWith("  ") && s.contains("(") && s.contains(")")) {
                String[] mtrim = s.replaceAll("\\(.*", "").split("\\s+");
                if (names) {
                    shortened.add(mtrim[mtrim.length - 1] + "(..)");
                }
                if (fqns) {
                    r.add(s.trim());
                }
            }
        }
        r.addAll(shortened);
        return r.toArray(new String[0]);
    }

    static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

}
