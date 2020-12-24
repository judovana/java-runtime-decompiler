package org.jrd.backend.communication;

import org.jc.api.ClassIdentifier;
import org.jc.api.ClassesProvider;
import org.jc.api.IdentifiedBytecode;
import org.jc.api.IdentifiedSource;
import org.jc.api.InMemoryCompiler;
import org.jc.api.MessagesListener;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

public class RuntimeCompilerConnector {

    private static final boolean slow = true;

    public static class DummyRuntimeCompiler implements InMemoryCompiler {

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
            if (slow){
                Thread.sleep(i);
            }
        }

    }

    private static int getSrcLengthCatched(IdentifiedSource is) {
        try {
            return is.getSourceCode().length();
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
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
}
