package org.jrd.backend.communication;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

public class RuntimeCompilerConnector {

    public static class JRDClassesProvider implements ClassesProvider {
        private final VmInfo vmInfo;
        private final VmManager vmManager;

        public JRDClassesProvider(VmInfo vmInfo, VmManager vmManager) {
            this.vmInfo = vmInfo;
            this.vmManager = vmManager;
        }

        @Override
        public Collection<IdentifiedBytecode> getClass(ClassIdentifier... classIdentifiers) {
            List<IdentifiedBytecode> results = new ArrayList<>(classIdentifiers.length);
            for (ClassIdentifier clazz : classIdentifiers) {
                VmDecompilerStatus result = Cli.obtainClass(vmInfo, clazz.getFullName(), vmManager);
                byte[] ba = Base64.getDecoder().decode(result.getLoadedClassBytes());
                results.add(new IdentifiedBytecode(new ClassIdentifier(clazz.getFullName()), ba));
            }
            return results;
        }

        @Override
        public List<String> getClassPathListing() {
            AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo, AgentRequestAction.RequestAction.CLASSES);
            String response = VmDecompilerInformationController.submitRequest(vmManager, request);
            if (response.equals("ok")) {
                String[] classes = vmInfo.getVmDecompilerStatus().getLoadedClassNames();
                return Arrays.asList(classes);
            } else {
                throw new RuntimeException("Error obtaining list of classes: " + response);
            }
        }
    }

    public static class ForeignCompilerWrapper implements ClasspathlessCompiler {
        private final DecompilerWrapperInformation currentDecompiler;

        public ForeignCompilerWrapper(DecompilerWrapperInformation currentDecompiler) {
            this.currentDecompiler = currentDecompiler;
        }

        @Override
        public Collection<IdentifiedBytecode> compileClass(ClassesProvider classprovider, Optional<MessagesListener> messagesConsummer, IdentifiedSource... javaSourceFiles) {
            try {
                Map<String,String> inputs = new HashMap<>();
                for(IdentifiedSource is: javaSourceFiles) {
                    inputs.put(is.getClassIdentifier().getFullName(), is.getSourceCode());
                }
                Object r = currentDecompiler.getCompileMethod().invoke(currentDecompiler.getInstance(), inputs, new String[0], messagesConsummer.get());
                Map<String, byte[]> rr = (Map<String, byte[]>) r;
                List<IdentifiedBytecode> rrr = new ArrayList<>(rr.size());
                for(Map.Entry<String, byte[]> e: rr.entrySet()){
                    rrr.add(new IdentifiedBytecode(new ClassIdentifier(e.getKey()), e.getValue()));
                }
                return  rrr;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
