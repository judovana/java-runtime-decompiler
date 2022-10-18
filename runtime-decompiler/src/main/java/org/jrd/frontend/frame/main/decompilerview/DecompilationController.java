package org.jrd.frontend.frame.main.decompilerview;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.communication.FsAgent;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.communication.TopLevelErrorCandidate;
import org.jrd.backend.core.AgentAttachManager;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.AgentRequestAction.RequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.Config;
import org.jrd.backend.data.DependenciesReader;
import org.jrd.backend.data.Model;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.frame.filesystem.NewFsVmController;
import org.jrd.frontend.frame.filesystem.NewFsVmView;
import org.jrd.frontend.frame.main.AgentsManager;
import org.jrd.frontend.frame.main.GlobalConsole;
import org.jrd.frontend.frame.main.LoadingDialog;
import org.jrd.frontend.frame.main.LoadingDialogProvider;
import org.jrd.frontend.frame.main.MainFrameView;
import org.jrd.frontend.frame.main.ModelProvider;
import org.jrd.frontend.frame.main.OverridesManager;
import org.jrd.frontend.frame.overwrite.LatestPaths;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.frame.plugins.PluginConfigurationEditorController;
import org.jrd.frontend.frame.plugins.PluginConfigurationEditorView;
import org.jrd.frontend.frame.remote.NewConnectionController;
import org.jrd.frontend.frame.remote.NewConnectionView;
import org.jrd.frontend.utility.CommonUtils;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

/**
 * This class provides Action listeners and request handling for
 * the GUI.
 */
public class DecompilationController implements ModelProvider, LoadingDialogProvider {

    private final MainFrameView mainFrameView;
    private final BytecodeDecompilerView bytecodeDecompilerView;
    private NewConnectionView newConnectionDialog;
    private NewFsVmView newFsVmDialog;
    private PluginConfigurationEditorView pluginConfigurationEditorView;
    private PluginConfigurationEditorController pluginConfigurationEditorController;
    private LoadingDialog loadingDialog;
    private VmManager vmManager;
    private VmInfo vmInfo;
    private PluginManager pluginManager;
    private boolean isVerbose;

    public DecompilationController(MainFrameView mainFrameView, Model model, boolean isVerbose) {
        this.mainFrameView = mainFrameView;
        this.bytecodeDecompilerView = mainFrameView.getBytecodeDecompilerView();
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();
        this.isVerbose = isVerbose;

        updateVmLists();

        vmManager.subscribeToVMChange(e -> updateVmLists());

        mainFrameView.setRefreshLocalVmsListener(e -> vmManager.updateLocalVMs());
        mainFrameView.setNewConnectionDialogListener(e -> createNewConnectionDialog());
        mainFrameView.setNewFsVmDialogListener(e -> createNewFsVMDialog());
        mainFrameView.setRemoveVmDialogListener(this::removeVmDialog);
        bytecodeDecompilerView.setInitActionListener(e -> initClass(e.getActionCommand()));
        bytecodeDecompilerView.setClassesActionListener(e -> loadClassNames());
        bytecodeDecompilerView.setBytesActionListener(e -> {
            showLoadingDialog(a -> hideLoadingDialog(), "Loading bytecode");
            try {
                loadClassBytecode(e.getActionCommand());
            } finally {
                hideLoadingDialog();
            }
        });
        bytecodeDecompilerView.setOverwriteActionListener(new ClassOverwriter());
        bytecodeDecompilerView.setCompileListener(new QuickCompiler());
        bytecodeDecompilerView.setPopup(new AgentApiGenerator());
        bytecodeDecompilerView.setDepsProvider(new DependenciesReader(this, this));
        mainFrameView.setVmChanging(this::changeVm);
        mainFrameView.setHaltAgentListener(e -> haltAgent());
        mainFrameView.setKillAllSessionListener(e -> killAllSession());
        mainFrameView.setPluginConfigurationEditorListener(actionEvent -> createConfigurationEditor());
        mainFrameView.setManageOverrides(new Runnable() {
            @Override
            public void run() {
                OverridesManager.showFor(mainFrameView.getMainFrame(), DecompilationController.this);
            }
        });
        mainFrameView.setManageAgents(new Runnable() {
            @Override
            public void run() {
                AgentsManager.showFor(mainFrameView.getMainFrame(), vmManager);
            }
        });
        bytecodeDecompilerView.refreshComboBox(pluginManager.getWrappers());
    }

