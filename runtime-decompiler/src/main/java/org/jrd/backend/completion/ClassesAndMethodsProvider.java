package org.jrd.backend.completion;

import org.jrd.backend.data.VmInfo;

import javax.swing.JList;

public interface ClassesAndMethodsProvider {

    String[] getClasses();
    String[] getWhateverFromClass(String fqn);


    class JrdClassesAndMethodsProvider implements ClassesAndMethodsProvider {
        public JrdClassesAndMethodsProvider(JList<VmInfo> localVmList) {

        }
    }
}
