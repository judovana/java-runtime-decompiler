package org.jrd.backend.data.cli;

import com.github.difflib.patch.PatchFailedException;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

import org.jrd.backend.communication.ErrorCandidate;
import org.jrd.backend.communication.FsAgent;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentLoader;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.Model;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.workers.AddClasses;
import org.jrd.backend.data.cli.workers.Api;
import org.jrd.backend.data.cli.workers.AttachDetach;
import org.jrd.backend.data.cli.workers.Classes;
import org.jrd.backend.data.cli.workers.Decompile;
import org.jrd.backend.data.cli.workers.InitClass;
import org.jrd.backend.data.cli.workers.ListAgents;
import org.jrd.backend.data.cli.workers.ListJvms;
import org.jrd.backend.data.cli.workers.ListPlugins;
import org.jrd.backend.data.cli.workers.Overrides;
import org.jrd.backend.data.cli.workers.OverwriteAndUpload;
import org.jrd.backend.data.cli.workers.PrintBytes;
import org.jrd.backend.data.cli.workers.Shared;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.frame.main.popup.DiffPopup;
import org.jrd.frontend.frame.main.popup.SingleFilePatch;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.CommonUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jrd.backend.data.cli.CliSwitches.*;

public class Cli {

    private final List<String> filteredArgs;
    private final VmManager vmManager;
    private final PluginManager pluginManager;
    private Saving saving;
    private boolean isVerbose;
    private boolean isHex;
    private boolean isRevert;
    private boolean isBoot;

    public Cli(String[] orig, Model model) {
        this.filteredArgs = prefilterArgs(orig);
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();
    }

    public boolean shouldBeVerbose() {
        return isVerbose;
    }

    @SuppressWarnings({"UnnecessaryParentheses"})
    public boolean isGui() {
        return filteredArgs.isEmpty() || (isHex && (CliSwitches.noMatch(filteredArgs)));
    }

    public boolean isHex() {
        return isHex;
    }

    @SuppressWarnings("ModifiedControlVariable") // shifting arguments when parsing
    private List<String> prefilterArgs(String[] originalArgs) {
        List<String> args = new ArrayList<>(originalArgs.length);
        String saveAs = null;
        String saveLike = null;
        List<String> agentArgs = new ArrayList<>();

        for (int i = 0; i < originalArgs.length; i++) {
            String arg = originalArgs[i];
            String cleanedArg = CliUtils.cleanParameter(arg);

            if (cleanedArg.equals(VERBOSE)) {
                isVerbose = true;
                Logger.getLogger().setVerbose(true);
            } else if (cleanedArg.equals(BOOT_CLASS_LOADER)) {
                isBoot = true;
            } else if (cleanedArg.equals(HEX)) {
                isHex = true;
            } else if (cleanedArg.equals(REVERT)) {
                isRevert = true;
            } else if (cleanedArg.equals(SAVE_AS)) {
                saveAs = originalArgs[i + 1];
                i++;
            } else if (cleanedArg.equals(SAVE_LIKE)) {
                saveLike = originalArgs[i + 1];
                i++;
            } else if (cleanedArg.equals(AGENT)) {
                i = readAgentParams(originalArgs, agentArgs, i);
            } else {
                args.add(arg);
            }
        }
        this.saving = new Saving(saveAs, saveLike);
        setDefaultAgentConfig(args, agentArgs);
        if (!agentArgs.isEmpty() && args.isEmpty()) {
            throw new RuntimeException("It is not allowed to set " + AGENT + " in gui mode");
        }
        return args;
    }