    // Method for opening plugin configuration window
    private void createConfigurationEditor() {
        pluginConfigurationEditorView = new PluginConfigurationEditorView(mainFrameView);
        pluginConfigurationEditorController = new PluginConfigurationEditorController(pluginConfigurationEditorView, pluginManager);
        pluginConfigurationEditorController.setPluginsConfiguredListener(actionEvent -> {
            bytecodeDecompilerView.refreshComboBox(pluginManager.getWrappers());
        });
        pluginConfigurationEditorView.setVisible(true);
    }

    private void createNewConnectionDialog() {
        newConnectionDialog = new NewConnectionView(mainFrameView);
        new NewConnectionController(newConnectionDialog, vmManager);
        newConnectionDialog.setVisible(true);

        mainFrameView.switchTabsToRemoteVms(); // for JMenuItem ActionEvent origin
    }

    private void createNewFsVMDialog() {
        newFsVmDialog = new NewFsVmView(mainFrameView);
        new NewFsVmController(newFsVmDialog, vmManager);
        newFsVmDialog.setVisible(true);
    }

    @SuppressWarnings({"unchecked", "CyclomaticComplexity"}) // event.getSource() is always of type JList<VmInfo>
    private void removeVmDialog(ActionEvent event) {
        String vmType = event.getActionCommand();
        JList<VmInfo> sourceList = (JList<VmInfo>) event.getSource();
        VmInfo selectedVm = sourceList.getSelectedValue();
        boolean shouldRemoteDetachAgent = event.getModifiers() == 1 && selectedVm.getType() == VmInfo.Type.REMOTE;

        if (exitOnEmpty(vmType, selectedVm)) {
            return;
        }

        boolean isSavedFsVm = selectedVm.getType() == VmInfo.Type.FS && Config.getConfig().isSavedFsVm(selectedVm);
        String confirmMessage = "Are you sure you want to remove this " + vmType + "?";
        if (isSavedFsVm) {
            confirmMessage += "\nRemoving it will also no longer keep it saved between JRD instances.";
        }

        if (shouldRemoteDetachAgent) {
            confirmMessage += "\nKilling the agent will prohibit any other connection in future. You will need to attach it again";
        }
        int dialogResult =
                JOptionPane.showConfirmDialog(mainFrameView.getMainFrame(), confirmMessage, "Warning", JOptionPane.OK_CANCEL_OPTION);
        if (dialogResult == JOptionPane.CANCEL_OPTION || dialogResult < 0) {
            return;
        }
        if (MainFrameView.FS_VM_COMMAND.equals(vmType)) {
            if (warnOnOvveridesOfFsVm(selectedVm)) {
                return;
            }
        }

        if (shouldRemoteDetachAgent) {
            DecompilerRequestReceiver.getHaltAction(
                    selectedVm.getVmName(), selectedVm.getVmDecompilerStatus().getListenPort(), selectedVm.getVmId(), selectedVm.getVmPid(),
                    new AgentAttachManager(vmManager), vmManager, false
            );
        }

        if (!vmManager.removeVm(selectedVm)) {
            String removeFailMessage = "Failed to remove VM: " + selectedVm;
            Logger.getLogger().log(Logger.Level.ALL, removeFailMessage);
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(), removeFailMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (isSavedFsVm) {
            try {
                Config.getConfig().removeSavedFsVm(selectedVm);
                Config.getConfig().saveConfigFile();
            } catch (IOException e) {
                Logger.getLogger().log(Logger.Level.ALL, "Unable to tag '" + vmInfo + "' as no longer to be saved. Cause:");
                Logger.getLogger().log(e);
            }
        }

        cleanup(true);
    }

    private boolean exitOnEmpty(String vmType, VmInfo selectedVm) {
        if (selectedVm == null) {
            Logger.getLogger().log(Logger.Level.ALL, "Attempted to remove " + vmType + " with none selected.");
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(), "No " + vmType + " selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean warnOnOvveridesOfFsVm(VmInfo selectedVm) {
        return warnOnOvveridesOfFsVm(selectedVm, mainFrameView.getMainFrame());
    }

    public static boolean warnOnOvveridesOfFsVm(VmInfo vmInfo, JFrame parent) {
        List<String> overrides = FsAgent.get(vmInfo).getOverrides();
        if (overrides.size() > 0) {
            String omessage = "This vm " + vmInfo.getVmId() + " have overridden classes: ";
            if (overrides.size() < 5) {
                omessage = omessage + String.join(", ", overrides);
            } else {
                omessage = omessage + overrides.size();
            }
            omessage = omessage + "\n Where running JVM can restore original classes by design, FS ones are now permanently overridden." +
                    "\nPress Cancel to restore them manually";
            int r = JOptionPane.showConfirmDialog(parent, omessage, "Warning", JOptionPane.OK_CANCEL_OPTION);
            if (r != JOptionPane.OK_OPTION) {
                return true;
            }
        }
        return false;
    }

    private void updateVmLists() {
        List<VmInfo> localVms = new ArrayList<>();
        List<VmInfo> remoteVms = new ArrayList<>();
        List<VmInfo> fsVms = new ArrayList<>();

        vmManager.getVmInfoSet().forEach(info -> {
            if (info.getType() == VmInfo.Type.LOCAL) {
                localVms.add(info);
            } else if (info.getType() == VmInfo.Type.REMOTE) {
                remoteVms.add(info);
            } else if (info.getType() == VmInfo.Type.FS) {
                fsVms.add(info);
            }
        });

        localVms.sort(VmInfo.LOCAL_VM_COMPARATOR);
        remoteVms.sort(VmInfo.REMOTE_VM_COMPARATOR);
        fsVms.sort(VmInfo.FS_VM_COMPARATOR);

        mainFrameView.setLocalVmList(localVms.toArray(new VmInfo[0]));
        mainFrameView.setRemoteVmList(remoteVms.toArray(new VmInfo[0]));
        mainFrameView.setFsVmList(fsVms.toArray(new VmInfo[0]));
    }

    private void changeVm(ActionEvent event) {
        JList<VmInfo> vmList = (JList<VmInfo>) event.getSource();
        VmInfo selectedVmInfo = vmList.getSelectedValue();
        mainFrameView.switchPanel(selectedVmInfo != null);
        clearOtherList(vmList);
        if (selectedVmInfo != null) {
            new Thread(() -> {
                this.vmInfo = selectedVmInfo;
                loadClassNames();
            }).start();
        }
    }

    /**
     * If selected list is remoteVmList clears localVmList and vice versa.<br>
     * Effectively merging them into one.
     *
     * @param vmList list that doesn't get cleared containing the VM that user
     *               wants to attach.
     */
    private void clearOtherList(JList<VmInfo> vmList) {
        switch (vmList.getName()) {
            case "remoteVmList":
                mainFrameView.clearLocalListSelection();
                break;
            case "localVmList":
                mainFrameView.clearRemoteListSelection();
                break;
            case "localFsVmList":
                mainFrameView.clearLocalListSelection();
                mainFrameView.clearRemoteListSelection();
                break;
            default:
                throw new RuntimeException("Unknown list requested cleaning.");
        }
    }

    private void showLoadingDialog(String title) {
        showLoadingDialog(a -> abortClassLoading(), title);
    }

    @Override
    public void showLoadingDialog(ActionListener listener, String title) {
        SwingUtilities.invokeLater(() -> {
            loadingDialog = new LoadingDialog(title);
            loadingDialog.setAbortActionListener(listener);
            ScreenFinder.centerWindowOnto(mainFrameView.getMainFrame(), loadingDialog);
            loadingDialog.setVisible(true);
        });
    }

    @Override
    public void hideLoadingDialog() {
        // Avoid race-conditions by queueing closing after opening
        SwingUtilities.invokeLater(() -> loadingDialog.dispose());
    }

    private void cleanup(boolean halt) {
        mainFrameView.switchPanel(false);
        mainFrameView.getBytecodeDecompilerView().reloadClassList(new ClassInfo[0]);
        mainFrameView.getBytecodeDecompilerView().reloadTextField("", "", new byte[16], "", new byte[16], "");
        if (halt) {
            haltAgent();
        }
    }

    private void abortClassLoading() {
        cleanup(false);
        updateVmLists();
        mainFrameView.clearLocalListSelection();
        mainFrameView.clearRemoteListSelection();
        hideLoadingDialog();
    }

    public static final String CLASSES_NOPE = "Classes couldn't be loaded." + "Do you have agent configured?" +
            "On JDK 9 and higher, did you run the target process with '-Djdk.attach.allowAttachSelf=true'?" +
            " Or maybe agent is not loaded? Or bad agent?";

    private void initClass(String fqn) {
        showLoadingDialog("Initializing class");
        AgentRequestAction request = createRequest(RequestAction.INIT_CLASS, fqn);
        String response = submitRequest(request);
        hideLoadingDialog();
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            loadClassNames();
        }
        if (new TopLevelErrorCandidate(response).isError()) {
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(), response + "\n" + CLASSES_NOPE, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sends request for classes. If "ok" response is received updates classes
     * list. If "error" response is received shows an error dialog.
     */
    private void loadClassNames() {
        showLoadingDialog("Loading classes");
        AgentRequestAction request =
                createRequest(bytecodeDecompilerView.doShowClassInfo() ? RequestAction.CLASSES_WITH_INFO : RequestAction.CLASSES, "");
        String response = submitRequest(request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            bytecodeDecompilerView.reloadClassList(vmInfo.getVmDecompilerStatus().getLoadedClasses());
        }
        hideLoadingDialog();
        if (new TopLevelErrorCandidate(response).isError()) {
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(), response + "\n" + CLASSES_NOPE, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadClassBytecode(String name) {
        AgentRequestAction request = createRequest(RequestAction.BYTES, name);
        String response = submitRequest(request);
        String decompiledClass = "";
        if (new TopLevelErrorCandidate(response).isError()) {
            JOptionPane.showMessageDialog(
                    mainFrameView.getMainFrame(), response + "\nBytecode couldn't be loaded.", "Error", JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        VmDecompilerStatus vmStatus = vmInfo.getVmDecompilerStatus();
        String bytesInString = vmStatus.getLoadedClassBytes();
        byte[] bytes = Base64.getDecoder().decode(bytesInString);
        try {
            decompiledClass = pluginManager.decompile(bytecodeDecompilerView.getSelectedDecompiler(), name, bytes, null, vmInfo, vmManager);
        } catch (Exception e) {
            Logger.getLogger().log(Logger.Level.ALL, e);
        }
        byte[] additionalBytes = Config.getConfig().getAdditionalClassPathBytes(name);
        String additionalDecompiled = "";
        if (additionalBytes != null && additionalBytes.length > 0) {
            try {
                //FIXME this have to fake vminfo and vmmanager to rovide origin form local cp only
                //FIXME this actually discovered a bug in some wrappers, which have wrong fallback method (eg cfr, but fernflower is ok)
                additionalDecompiled =
                        pluginManager.decompile(bytecodeDecompilerView.getSelectedDecompiler(), name, additionalBytes, null, null, null);
            } catch (Exception e) {
                Logger.getLogger().log(Logger.Level.ALL, e);
            }
        }
        String additionalSrcClass = Config.getConfig().getAdditionalSourcePathString(name);
        bytecodeDecompilerView.reloadTextField(name, decompiledClass, bytes, additionalDecompiled, additionalBytes, additionalSrcClass);
    }

    public String getVm() {
        if (vmInfo == null) {
            return null;
        }
        return vmInfo.getVmId();
    }

    public String[] getOverrides() {
        AgentRequestAction request = createRequest(AgentRequestAction.RequestAction.OVERRIDES);
        String response = submitRequest(request);
        if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            VmDecompilerStatus vmStatus = vmInfo.getVmDecompilerStatus();
            String[] classes = vmStatus.getLoadedClassNames();
            return classes;
        } else {
            throw new RuntimeException(response);
        }
    }

    public void removeOverrides(String pattern) {
        AgentRequestAction request = createRequest(vmInfo, RequestAction.REMOVE_OVERRIDES, pattern);
        String response = submitRequest(request);
        if (!DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
            throw new RuntimeException(response);
        }
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "vm info is know everywhere")
    public VmInfo getVmInfo() {
        return vmInfo;
    }

    @Override
    public VmManager getVmManager() {
        return vmManager;
    }

    @Override
    public RuntimeCompilerConnector.JrdClassesProvider getClassesProvider() {
        return new RuntimeCompilerConnector.JrdClassesProvider(vmInfo, vmManager);
    }

    class QuickCompiler {

        public void upload(String clazz, byte[] body) {
            CommonUtils.uploadByGui(vmInfo, vmManager, new CommonUtils.StatusKeeper() {
                @Override
                public void setText(String s) {
                    GlobalConsole.getConsole().addMessage(Level.WARNING, s);
                }

                @Override
                public void onException(Exception ex) {
                    Logger.getLogger().log(ex);
                    GlobalConsole.getConsole().addMessage(Level.WARNING, ex.toString());
                }
            }, clazz, body);
        }

        public void run(DecompilerWrapper wrapper, boolean upload, IdentifiedSource... srcs) {
            PluginManager.BundledCompilerStatus internalCompiler = pluginManager.getBundledCompilerStatus(wrapper);
            GlobalConsole.getConsole().addMessage(Level.ALL, internalCompiler.getStatus());
            OverwriteClassDialog.CompilationWithResult compiler = new OverwriteClassDialog.CompilationWithResult(
                    OverwriteClassDialog.getClasspathlessCompiler(wrapper, internalCompiler.isEmbedded(), isVerbose), getClassesProvider(),
                    GlobalConsole.getConsole(), srcs
            ) {

                @Override
                public void run() {
                    super.run();
                    if (this.getResult() != null && !this.getResult().isEmpty()) {
                        for (IdentifiedBytecode bin : this.getResult()) {
                            if (upload) {
                                GlobalConsole.getConsole().addMessage(Level.ALL, "Uploading: " + bin.getClassIdentifier().getFullName());
                                upload(bin.getClassIdentifier().getFullName(), bin.getFile());
                            } else {
                                GlobalConsole.getConsole().addMessage(Level.ALL, "Compiled " + bin.getClassIdentifier().getFullName());
                            }
                        }
                    } else {
                        GlobalConsole.getConsole().addMessage(Level.ALL, "No output from compilation");
                    }
                }
            };
            Thread t = new Thread(compiler);
            t.start();
        }
    }

    class ClassOverwriter {
        private LatestPaths lastLoaded = new LatestPaths();

        void overwriteClass(DecompilerWrapper selectedDecompiler, String name, String buffer, byte[] binBuffer, boolean isBinary) {
            if (name == null || name.trim().isEmpty()) {
                name = "???";
            }

            final OverwriteClassDialog overwriteClassDialog = new OverwriteClassDialog(
                    name, lastLoaded, buffer, binBuffer, vmInfo, vmManager, pluginManager, selectedDecompiler, isBinary, isVerbose
            );
            ScreenFinder.centerWindowToCurrentScreen(overwriteClassDialog);
            overwriteClassDialog.setVisible(true);

            lastLoaded.setLastManualUpload(overwriteClassDialog.getManualUploadPath());
            lastLoaded.setLastSaveSrc(overwriteClassDialog.getSaveSrcPath());
            lastLoaded.setLastSaveBin(overwriteClassDialog.getSaveBinPath());
            lastLoaded.setFilesToCompile(overwriteClassDialog.getFilesToCompile());
            lastLoaded.setOutputExternalFilesDir(overwriteClassDialog.getOutputExternalFilesDir());
            lastLoaded.setOutputBinaries(overwriteClassDialog.getOutputBinaries());
        }
    }

    public static String fileToBase64(String path, boolean deHex) {
        try {
            if (deHex) {
                return bytesToBase64(HexWithControls.hexToBytes(HexWithControls.hexLinesToHexString(fileToLines(path))));
            } else {
                return bytesToBase64(fileToBytes(path));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String stdinToBase64(boolean deHex) {
        try {
            if (deHex) {
                return bytesToBase64(HexWithControls.hexToBytes(HexWithControls.hexLinesToHexString(stdinToStrings())));
            } else {
                return bytesToBase64(stdinToBytes());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] fileToBytes(String path) throws IOException {
        return Files.readAllBytes(new File(path).toPath());
    }

    public static List<String> fileToLines(String path) throws IOException {
        return Files.readAllLines(new File(path).toPath());
    }

    public static byte[] stdinToBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[32 * 1024];
        int bytesRead;
        while ((bytesRead = System.in.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }

    public static List<String> stdinToStrings() throws IOException {
        List<String> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                r.add(s);
            }
        }
        return r;
    }

    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void haltAgent() {
        if (vmInfo == null || vmInfo.getType() == VmInfo.Type.REMOTE || vmInfo.getType() == VmInfo.Type.FS) {
            return;
        }
        try {
            AgentRequestAction request = createRequest(RequestAction.HALT, "");
            String response = submitRequest(request);
            if (DecompilerRequestReceiver.OK_RESPONSE.equals(response)) {
                Logger.getLogger().log(Logger.Level.DEBUG, "Agent closing socket and exiting");
            }
        } catch (Exception e) {
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Error when sending request to halt agent", e));
        }
    }

    private void killAllSession() {
        KnownAgents.getInstance().killAllSessionAgents(vmManager);
    }

    private AgentRequestAction createRequest(RequestAction action, String... commands) {
        return createRequest(vmInfo, action, commands);
    }

    public static AgentRequestAction createRequest(VmInfo vmInfo, RequestAction action, String... commands) {
        VmDecompilerStatus status = vmInfo.getVmDecompilerStatus();
        int listenPort = AgentRequestAction.NOT_ATTACHED_PORT;
        String hostname = "localhost";
        if (status != null) {
            listenPort = status.getListenPort();
            hostname = status.getHostname();
        }

        AgentRequestAction request;
        if (null == action) {
            throw new AssertionError("Unknown null action");
        }

        switch (action) {
            case VERSION:
            case CLASSES:
            case OVERRIDES:
            case CLASSES_WITH_INFO:
            case HALT:
                request = AgentRequestAction.create(vmInfo, hostname, listenPort, action);
                break;
            case REMOVE_OVERRIDES:
            case INIT_CLASS:
            case BYTES:
                request = AgentRequestAction.create(vmInfo, hostname, listenPort, action, commands[0]);
                break;
            case OVERWRITE:
                try {
                    request = AgentRequestAction.create(vmInfo, hostname, listenPort, action, commands[0], commands[1]);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                break;
            default:
                throw new AssertionError("Unknown action: " + action);
        }

        return request;
    }

    String submitRequest(AgentRequestAction request) {
        return submitRequest(vmManager, request);
    }

    public static String submitRequest(VmManager vmManager, AgentRequestAction request) {
        DecompilerRequestReceiver receiver = new DecompilerRequestReceiver(vmManager);
        // wait for the request processing
        return receiver.processRequest(request); //listener
    }

    public class AgentApiGenerator {
        public JPopupMenu getFor(RSyntaxTextArea text, boolean filtered) {
            if (vmInfo.getVmPid() >= 0) {
                org.jrd.frontend.utility.AgentApiGenerator.initItems(vmInfo, vmManager, pluginManager);
                return org.jrd.frontend.utility.AgentApiGenerator
                        .create(text, filtered ? createFilter(text.getText(), text.getCaretPosition()) : null);
            } else {
                JPopupMenu p = new JPopupMenu();
                JMenuItem infoItem = new JMenuItem("Agent API is only valid for running VMs!");
                infoItem.setEnabled(false);
                p.add(infoItem);
                return p;
            }
        }
    }

    private String createFilter(String text, int caretPosition) {
        int start = caretPosition;
        while (true) {
            start = start - 1;
            if (start < 0) {
                break;
            }
            if ((text.charAt(start) + "").matches("\\s")) {
                break;
            }
            if (text.charAt(start) == '(' || text.charAt(start) == '{') {
                start++;
                break;
            }
        }
        String sub = text.substring(Math.max(0, start), caretPosition).trim();
        return sub;
    }
}
