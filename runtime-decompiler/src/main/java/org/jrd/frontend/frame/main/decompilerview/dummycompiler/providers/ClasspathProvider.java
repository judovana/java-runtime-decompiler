package org.jrd.frontend.frame.main.decompilerview.dummycompiler.providers;

import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

public interface ClasspathProvider {

    ClassesAndMethodsProvider getClasspath();

    VmInfo getVmInfo();

    VmManager getVmManager();

    class SettingsClasspathProvider implements ClasspathProvider {

        @Override
        public ClassesAndMethodsProvider getClasspath() {
            return new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider();
        }

        @Override
        public VmInfo getVmInfo() {
            return null;
        }

        @Override
        public VmManager getVmManager() {
            return null;
        }
    }
}
