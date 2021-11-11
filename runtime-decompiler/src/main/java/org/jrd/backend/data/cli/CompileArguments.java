package org.jrd.backend.data.cli;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class CompileArguments {
    String wantedCustomCompiler;
    String puc;
    boolean isRecursive = false;
    List<File> filesToCompile = new ArrayList<>(1);
    private final PluginManager pluginManager;
    private final VmManager vmManager;

    @SuppressWarnings("ModifiedControlVariable")
    // shifting arguments when parsing
    CompileArguments(List<String> filteredArgs, PluginManager pluginManager, VmManager vmManager) throws FileNotFoundException {
        this.pluginManager = pluginManager;
        this.vmManager = vmManager;
        for (int i = 1; i < filteredArgs.size(); i++) {
            String arg = filteredArgs.get(i);

            if (Cli.P.equals(arg)) {
                wantedCustomCompiler = filteredArgs.get(i + 1);
                i++; // shift
            } else if (Cli.CP.equals(arg)) {
                puc = filteredArgs.get(i + 1);
                i++; // shift
            } else if (Cli.R.equals(arg)) {
                isRecursive = true;
            } else {
                File fileToCompile = new File(arg);

                if (!fileToCompile.exists()) {
                    throw new FileNotFoundException(fileToCompile.getAbsolutePath());
                }

                filesToCompile.add(fileToCompile);
            }
        }

        if (filesToCompile.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one file for compile.");
        }
    }

    public ClassesProvider getClassesProvider() {
        if (puc == null) {
            return new ClassesProvider() {
                @Override
                public Collection<IdentifiedBytecode> getClass(ClassIdentifier... names) {
                    return new ArrayList<>();
                }

                @Override
                public List<String> getClassPathListing() {
                    return new ArrayList<>();
                }
            };
        } else {
            return new RuntimeCompilerConnector.JrdClassesProvider(Cli.getVmInfo(puc, vmManager), vmManager);
        }
    }

    public ClasspathlessCompiler getCompiler(boolean isVerbose) {
        DecompilerWrapper decompiler = null;
        boolean hasCompiler = false;
        String compilerLogMessage = "Default runtime compiler will be used for overwrite.";

        if (wantedCustomCompiler != null) {
            decompiler = Cli.findDecompiler(wantedCustomCompiler, pluginManager);

            if (pluginManager.hasBundledCompiler(decompiler)) {
                compilerLogMessage = wantedCustomCompiler + "'s bundled compiler will be used for overwrite.";
                hasCompiler = true;
            }
        }
        Logger.getLogger().log(compilerLogMessage);
        return OverwriteClassDialog.getClasspathlessCompiler(decompiler, hasCompiler, isVerbose);
    }
}
