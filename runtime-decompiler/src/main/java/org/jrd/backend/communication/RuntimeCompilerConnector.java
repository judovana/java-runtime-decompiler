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

    private static final boolean slow = true;

    public static class DummyRuntimeCompiler implements ClasspathlessCompiler {

        @Override
        public Collection<IdentifiedBytecode> compileClass(ClassesProvider classesProvider, Optional<MessagesListener> messagesListener, IdentifiedSource... identifiedSources) {
            List<IdentifiedBytecode> results = new ArrayList<>(identifiedSources.length);
            try {
                for (IdentifiedSource is : identifiedSources) {
                    messagesListener.ifPresent(message -> {
                        message.addMessage(Level.INFO, "Compiling " + is.getClassIdentifier().getFullName() + " of lenght of " + is.getFile().length + " bytes (" + getSrcLengthCatched(is) + " characters)");
                    });
                    sleepIfSlow(1000);
                    for (int x = 0; x < 3; x++) {
                        messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "Listing classes"));
                        List<String> l = classesProvider.getClassPathListing();
                        messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "Listed " + l.size()));
                        for (int i = 0; i < 3; i++) {
                            String clname = l.get(new Random().nextInt(l.size() / 2));
                            messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "Obtaining class: " + clname));
                            Collection<IdentifiedBytecode> obtained = classesProvider.getClass(new ClassIdentifier(clname));
                            messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "got " + obtained.size() + " classes: "));
                            for (IdentifiedBytecode ib : obtained) {
                                messagesListener.ifPresent(message -> message.addMessage(Level.INFO, ib.getClassIdentifier().getFullName() + " of " + ib.getFile().length + " bytes"));
                            }
                            sleepIfSlow(1000);
                        }
                        sleepIfSlow(1000);
                    }
                    IdentifiedBytecode ib = new IdentifiedBytecode(is.getClassIdentifier(), ("Freshly compiled " + is.getClassIdentifier().getFullName() + " from src of lenght of " + is.getFile().length + " bytes (" + getSrcLengthCatched(is) + " characters) at " + new Date().toString()).getBytes());
                    messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "Compiled " + ib.getClassIdentifier().getFullName() + " to " + ib.getFile().length + " bytes"));
                    results.add(ib);
                }
                int i = 0;
                Random r = new Random();
                while (r.nextBoolean()) {
                    i++;
                    String n = "random.inner$class" + i;
                    IdentifiedBytecode ib = new IdentifiedBytecode(new ClassIdentifier(n), ("Freshly compiled " + n + new Date().toString()).getBytes());
                    messagesListener.ifPresent(message -> message.addMessage(Level.INFO, "Compiled " + ib.getClassIdentifier().getFullName() + " to " + ib.getFile().length + " bytes"));
                    results.add(ib);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return results;
        }

        private void sleepIfSlow(long i) throws InterruptedException {
            if (slow) {
                Thread.sleep(i);
            }
        }

    }

    private static int getSrcLengthCatched(IdentifiedSource is) {
        return is.getSourceCode().length();
    }

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
        private final PluginManager pluginManager;
        private final DecompilerWrapperInformation currentDecompiler;

        public ForeignCompilerWrapper(PluginManager pm, DecompilerWrapperInformation currentDecompiler) {
            this.pluginManager = pm;
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
