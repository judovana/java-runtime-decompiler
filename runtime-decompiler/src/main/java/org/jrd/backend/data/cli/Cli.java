package org.jrd.backend.data.cli;

import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.Model;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.utils.AgentConfig;
import org.jrd.backend.data.cli.utils.ReceivedType;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.backend.data.cli.workers.AddClasses;
import org.jrd.backend.data.cli.workers.Api;
import org.jrd.backend.data.cli.workers.AttachDetach;
import org.jrd.backend.data.cli.workers.Classes;
import org.jrd.backend.data.cli.workers.Compile;
import org.jrd.backend.data.cli.workers.Decompile;
import org.jrd.backend.data.cli.workers.InitClass;
import org.jrd.backend.data.cli.workers.ListAgents;
import org.jrd.backend.data.cli.workers.ListJvms;
import org.jrd.backend.data.cli.workers.ListPlugins;
import org.jrd.backend.data.cli.workers.Overrides;
import org.jrd.backend.data.cli.workers.OverwriteAndUpload;
import org.jrd.backend.data.cli.workers.Patch;
import org.jrd.backend.data.cli.workers.PrintBytes;
import org.jrd.backend.decompiling.PluginManager;
import org.kcc.CompletionItem;
import org.kcc.wordsets.ConnectedKeywords;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private AgentConfig currentAgent = AgentConfig.getDefaultSinglePermanentAgent();

    public Cli(String[] orig, Model model) {
        this.filteredArgs = prefilterArgs(orig);
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();
    }

    public boolean shouldBeVerbose() {
        return isVerbose;
    }

    public boolean isGui() {
        return filteredArgs.isEmpty() || CliSwitches.noMatch(filteredArgs);
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
        currentAgent = AgentConfig.create(
                agentArgs,
                args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(OVERWRITE)) ||
                        args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(ATTACH)) ||
                        args.stream().map(a -> CliUtils.cleanParameter(a)).anyMatch(a -> a.equals(PATCH)) ||
                        (args.stream().map(a -> CliUtils.cleanParameter(a))
                                .anyMatch(a -> a.equals(COMPILE) && Compile.shouldUpload(saving)))
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
                    new Compile(isHex, isVerbose, filteredArgs, saving, vmManager, pluginManager).compileWrapper(operatedOn);
                    break;
                case PATCH:
                    VmInfo patchVmInfo =
                            new Patch(isHex, isVerbose, filteredArgs, isRevert, vmManager, pluginManager, isBoot, saving).patch();
                    operatedOn.add(patchVmInfo);
                    break;
                case OVERWRITE:
                    VmInfo vmInfo5 = new OverwriteAndUpload(filteredArgs, vmManager, isBoot, isHex).overwrite(ReceivedType.OVERWRITE_CLASS);
                    operatedOn.add(vmInfo5);
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
                    VmInfo vmInfo7 = new AttachDetach(filteredArgs, vmManager).attach(currentAgent);
                    operatedOn.add(vmInfo7);
                    break;
                case DETACH:
                    new AttachDetach(filteredArgs, vmManager).detach();
                    break;
                case API:
                    VmInfo vmInfo8 = new Api(filteredArgs, saving, vmManager, pluginManager).api();
                    operatedOn.add(vmInfo8);
                    break;
                case COMPLETION:
                    printCompletion(filteredArgs);
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
            if (currentAgent.getLiveliness() == AgentLiveliness.SESSION) {
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
            } else if (currentAgent.getLiveliness() == AgentLiveliness.ONE_SHOT) {
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

    private void printCompletion(List<String> filteredArgsLocal) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        if (filteredArgsLocal.size() == 1) {
            System.out.println(org.kcc.wordsets.JrdApiKeywords.class.getSimpleName());
            System.out.println(org.kcc.wordsets.BytecodeKeywordsWithHelp.class.getSimpleName());
            System.out.println(org.kcc.wordsets.BytemanKeywords.class.getSimpleName());
            System.out.println(org.kcc.wordsets.JavaKeywordsWithHelp.class.getSimpleName());
        } else {
            List<CompletionItem.CompletionItemSet> sets = new ArrayList<>();
            for (int x = 1; x < filteredArgsLocal.size(); x++) {
                for (String s : filteredArgsLocal.get(x).split(",")) {
                    Class completion = Class.forName("org.kcc.wordsets." + s);
                    Object obejct = completion.getDeclaredConstructor().newInstance();
                    CompletionItem.CompletionItemSet cs = (CompletionItem.CompletionItemSet) obejct;
                    sets.add(cs);
                }
            }
            ConnectedKeywords ck = new ConnectedKeywords(sets.toArray(new CompletionItem.CompletionItemSet[0]));
            for (CompletionItem ci : ck.getItemsList()) {
                System.out.println("**** " + ci.getKey() + " ****");
                if (!ci.getRealReplacement().equals(ci.getKey())) {
                    System.out.println("-> " + ci.getRealReplacement());
                }
                System.out.println(ci.getDescription());
            }
        }
    }

    private void printConfig() throws IOException {
        if (isVerbose) {
            System.out.println(Files.readString(Config.getConfig().getConfFile().toPath()));
        } else {
            System.out.println(Config.getConfig().getConfFile().getAbsolutePath());
        }
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
