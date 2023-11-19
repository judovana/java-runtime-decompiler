package org.jrd.backend.data.cli.workers;

import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.CliUtils;
import org.jrd.backend.data.cli.utils.CompileArguments;
import org.jrd.backend.data.cli.Lib;
import org.jrd.backend.data.cli.utils.PluginWrapperWithMetaInfo;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.CommonUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

public class Compile {

    private final List<String> filteredArgs;
    private final Saving saving;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private final boolean isHex;
    private final boolean isVerbose;
    private final String classloader;

    public Compile(
            boolean isHex, boolean isVerbose, List<String> filteredArgs, Saving saving, VmManager vmManager, PluginManager pluginManager,
            String classloader
    ) {
        this.filteredArgs = filteredArgs;
        this.saving = saving;
        this.vmManager = vmManager;
        this.pluginManager = pluginManager;
        this.isHex = isHex;
        this.isVerbose = isVerbose;
        this.classloader = classloader;
    }

    public void compileWrapper(List<VmInfo> operatedOn) throws Exception {
        VmInfo[] sourceTarget = compileAndUpload(new CompileArguments(filteredArgs, pluginManager, vmManager, true));
        if (sourceTarget[0] != null) {
            operatedOn.add(sourceTarget[0]);
        }
        if (sourceTarget[1] != null) {
            operatedOn.add(sourceTarget[1]);
        }
    }

