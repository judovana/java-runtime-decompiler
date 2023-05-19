package org.jrd.backend.completion;

import org.jrd.backend.data.Config;

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
            return new String[0];
            //fixme decompile and extract methods
            //return Config.getConfig().getAdditionalClassPathBytes(fqn);
        }
    }

}
