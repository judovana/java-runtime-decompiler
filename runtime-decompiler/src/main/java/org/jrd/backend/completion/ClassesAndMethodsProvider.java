package org.jrd.backend.completion;

import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.PluginManager;

import javax.swing.JList;
import java.util.Arrays;
import java.util.stream.Collectors;

public interface ClassesAndMethodsProvider {

    String[] getClasses();
    String[] getWhateverFromClass(String fqn);


    class JrdClassesAndMethodsProvider implements ClassesAndMethodsProvider {
        public JrdClassesAndMethodsProvider(JList<VmInfo> localVmList, VmManager vmManager, PluginManager pluginManager) {

        }

        @Override
        public String[] getClasses() {
            return new String[0];
        }

        @Override
        public String[] getWhateverFromClass(String fqn) {
            return new String[0];
        }
    }

    class SettingsClassesAndMethodsProvider implements  ClassesAndMethodsProvider {

        @Override
        public String[] getClasses() {
            return Config.getConfig().getAdditionalClassPathListing();
        }

        @Override
        public String[] getWhateverFromClass(String fqn) {
            return new String[0];
            //fixme decompile and extract methods
            //return Config.getConfig().getAdditionalClassPathBytes(fqn);
        }
    }

    class PreloadedClassesProvider implements  ClassesAndMethodsProvider {

        private final String[] classes;

        public PreloadedClassesProvider(ClassInfo[] classes) {
            this.classes =
                    Arrays.stream(classes).map(a->a.getName()).collect(Collectors.toList()).toArray(new String[0]);
        }

        @Override
        public String[] getClasses() {
            return classes;
        }

        @Override
        public String[] getWhateverFromClass(String fqn) {
            return new String[0];
        }
    }
}