    @SuppressWarnings(
        {"CyclomaticComplexity", "JavaNCSS", "ExecutableStatementCount", "VariableDeclarationUsageDistance", "AvoidNestedBlocks"}
    ) // todo refactor
    private VmInfo[] compileAndUpload(CompileArguments args) throws Exception {

        RuntimeCompilerConnector.JrdClassesProvider provider = args.getClassesProvider();
        boolean shouldUpload = shouldUpload();
        VmInfo targetVm = null;
        if (shouldUpload) {
            targetVm = CliUtils.getVmInfo(saving.getAs(), vmManager);
        }
        Map<Integer, List<IdentifiedSource>> sortedSources = new HashMap<>();
        {
            IdentifiedSource[] identifiedSources = CommonUtils.toIdentifiedSources(args.isRecursive(), args.getFilesToCompile());
            for (IdentifiedSource is : identifiedSources) {
                String fqn = is.getClassIdentifier().getFullName();
                Integer detectedByteCode = null;
                //FIRST try to find the class in target vm if enabled
                if (shouldUpload) {
                    try {
                        int[] versions = Lib.getByteCodeVersions(new ClassInfo(fqn), targetVm, vmManager, Optional.ofNullable(classloader));
                        detectedByteCode = versions[1];
                        Logger.getLogger().log(Logger.Level.ALL, fqn + " - detected bytecode in target VM: " + detectedByteCode);
                    } catch (Exception ex) {
                        System.out.println(""); //findbugs issue
                    }
                }
                if (detectedByteCode == null) {
                    //FALLBACK try to find the class in target vm in source
                    try {
                        int[] versions = Lib.getByteCodeVersions(
                                new ClassInfo(fqn), args.getClassesProvider().getVmInfo(), vmManager, Optional.ofNullable(classloader)
                        );
                        detectedByteCode = versions[1]; /**/
                        Logger.getLogger().log(Logger.Level.ALL, fqn + " - detected bytecode in source VM: " + detectedByteCode);
                    } catch (Exception ex) {
                        Logger.getLogger().log(ex);
                    }
                }
                if (detectedByteCode == null) {
                    if (targetVm != null) {
                        detectedByteCode = Lib.getDefaultRemoteBytecodelevelCatched(targetVm, vmManager);
                    }
                    if (detectedByteCode == null) {
                        try {
                            String randomClass = Lib
                                    .obtainClasses(args.getClassesProvider().getVmInfo(), vmManager, Optional.ofNullable(classloader))[0];
                            detectedByteCode = Lib.getRemoteBytecodelevel(
                                    args.getClassesProvider().getVmInfo(), vmManager, randomClass, Optional.ofNullable(classloader)
                            );
                        } catch (Exception ex) {
                            Logger.getLogger().log(ex);
                        }
                    }
                    Logger.getLogger().log(
                            Logger.Level.ALL,
                            fqn + " - failed to detect bytecode level. Fallback to " +
                                    (detectedByteCode == null ? "nothing" : detectedByteCode)
                    );
                }
                if (!Config.getConfig().doOverwriteST()) {
                    Logger.getLogger().log(Logger.Level.ALL, " * Overwrite of source/target turned off! Resetting to defaults *");
                    detectedByteCode = null;
                }
                List<IdentifiedSource> sources = sortedSources.get(detectedByteCode);
                if (sources == null) {
                    sources = new ArrayList<>();
                    sortedSources.put(detectedByteCode, sources);
                }
                sources.add(is);
            }
        }
        PluginWrapperWithMetaInfo wrapper = Lib.getPluginWrapper(pluginManager, args.getWantedCustomCompiler(), true);
        List<IdentifiedBytecode> allBytecode = new ArrayList<>();
        for (Map.Entry<Integer, List<IdentifiedSource>> entry : sortedSources.entrySet()) {
            Integer detectedByteCode = entry.getKey();
            IdentifiedSource[] identifiedSources = entry.getValue().toArray(new IdentifiedSource[0]);
            allBytecode.addAll(compile(args, identifiedSources, wrapper, detectedByteCode, System.err, isVerbose));
        }

        if (shouldUpload) {
            int failCount = 0;
            for (IdentifiedBytecode bytecode : allBytecode) {
                String className = bytecode.getClassIdentifier().getFullName();
                Logger.getLogger().log("Uploading class '" + className + "'.");

                String response = Lib.uploadClass(
                        targetVm, className, Base64.getEncoder().encodeToString(bytecode.getFile()), vmManager,
                        Optional.ofNullable(classloader)
                );

                if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
                    Logger.getLogger().log(Logger.Level.ALL, "Successfully uploaded class '" + className + "'.");
                } else {
                    failCount++;
                    Logger.getLogger().log(Logger.Level.ALL, "Failed to upload class '" + className + "'.");
                }
            }

            if (failCount > 0) {
                throw new RuntimeException("Failed to upload " + failCount + " classes out of " + allBytecode.size() + " total.");
            } else {
                Logger.getLogger().log("Successfully uploaded all " + allBytecode.size() + " classes.");
            }
        } else {
            if (!saving.shouldSave() && allBytecode.size() > 1) {
                throw new IllegalArgumentException(
                        "Unable to print multiple classes to stdout. Either use saving modifiers or compile one class at a time."
                );
            }

            for (IdentifiedBytecode bytecode : allBytecode) {
                new Shared(isHex, saving).outOrSave(bytecode.getClassIdentifier().getFullName(), ".class", bytecode.getFile(), true);
            }
        }
        if (provider.getVmInfo().equals(targetVm)) {
            return new VmInfo[]{provider.getVmInfo(), null};
        } else {
            return new VmInfo[]{provider.getVmInfo(), targetVm};
        }
    }

    public static Collection<IdentifiedBytecode> compile(
            CompileArguments args, IdentifiedSource[] identifiedSources, PluginWrapperWithMetaInfo wrapper, Integer detectedByteCode,
            PrintStream outerr, boolean isVerbose
    ) {
        String m = "Compiling group of files " + identifiedSources.length + " of level : ";
        if (detectedByteCode == null) {
            m = m + "default (" + Config.getConfig().getCompilerArgsString() + ")";
        } else {
            m = m + detectedByteCode;
        }
        m = m + " - " + Arrays.stream(identifiedSources).map(a -> a.getClassIdentifier().getFullName()).collect(Collectors.joining(", "));
        outerr.println(m);
        Config.getConfig().setBestSourceTarget(Optional.ofNullable(detectedByteCode));
        ClasspathlessCompiler compiler = OverwriteClassDialog.getClasspathlessCompiler(
                wrapper.getWrapper() == null ? null : wrapper.getWrapper().getDecompiler(), wrapper.haveCompiler(), isVerbose
        );
        Collection<IdentifiedBytecode> compiledFiles = compiler.compileClass(
                args.getClassesProvider(), Optional.of((level, message) -> Logger.getLogger().log(message)), identifiedSources
        );
        String mm = "Compiled " + identifiedSources.length + " sources to " + compiledFiles.size() + " files: " +
                compiledFiles.stream().map(a -> a.getClassIdentifier().getFullName()).collect(Collectors.joining(", "));
        outerr.println(mm);

        return compiledFiles;
    }

    private boolean shouldUpload() {
        return shouldUpload(saving);
    }

    public static boolean shouldUpload(Saving saving) {
        if (saving.shouldSave()) {
            try {
                VmInfo.Type t = CliUtils.guessType(saving.getAs());
                if (t == VmInfo.Type.LOCAL || t == VmInfo.Type.REMOTE) {
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
            }
        }
        return false;
    }
}
