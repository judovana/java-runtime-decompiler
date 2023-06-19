package org.jrd.frontend.frame.main;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

import io.github.mkoncek.classpathless.api.ClassesProvider;

public interface ModelProvider {

    VmInfo getVmInfo();

    VmManager getVmManager();

    ClassesProvider getClassesProvider();
}