    @SuppressWarnings("indentation") //conflict of checkstyle and formatter plugins
    private void setDefaultAgentConfig(List<String> args, List<String> agentArgs) {
        AgentLoader
                .setDefaultConfig(
                        AgentConfig.create(
                                agentArgs,
                                args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(OVERWRITE)) ||
                                        args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(ATTACH)) ||
                                        args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(PATCH)) ||
                                        (args.stream().map(a -> CliUtils.cleanParameter(a))
                                                .anyMatch(a -> a.equals(COMPILE) && shouldUpload()))
                        )
                );
    }

    private int readAgentParams(String[] originalArgs, List<String> agentArgs, int i) {
        if (!agentArgs.isEmpty()) {
            throw new RuntimeException("You had set second " + AGENT + ". Not allowed.");
        }
        while (true) {
            if (i == originalArgs.length - 1) {
                if (agentArgs.isEmpty()) {
                    throw new RuntimeException(
                            AGENT + " should have at least one parameter otherwise it is nonsense. Use: " + Help.AGENT_FORMAT
                    );
                } else {
                    break;
                }
            }
            String agentArg = originalArgs[i + 1];
            if (agentArg.startsWith("-")) {
                if (agentArgs.isEmpty()) {
                    throw new RuntimeException(
                            AGENT + " should have at least one parameter otherwise it is nonsense. Use: " + Help.AGENT_FORMAT
                    );
                } else {
                    break;
                }
            } else {
                agentArgs.add(agentArg);
                i++;
            }
        }
        return i;
    }

    @SuppressWarnings({"CyclomaticComplexity", "JavaNCSS", "ExecutableStatementCount", "MethodLength"})
    // un-refactorable
    public void consumeCli() throws Exception {
        if (filteredArgs.isEmpty()) { // impossible in org.jrd.backend.Main#Main() control flow, but possible in tests
            return;
        }
        final String operation = CliUtils.cleanParameter(filteredArgs.get(0));
        final List<VmInfo> operatedOn = new ArrayList<>(2);
        try {
            switch (operation) {
                case LIST_JVMS:
                    new ListJvms(filteredArgs, saving, vmManager).listJvms();
                    break;
                case LIST_PLUGINS:
                    new ListPlugins(filteredArgs, saving, pluginManager).listPlugins();
                    break;
                case LIST_AGENTS:
                    List<VmInfo> vmInfos = new ListAgents(isVerbose, vmManager).listAgents(filteredArgs);
                    operatedOn.addAll(vmInfos);
                    break;
                case LIST_OVERRIDES:
                    VmInfo vmInfo0 = new Overrides(filteredArgs, vmManager).listOverrides();
                    operatedOn.add(vmInfo0);
                    break;
                case REMOVE_OVERRIDES:
                    VmInfo vmInfo00 = new Overrides(filteredArgs, vmManager).removeOverrides();
                    operatedOn.add(vmInfo00);
                    break;
                case SEARCH:
                    VmInfo vmInfoSearch = new Classes(filteredArgs, vmManager, isHex, saving).searchClasses();
                    operatedOn.add(vmInfoSearch);
                    break;
                case LIST_CLASSES:
                    VmInfo vmInfo1 = new Classes(filteredArgs, vmManager, isHex, saving).listClasses(false, false, Optional.empty());
                    operatedOn.add(vmInfo1);
                    break;
                case LIST_CLASSESDETAILS:
                    VmInfo vmInfo2 = new Classes(filteredArgs, vmManager, isHex, saving).listClasses(true, false, Optional.empty());
                    operatedOn.add(vmInfo2);
                    break;
                case LIST_CLASSESBYTECODEVERSIONS:
                    VmInfo vmInfo11 = new Classes(filteredArgs, vmManager, isHex, saving).listClasses(false, true, Optional.empty());
                    operatedOn.add(vmInfo11);
                    break;
                case LIST_CLASSESDETAILSBYTECODEVERSIONS:
                    VmInfo vmInfo21 = new Classes(filteredArgs, vmManager, isHex, saving).listClasses(true, true, Optional.empty());
                    operatedOn.add(vmInfo21);
                    break;
                case BYTES:
                case BASE64:
                case DEPS:
                    VmInfo vmInfo3 = new PrintBytes(isHex, filteredArgs, saving, vmManager, pluginManager).printBytes(operation);
                    operatedOn.add(vmInfo3);
                    break;
                case DECOMPILE:
                    VmInfo vmInfo4 = new Decompile(isHex, filteredArgs, saving, vmManager, pluginManager).decompile();
                    operatedOn.add(vmInfo4);
                    break;
                case COMPILE:
                    compileWrapper(operatedOn);
                    break;
                case OVERWRITE:
                    VmInfo vmInfo5 = new OverwriteAndUpload(filteredArgs, vmManager, isBoot, isHex).overwrite(ReceivedType.OVERWRITE_CLASS);
                    operatedOn.add(vmInfo5);
                    break;
                case PATCH:
                    VmInfo patchVmInfo = patch();
                    operatedOn.add(patchVmInfo);
                    break;
                case ADD_CLASS:
                    VmInfo vmInfoAddClass =
                            new OverwriteAndUpload(filteredArgs, vmManager, isBoot, isHex).overwrite(ReceivedType.ADD_CLASS);
                    operatedOn.add(vmInfoAddClass);
                    break;
                case ADD_JAR:
                    VmInfo vmInfoAddJar = new OverwriteAndUpload(filteredArgs, vmManager, isBoot, isHex).overwrite(ReceivedType.ADD_JAR);
                    operatedOn.add(vmInfoAddJar);
                    break;
                case ADD_CLASSES:
                    VmInfo vmInfoAddClasses = new AddClasses(filteredArgs, vmManager, isBoot).addClasses();
                    operatedOn.add(vmInfoAddClasses);
                    break;
                case INIT:
                    VmInfo vmInfo6 = new InitClass(filteredArgs, vmManager).init();
                    operatedOn.add(vmInfo6);
                    break;
                case ATTACH:
                    VmInfo vmInfo7 = new AttachDetach(filteredArgs, vmManager).attach();
                    operatedOn.add(vmInfo7);
                    break;
                case DETACH:
                    new AttachDetach(filteredArgs, vmManager).detach();
                    break;
                case API:
                    VmInfo vmInfo8 = new Api(filteredArgs, saving, vmManager, pluginManager).api();
                    operatedOn.add(vmInfo8);
                    break;
                case HELP:
                case H:
                    printHelp();
                    break;
                case CONFIG:
                    printConfig();
                    break;
                case VERSION:
                    printVersion();
                    break;
                default:
                    printHelp();
                    throw new IllegalArgumentException("Unknown commandline argument '" + operation + "'.");
            }
        } finally {
            boolean localAgent = false;
            for (VmInfo status : operatedOn) {
                if (status.getType() == VmInfo.Type.LOCAL && !status.getVmDecompilerStatus().isReused()) {
                    localAgent = true;
                }
            }
            if (AgentLoader.getDefaultConfig().liveliness == AgentLiveliness.SESSION) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        for (VmInfo status : operatedOn) {
                            if (status.getType() == VmInfo.Type.LOCAL && !status.getVmDecompilerStatus().isReused()) {
                                AttachDetach.detachLocalhost(status.getVmDecompilerStatus().getListenPort(), vmManager);
                            }
                        }
                    }
                });
                if (localAgent) {
                    System.err.println(
                            " kill this process (" + ProcessHandle.current().pid() + ") to detach agent(s) on port: " +
                                    operatedOn.stream().map(a -> a.getVmDecompilerStatus().getListenPort() + "")
                                            .collect(Collectors.joining(","))
                    );
                    while (true) {
                        Thread.sleep(1000);
                    }
                }
            } else if (AgentLoader.getDefaultConfig().liveliness == AgentLiveliness.ONE_SHOT) {
                for (VmInfo status : operatedOn) {
                    if (operation.equals(ATTACH)) {
                        System.err.println("agent was just attached.. and is detaching right away. Weird, yah?");
                    } else {
                        Logger.getLogger().log("Detaching single attach agent(s), if any");
                    }
                    if (status.getType() == VmInfo.Type.LOCAL) {
                        AttachDetach.detachLocalhost(status.getVmDecompilerStatus().getListenPort(), vmManager);
                    }
                }
            } else {
                for (VmInfo status : operatedOn) {
                    if (localAgent) {
                        System.err.println(
                                "agent is permanently attached to " + status.getVmPid() + " on port " +
                                        status.getVmDecompilerStatus().getListenPort()
                        );
                    }
                }
                if (operation.equals(ATTACH) || operation.equals(DETACH) || isVerbose) {
                    System.err.println("exiting");
                }
            }
        }
    }

    private void compileWrapper(List<VmInfo> operatedOn) throws Exception {
        VmInfo[] sourceTarget = compileAndUpload(new CompileArguments(filteredArgs, pluginManager, vmManager, true));
        if (sourceTarget[0] != null) {
            operatedOn.add(sourceTarget[0]);
        }
        if (sourceTarget[1] != null) {
            operatedOn.add(sourceTarget[1]);
        }
    }

    private void printConfig() throws IOException {
        if (isVerbose) {
            System.out.println(Files.readString(Config.getConfig().getConfFile().toPath()));
        } else {
            System.out.println(Config.getConfig().getConfFile().getAbsolutePath());
        }
    }

    /* FIXME refactor
     * The refactoring should be simple, there are obvious parts like check all , init all, gather all, compile all, upload all...
     */
    @SuppressWarnings({"MethodLength", "CyclomaticComplexity", "ExecutableStatementCount", "JavaNCSS", "UnnecessaryParentheses"})
    private VmInfo patch() throws Exception {
        //--patch <puc>  ((plugin)xor(SP/CP)( (-hex) (-R) < patch
        String puc;
        VmInfo vmInfo;
        String pluginXorPath;
        if (isHex) {
            if (filteredArgs.size() == 2) {
                puc = filteredArgs.get(1);
                vmInfo = getVmInfo(puc);
                pluginXorPath = "nothingNowhere";
            } else if (filteredArgs.size() == 3) {
                puc = filteredArgs.get(1);
                vmInfo = getVmInfo(puc);
                pluginXorPath = filteredArgs.get(2);
            } else {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.PATCH_FORMAT + "'.");
            }
        } else {
            if (filteredArgs.size() != 3) {
                throw new IllegalArgumentException("Incorrect argument count! Please use '" + Help.PATCH_FORMAT + "'.");
            }
            puc = filteredArgs.get(1);
            vmInfo = getVmInfo(puc);
            pluginXorPath = filteredArgs.get(2);
        }
        List<String> patch = DecompilationController.stdinToStrings();
        List<SingleFilePatch> files = DiffPopup.getIndividualPatches(patch);
        for (SingleFilePatch startEnd : files) {
            /**
             * FIXME, add support for:
             * --- /dev/null
             * +++ b/runtime-deco
             * (or in revert:
             * --- a/runtime-deco
             * +++ /dev/null
             * )
             * via addClasses
             * FIXME, State clearly that
             * --- a/runtime-deco
             * +++ /dev/null
             * (or in revert
             * --- /dev/null
             * +++ b/runtime-deco
             * )
             * can not be supported
             *
             * todo, compile the +++ b/runtime-deco
             * with biggest group? By default?
             * new agent api to get more info from runniong vm?
             */
            String header1 = patch.get(startEnd.getStart());
            String header2 = patch.get(startEnd.getStart() + 1);
            String className = DiffPopup.parseClassFromHeader(header1);
            String classNameCheck = DiffPopup.parseClassFromHeader(header2);
            if (!className.equals(classNameCheck)) {
                if ((!isRevert && DiffPopup.isRemoveFile(header1) && DiffPopup.isAddDevNull(header2)) ||
                        (isRevert && DiffPopup.isAddFile(header2) && DiffPopup.isRemoveDevNull(header1))) {
                    throw new RuntimeException(
                            "Removal of files can not be supported. Jvm can not unload: :\n" + patch.get(startEnd.getStart()) + "\n" +
                                    patch.get(startEnd.getStart() + 1) + (isRevert ? "\nrevert on" : "")
                    );
                }
                throw new RuntimeException(
                        "Invalid file header for:\n" + patch.get(startEnd.getStart()) + "\n" + patch.get(startEnd.getStart() + 1)
                );
            }
            Lib.initClass(vmInfo, vmManager, className, System.out);
        }

        FsAgent initialSearch;
        if (isHex) {
            initialSearch = FsAgent.createAdditionalClassPathFsAgent(pluginXorPath);
        } else {
            initialSearch = FsAgent.createAdditionalSourcePathFsAgent(pluginXorPath);
        }

        List<String> local = new ArrayList<>();
        List<String> remote = new ArrayList<>();

        List<ObtainedCodeWithNameAndBytecode> obtainedCodeWithNameAndBytecode = new ArrayList<>(files.size());
        Map<String, Integer> bytecodeLevelCache = new HashMap<>();
        for (SingleFilePatch startEnd : files) {
            String className = DiffPopup.parseClassFromHeader(patch.get(startEnd.getStart()));
            System.out.println("Obtaining " + className);
            Integer byteCodeLevel = null;
            String base64Bytes = initialSearch.submitRequest(AgentRequestAction.RequestAction.BYTES + " " + className);
            ErrorCandidate errorCandidateLocal = new ErrorCandidate(base64Bytes);
            if (errorCandidateLocal.isError()) {
                //not found on additional cp/sp: getting from puc
                base64Bytes = Lib.obtainClass(vmInfo, className, vmManager).getLoadedClassBytes();
                ErrorCandidate errorCandidateRemote = new ErrorCandidate(base64Bytes);
                if (errorCandidateRemote.isError()) {
                    throw new RuntimeException(className + " not found on local nor remote paths/vm"); //not probable, see the init check above
                }
                byteCodeLevel = Lib.getBuildJavaPerVersion(Base64.getDecoder().decode(base64Bytes));
                remote.add(className);
                if (isHex) {
                    //we are done, we have just  by obtainClasses
                    System.out.println("...remote binary");
                } else {
                    String decompiledSrc = decompileBytesByDecompilerName(base64Bytes, pluginXorPath, className, vmInfo);
                    base64Bytes = Base64.getEncoder().encodeToString(decompiledSrc.getBytes(StandardCharsets.UTF_8));
                    System.out.println("...remote decompiled src ( compiled as " + byteCodeLevel + ")");
                }
            } else {
                local.add(className);
                if (isHex) {
                    //we are done, base64 bytes are already loaded by initialSearch
                    System.out.println("...local binary");
                } else {
                    //we are done also here, it was found on SRC path (and is base64 as it is found by agent)
                    //but wee need to find how it was compiled
                    String remoteImpl = Lib.obtainClass(vmInfo, className, vmManager).getLoadedClassBytes();
                    ErrorCandidate errorCandidateRemote = new ErrorCandidate(remoteImpl);
                    if (errorCandidateRemote.isError()) {
                        throw new RuntimeException(className + " not found on local nor remote paths/vm"); //not probable, see the init check above
                    }
                    byteCodeLevel = Lib.getBuildJavaPerVersion(Base64.getDecoder().decode(remoteImpl));
                    System.out.println("...local src ( compiled as " + byteCodeLevel + ")");
                }
            }
            obtainedCodeWithNameAndBytecode.add(new ObtainedCodeWithNameAndBytecode(className, base64Bytes, byteCodeLevel));
            bytecodeLevelCache.put(className, byteCodeLevel);
        }
        if (obtainedCodeWithNameAndBytecode.size() == remote.size()) {
            System.out.println("Warning! All classes found only in remote vm!");
        } else if (obtainedCodeWithNameAndBytecode.size() == local.size()) {
            System.out.println("All classes found on local path");
        } else if (obtainedCodeWithNameAndBytecode.size() == (local.size() + remote.size())) {
            //this is theoretical, current input/output do nto allow inclusion of decompielr and src path together... maybe improvable later
            System.out.println("WARNING! Some (" + local.size() + ") classes found in local, some (" + remote.size() + ") in remote");
        } else {
            throw new RuntimeException(" Only " + (local.size() + remote.size()) + " from " + files.size() + " found!");
        }

        //Dont forget, if one file is patched more times, then it is expected, that it is applied to the already patched!
        Map<String, String> patched = new HashMap(files.size());
        for (int i = 0; i < files.size(); i++) {
            SingleFilePatch startEnd = files.get(i);
            String className = DiffPopup.parseClassFromHeader(patch.get(startEnd.getStart()));
            ObtainedCodeWithNameAndBytecode nameBody = obtainedCodeWithNameAndBytecode.get(i);
            if (!nameBody.getName().equals(className)) {
                throw new RuntimeException("Misaligned patching of " + nameBody.getName() + " by " + className);
            }
            System.out.println("Patching " + nameBody.getName());
            String toPatch = nameBody.getBase64Body();
            if (patched.containsKey(className)) {
                toPatch = patched.get(className);
            }
            if (isHex) {
                byte[] bytes = Base64.getDecoder().decode(toPatch);
                List<String> hexLines = HexWithControls.bytesToStrings(bytes);
                if (isVerbose) {
                    System.out.println("-  " + hexLines.stream().collect(Collectors.joining("\n-  ")));
                }
                List<String> patchedLines = applySubPatch(patch, startEnd, hexLines);
                if (isVerbose) {
                    System.out.println("+  " + patchedLines.stream().collect(Collectors.joining("\n+  ")));
                }
                String patchedBase64 =
                        Base64.getEncoder().encodeToString(HexWithControls.hexToBytes(HexWithControls.hexLinesToHexString(patchedLines)));
                patched.put(className, patchedBase64);
                System.out.println("Patched bin");
            } else {
                String src = new String(Base64.getDecoder().decode(toPatch), StandardCharsets.UTF_8); //change to src
                List<String> srcLines = Arrays.asList(src.split("\n"));
                if (isVerbose) {
                    System.out.println("-  " + srcLines.stream().collect(Collectors.joining("\n-  ")));
                }
                List<String> patchedLines = applySubPatch(patch, startEnd, srcLines);
                if (isVerbose) {
                    System.out.println("+  " + patchedLines.stream().collect(Collectors.joining("\n+  ")));
                }
                String patchedBase64 = Base64.getEncoder()
                        .encodeToString(patchedLines.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
                patched.put(className, patchedBase64);
                System.out.println("Patched src");
            }
        }

        //in addition, we have to different by bytecode version (although it is src only)
        Map<Integer, Map<String, String>> binariesToUpload = new HashMap<>(files.size());
        if (isHex) {
            BytecodeSorter bt = new BytecodeSorter.HexDummySorter(patched.entrySet());
            binariesToUpload = bt.sort();
        } else {
            System.out.println("Compiling patched sources.");
            CompileArguments args = new CompileArguments(pluginManager, vmManager, vmInfo, pluginXorPath);
            PluginWrapperWithMetaInfo wrapper = Lib.getPluginWrapper(pluginManager, pluginXorPath, false);
            List<Map.Entry<String, String>> patchedList = new ArrayList<>(patched.entrySet());
            for (Integer detectedByteCode : new HashSet<>(bytecodeLevelCache.values())) {
                List<IdentifiedSource> thisBytecodeClasses = patchedList.stream().filter(
                        a -> null == bytecodeLevelCache.get(a.getKey()) || detectedByteCode.equals(bytecodeLevelCache.get(a.getKey()))
                ).map(a -> new IdentifiedSource(new ClassIdentifier(a.getKey()), Base64.getDecoder().decode(a.getValue())))
                        .collect(Collectors.toList());
                //ternary operator is constantly killed by autoformater

                Map<String, String> compiledForSingleBytecodeLevel = new HashMap<>();
                Collection<IdentifiedBytecode> compiledFiles =
                        compile(args, thisBytecodeClasses.toArray(IdentifiedSource[]::new), wrapper, detectedByteCode, System.out);
                for (IdentifiedBytecode compiled : compiledFiles) {
                    compiledForSingleBytecodeLevel
                            .put(compiled.getClassIdentifier().getFullName(), Base64.getEncoder().encodeToString(compiled.getFile()));
                }
                binariesToUpload.put(detectedByteCode, compiledForSingleBytecodeLevel);
            }
        }

        List<String> failures = new ArrayList<>();
        List<String> passes = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, String>> toUploadWithBytecode : binariesToUpload.entrySet()) {
            Integer bytecodeLevel = toUploadWithBytecode.getKey();
            System.out.println("Upload group of bytecode level: " + (bytecodeLevel == null ? "default:" : "" + bytecodeLevel));
            for (Map.Entry<String, String> toUpload : toUploadWithBytecode.getValue().entrySet()) {
                System.out.println("Uploading: " + toUpload.getKey());
                String reply = Lib.uploadClass(vmInfo, toUpload.getKey(), toUpload.getValue(), vmManager);
                ErrorCandidate ec = new ErrorCandidate(reply);
                if (ec.isError() || reply.startsWith("error ")/*fix me, why the or is needed?*/) {
                    System.out.println("failed - " + reply.replaceAll("for request 'OVERWRITE.*", ""));
                    failures.add(toUpload.getKey());
                } else {
                    System.out.println("Uploaded.");
                    passes.add(toUpload.getKey());
                }
            }
        }
        if (failures.isEmpty()) {
            System.out.println("All looks good");
        } else {
            System.out.println("Failed to upload: " + failures.stream().collect(Collectors.joining(", ")));
            throw new RuntimeException("Failed to upload " + failures.size() + " classes from " + (passes.size() + failures.size()) + "");
        }
        return vmInfo;
    }

    private List<String> applySubPatch(List<String> patch, SingleFilePatch startEnd, List<String> linesToPatch)
            throws PatchFailedException {
        List<String> subPatch = patch.subList(startEnd.getStart(), startEnd.getEnd() + 1);
        List<String> patchedLines = DiffPopup.patch(linesToPatch, subPatch, isRevert);
        return patchedLines;
    }

    private String decompileBytesByDecompilerName(String base64Bytes, String pluginName, String className, VmInfo vmInfo) throws Exception {
        return Lib.decompileBytesByDecompilerName(base64Bytes, pluginName, className, vmInfo, vmManager, pluginManager);
    }

    @SuppressWarnings(
        {"CyclomaticComplexity", "JavaNCSS", "ExecutableStatementCount", "VariableDeclarationUsageDistance", "AvoidNestedBlocks"}
    ) // todo refactor
    private VmInfo[] compileAndUpload(CompileArguments args) throws Exception {

        RuntimeCompilerConnector.JrdClassesProvider provider = args.getClassesProvider();
        boolean shouldUpload = shouldUpload();
        VmInfo targetVm = null;
        if (shouldUpload) {
            targetVm = getVmInfo(saving.getAs());
        }
        Map<Integer, List<IdentifiedSource>> sortedSources = new HashMap<>();
        {
            IdentifiedSource[] identifiedSources = CommonUtils.toIdentifiedSources(args.isRecursive, args.filesToCompile);
            for (IdentifiedSource is : identifiedSources) {
                String fqn = is.getClassIdentifier().getFullName();
                Integer detectedByteCode = null;
                //FIRST try to find the class in target vm if enabled
                if (shouldUpload) {
                    try {
                        int[] versions = Lib.getByteCodeVersions(new ClassInfo(fqn), targetVm, vmManager);
                        detectedByteCode = versions[1];
                        Logger.getLogger().log(Logger.Level.ALL, fqn + " - detected bytecode in target VM: " + detectedByteCode);
                    } catch (Exception ex) {
                        System.out.println(""); //findbugs issue
                    }
                }
                if (detectedByteCode == null) {
                    //FALLBACK try to find the class in target vm in source
                    try {
                        int[] versions = Lib.getByteCodeVersions(new ClassInfo(fqn), args.getClassesProvider().getVmInfo(), vmManager);
                        detectedByteCode = versions[1]; /**/
                        Logger.getLogger().log(Logger.Level.ALL, fqn + " - detected bytecode in source VM: " + detectedByteCode);
                    } catch (Exception ex) {
                        System.out.println(""); //findbugs issue
                    }
                }
                if (detectedByteCode == null) {
                    Logger.getLogger().log(Logger.Level.ALL, fqn + " - failed to detect bytecode level");
                }
                List<IdentifiedSource> sources = sortedSources.get(detectedByteCode);
                if (sources == null) {
                    sources = new ArrayList<>();
                    sortedSources.put(detectedByteCode, sources);
                }
                sources.add(is);
            }
        }
        PluginWrapperWithMetaInfo wrapper = Lib.getPluginWrapper(pluginManager, args.wantedCustomCompiler, true);
        List<IdentifiedBytecode> allBytecode = new ArrayList<>();
        for (Map.Entry<Integer, List<IdentifiedSource>> entry : sortedSources.entrySet()) {
            Integer detectedByteCode = entry.getKey();
            IdentifiedSource[] identifiedSources = entry.getValue().toArray(new IdentifiedSource[0]);
            allBytecode.addAll(compile(args, identifiedSources, wrapper, detectedByteCode, System.err));
        }

        if (shouldUpload) {
            int failCount = 0;
            for (IdentifiedBytecode bytecode : allBytecode) {
                String className = bytecode.getClassIdentifier().getFullName();
                Logger.getLogger().log("Uploading class '" + className + "'.");

                String response = Lib.uploadClass(targetVm, className, Base64.getEncoder().encodeToString(bytecode.getFile()), vmManager);

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

    private Collection<IdentifiedBytecode> compile(
            CompileArguments args, IdentifiedSource[] identifiedSources, PluginWrapperWithMetaInfo wrapper, Integer detectedByteCode,
            PrintStream outerr
    ) {
        String m = "Compiling group of files " + identifiedSources.length + " of level : ";
        if (detectedByteCode == null) {
            m = m + "unknown";
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

    private void printVersion() {
        System.out.println(MetadataProperties.getInstance());
    }

    private void printHelp() {
        Help.printHelpText();
    }

    private VmInfo getVmInfo(String param) {
        return CliUtils.getVmInfo(param, vmManager);
    }

    public List<String> getFilteredArgs() {
        return Collections.unmodifiableList(filteredArgs);
    }
}
