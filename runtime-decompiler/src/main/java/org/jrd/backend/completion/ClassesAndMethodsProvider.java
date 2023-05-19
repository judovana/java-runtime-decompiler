package org.jrd.backend.completion;

import io.github.mkoncek.classpathless.api.ClassesProvider;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;

import javax.swing.JList;
import java.util.Arrays;
import java.util.stream.Collectors;

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
