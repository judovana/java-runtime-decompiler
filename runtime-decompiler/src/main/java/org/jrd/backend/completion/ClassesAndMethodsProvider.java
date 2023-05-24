package org.jrd.backend.completion;

import org.jrd.backend.data.Config;
import org.jrd.backend.decompiling.JavapDisassemblerWrapper;

import java.util.ArrayList;
import java.util.List;

public interface ClassesAndMethodsProvider {

    String[] getClasses();

    String[] getWhateverFromClass(String fqn);

    class SettingsClassesAndMethodsProvider implements ClassesAndMethodsProvider {

        @Override
        public String[] getClasses() {
            return Config.getConfig().getAdditionalClassPathListing();
        }

        @Override
        public String[] getWhateverFromClass(String fqn) {
            byte[] b = Config.getConfig().getAdditionalClassPathBytes(fqn);
            String[] l = bytesToMethods(b);
            if (l.length == 0) {
                return new String[]{"Not found " + fqn + " or no methods in it"};
            } else {
                return l;
            }
        }
    }

    static String[] bytesToMethods(byte[] b) {
        JavapDisassemblerWrapper javap = new JavapDisassemblerWrapper("");
        String code = javap.decompile(b, new String[0]);
        String[] lines = code.split("\n");
        List<String> r = new ArrayList<>(lines.length);
        for (String s : lines) {
            if (s.startsWith("  ") && s.contains("(") && s.contains(")")) {
                r.add(s.trim());
            }
        }
        return r.toArray(new String[0]);
    }

}
