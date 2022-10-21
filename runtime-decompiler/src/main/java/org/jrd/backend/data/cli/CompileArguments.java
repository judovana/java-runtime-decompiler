package org.jrd.backend.data.cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

final class CompileArguments {
    String wantedCustomCompiler;
    String puc;
    boolean isRecursive = false;
    List<File> filesToCompile = new ArrayList<>(1);
    private final PluginManager pluginManager;
    private final VmManager vmManager;
    private VmInfo vmInfo;

    CompileArguments(PluginManager pluginManager, VmManager vmManager, VmInfo vmInfo, String pluginCompiler) {
        this.pluginManager = pluginManager;
        this.vmManager = vmManager;
        this.wantedCustomCompiler = pluginCompiler;
        this.vmInfo = vmInfo;

    }

    @SuppressWarnings("ModifiedControlVariable")
    // shifting arguments when parsing
    CompileArguments(List<String> filteredArgs, PluginManager pluginManager, VmManager vmManager, boolean checkFile)
            throws FileNotFoundException {
        this.pluginManager = pluginManager;
        this.vmManager = vmManager;
        for (int i = 1; i < filteredArgs.size(); i++) {
            String arg = filteredArgs.get(i);

            if (CliSwitches.P.equals(arg)) {
                wantedCustomCompiler = filteredArgs.get(i + 1);
                i++; // shift
            } else if (CliSwitches.CP.equals(arg)) {
                puc = filteredArgs.get(i + 1);
                i++; // shift
            } else if (CliSwitches.R.equals(arg)) {
                isRecursive = true;
            } else {
                File fileToCompile = new File(arg);

                if (!fileToCompile.exists() && checkFile) {
                    throw new FileNotFoundException(fileToCompile.getAbsolutePath());
                }

                filesToCompile.add(fileToCompile);
            }
        }

        if (filesToCompile.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one file for compile.");
        }
    }

    public RuntimeCompilerConnector.JrdClassesProvider getClassesProvider() {
        VmInfo localVmInfo = this.vmInfo;
        if (localVmInfo == null) {
            localVmInfo = CliUtils.getVmInfo(puc, vmManager);
        }
        return new RuntimeCompilerConnector.JrdClassesProvider(localVmInfo, vmManager);
    }

    @SuppressFBWarnings(
            value = "NP_LOAD_OF_KNOWN_NULL_VALUE",
            justification = "Classpath is only used for FS VMs, in other cases getCp() does not get called"
    )
    public ClasspathlessCompiler getCompiler(boolean isVerbose, boolean acceptNonsenseAsDefault) {
        DecompilerWrapper decompiler = null;
        boolean hasCompiler = false;
        String compilerLogMessage = "Default runtime compiler will be used for overwrite.";

        if (wantedCustomCompiler != null) {
            decompiler = Lib.findDecompiler(wantedCustomCompiler, pluginManager);
            if (acceptNonsenseAsDefault && decompiler == null) {
                Logger.getLogger().log(compilerLogMessage);
                return OverwriteClassDialog.getClasspathlessCompiler(decompiler, hasCompiler, isVerbose);
            }

            if (pluginManager.hasBundledCompiler(decompiler)) {
                compilerLogMessage = wantedCustomCompiler + "'s bundled compiler will be used for overwrite.";
                hasCompiler = true;
            }
        }
        Logger.getLogger().log(compilerLogMessage);
        return OverwriteClassDialog.getClasspathlessCompiler(decompiler, hasCompiler, isVerbose);
    }
}
