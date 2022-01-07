package org.jrd.frontend.frame.main;

import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;

public interface ModelProvider {

    VmInfo getVmInfo();

    VmManager getVmManager();

    RuntimeCompilerConnector.JrdClassesProvider getClassesProvider();
}
