package org.jrd.backend.data.cli.workers;

import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.FqnAndClassToJar;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.InMemoryJar;
import org.jrd.backend.data.cli.Lib;
import org.jrd.frontend.frame.main.decompilerview.verifiers.ClassVerifier;
import org.jrd.frontend.frame.main.decompilerview.verifiers.FileVerifier;
import org.jrd.frontend.frame.main.decompilerview.verifiers.GetSetText;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AddClasses {

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final boolean isBoot;

    public AddClasses(List<String> filteredArgs, VmManager vmManager, boolean isBoot) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
        this.isBoot = isBoot;
    }

    public VmInfo addClasses() throws IOException {
        if (filteredArgs.size() < 2) {
            throw new RuntimeException("Expected two and more arguments - " + Help.ADD_CLASSES_FORMAT1 + " or " + Help.ADD_CLASSES_FORMAT2);
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        boolean allExists = true;
        for (int x = 2; x < filteredArgs.size(); x++) {
            if (!new File(filteredArgs.get(x)).exists()) {
                allExists = false;
                break;
            }
        }
        if (allExists) {
            addClassesGuessFqn(vmInfo, filteredArgs.subList(2, filteredArgs.size()));
        } else {
            if (Math.abs(filteredArgs.size() - 2/*--addclasses and puc*/) % 2 == 1) {
                throw new RuntimeException("Expected list of pairs (fqn file)^n - you have odd count");
            }
            addClassesEvenWithFqns(vmInfo, filteredArgs.subList(2, filteredArgs.size()));
        }
        return vmInfo;
    }

    private void addClassesGuessFqn(VmInfo vmInfo, List<String> files) throws IOException {
        List<FqnAndClassToJar> toJar = new ArrayList<>(files.size());
        for (int x = 0; x < files.size(); x++) {
            File f = new File(files.get(x));
            GetSetText r = new GetSetText.DummyGetSet("ok?");
            boolean b = new FileVerifier(new GetSetText.DummyGetSet(f.getAbsolutePath()), r).verifySource(null);
            if (!b) {
                throw new RuntimeException(f.getAbsolutePath() + " " + r.getText());
            }
            String fqn = Lib.readClassNameFromClass(Files.readAllBytes(f.toPath()));
            toJar.add(new FqnAndClassToJar(fqn, f));
        }
        addJar(vmInfo, toJar);
    }

    private void addClassesEvenWithFqns(VmInfo vmInfo, List<String> fqnAndFile) throws IOException {
        List<FqnAndClassToJar> toJar = new ArrayList<>(fqnAndFile.size() / 2);
        for (int x = 0; x < fqnAndFile.size(); x = x + 2) {
            String fqn = fqnAndFile.get(x);
            File f = new File(fqnAndFile.get(x + 1));
            GetSetText r = new GetSetText.DummyGetSet("ok?");
            boolean b = new FileVerifier(new GetSetText.DummyGetSet(f.getAbsolutePath()), r).verifySource(null);
            if (!b) {
                throw new RuntimeException(f.getAbsolutePath() + " " + r.getText());
            }
            toJar.add(new FqnAndClassToJar(fqn, f));
        }
        addJar(vmInfo, toJar);
    }

    private void addJar(VmInfo vmInfo, List<FqnAndClassToJar> toJar) throws IOException {
        System.out.println("Adding " + toJar.size() + " classes via jar to remote vm (" + Lib.getPrefixByBoot(isBoot) + ")");
        InMemoryJar jar = new InMemoryJar();
        jar.open();
        for (FqnAndClassToJar item : toJar) {
            GetSetText fakeInput = new GetSetText.DummyGetSet(item.getFile().getAbsolutePath());
            GetSetText fakeOutput = new GetSetText.DummyGetSet("ok?");
            boolean passed = new ClassVerifier(fakeInput, fakeOutput).verifySource(null);
            if (!passed) {
                throw new RuntimeException(item.getFile().getAbsolutePath() + " " + fakeOutput.getText());
            }
            jar.addFile(Files.readAllBytes(item.getFile().toPath()), item.getFqn());
        }
        jar.close();
        Lib.addJar(vmInfo, isBoot, "custom" + toJar.size() + "classes.jar", Base64.getEncoder().encodeToString(jar.toBytes()), vmManager);
    }
}
