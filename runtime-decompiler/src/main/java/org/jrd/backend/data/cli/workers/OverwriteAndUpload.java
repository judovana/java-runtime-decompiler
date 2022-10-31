package org.jrd.backend.data.cli.workers;

import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.Help;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.ReceivedType;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.decompilerview.verifiers.GetSetText;
import org.jrd.frontend.frame.main.decompilerview.verifiers.JarVerifier;
import org.jrd.frontend.frame.overwrite.FileToClassValidator;

import java.io.File;
import java.util.List;

public class OverwriteAndUpload {

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final boolean isBoot;
    private final boolean isHex;

    public OverwriteAndUpload(List<String> filteredArgs, VmManager vmManager, boolean isBoot, boolean isHex) {
        this.filteredArgs = filteredArgs;
        this.vmManager = vmManager;
        this.isBoot = isBoot;
        this.isHex = isHex;
    }

    //fixme, refactor, jar do not belongs here
    @SuppressWarnings({"CyclomaticComplexity"})
    public VmInfo overwrite(ReceivedType add) throws Exception {
        int maxargs = 4;
        if (add.equals(ReceivedType.ADD_JAR)) {
            Logger.getLogger().log(Logger.Level.ALL, "adding jar to " + Lib.getPrefixByBoot(isBoot));
            maxargs = 3;
        }
        String newBytecodeFile;
        if (filteredArgs.size() == (maxargs - 1)) {
            Logger.getLogger().log("Reading  from stdin.");
            newBytecodeFile = null;
        } else {
            if (filteredArgs.size() != maxargs) {
                switch (add) {
                    case OVERWRITE_CLASS:
                        throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.OVERWRITE_FORMAT + "'.");
                    case ADD_CLASS:
                        throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.ADD_CLASS_FORMAT + "'.");
                    case ADD_JAR:
                        throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.ADD_JAR_FORMAT + "'.");
                    default:
                        throw new RuntimeException("Unknown action " + add);
                }
            }
            if (add.equals(ReceivedType.ADD_JAR)) {
                newBytecodeFile = filteredArgs.get(2);
            } else {
                newBytecodeFile = filteredArgs.get(3);
            }
        }

        String className;
        if (add.equals(ReceivedType.ADD_JAR)) {
            className = newBytecodeFile == null ? "stdin.jar" : new File(newBytecodeFile).getName();
        } else {
            className = filteredArgs.get(2);
        }
        VmInfo vmInfo = CliUtils.getVmInfo(filteredArgs.get(1), vmManager);
        String clazz;

        if (newBytecodeFile == null) {
            clazz = DecompilationController.stdinToBase64(isHex);
        } else { // validate first
            if (!add.equals(ReceivedType.ADD_JAR)) {
                FileToClassValidator.StringAndScore r = FileToClassValidator.validate(className, newBytecodeFile);

                if (r.getScore() > 0 && r.getScore() < 10) {
                    Logger.getLogger().log(Logger.Level.ALL, "WARNING: " + r.getMessage());
                }
                if (r.getScore() >= 10) {
                    Logger.getLogger().log(Logger.Level.ALL, "ERROR: " + r.getMessage());
                }
            } else {
                GetSetText fakeInput = new GetSetText.DummyGetSet(newBytecodeFile);
                GetSetText fakeOutput = new GetSetText.DummyGetSet("ok?");
                boolean passed = new JarVerifier(fakeInput, fakeOutput).verifySource(null);
                if (!passed) {
                    throw new RuntimeException(fakeOutput.getText());
                }
            }
            clazz = DecompilationController.fileToBase64(newBytecodeFile, isHex);
        }

        String response;
        switch (add) {
            case OVERWRITE_CLASS:
                response = Lib.uploadClass(vmInfo, className, clazz, vmManager);
                break;
            case ADD_CLASS:
                response = Lib.addClass(vmInfo, className, clazz, vmManager);
                break;
            case ADD_JAR:
                response = Lib.addJar(vmInfo, isBoot, className, clazz, vmManager);
                break;
            default:
                throw new RuntimeException("Unknown action " + add);
        }

        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            switch (add) {
                case OVERWRITE_CLASS:
                    System.out.println("Overwrite of class '" + className + "' successful.");
                    break;
                case ADD_CLASS:
                    System.out.println("Addition of class '" + className + "' successful.");
                    break;
                case ADD_JAR:
                    System.out.println("Addition of jar '" + className + "' successful.");
                    break;
                default:
                    throw new RuntimeException("Unknown action " + add);
            }
        } else {
            throw new RuntimeException(DecompilationController.CLASSES_NOPE);
        }
        return vmInfo;
    }

}
