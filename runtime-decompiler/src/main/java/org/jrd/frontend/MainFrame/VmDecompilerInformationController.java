package org.jrd.frontend.MainFrame;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.AgentRequestAction.RequestAction;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.Model;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.NewConnectionFrame.NewConnectionController;
import org.jrd.frontend.NewConnectionFrame.NewConnectionView;
import org.jrd.frontend.PluginMangerFrame.PluginConfigurationEditorController;
import org.jrd.frontend.PluginMangerFrame.PluginConfigurationEditorView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Base64;

/**
 * This class provides Action listeners and result processing for the GUI.
 */
public class VmDecompilerInformationController {

    private final MainFrameView mainFrameView;
    private final BytecodeDecompilerView bytecodeDecompilerView;
    private NewConnectionView newConnectionDialog;
    private PluginConfigurationEditorView pluginConfigurationEditorView;
    private PluginConfigurationEditorController pluginConfigurationEditorController;
    private LoadingDialog loadingDialog;
    private NewConnectionController newConnectionController;
    private VmManager vmManager;
    private VmInfo vmInfo;
    private PluginManager pluginManager;

    public VmDecompilerInformationController(MainFrameView mainFrameView, Model model) {
        this.mainFrameView = mainFrameView;
        this.bytecodeDecompilerView = mainFrameView.getBytecodeDecompilerView();
        this.vmManager = model.getVmManager();
        this.pluginManager = model.getPluginManager();

        updateVmLists();

        vmManager.subscribeToVMChange(e -> updateVmLists());

        mainFrameView.setCreateNewConnectionDialogListener(e -> createNewConnectionDialog());

        bytecodeDecompilerView.setClassesActionListener(e -> loadClassNames());

        bytecodeDecompilerView.setBytesActionListener(e -> loadClassBytecode(e.getActionCommand()));

        mainFrameView.setVmChanging(this::changeVm);

        mainFrameView.setHaltAgentListener(e -> haltAgent());

        mainFrameView.setPluginConfigurationEditorListener(actionEvent -> createConfigurationEditor());
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
        newConnectionController = new NewConnectionController(newConnectionDialog, vmManager);
        newConnectionDialog.setVisible(true);
    }

    private void updateVmLists() {
        ArrayList<VmInfo> localVms = new ArrayList<>();
        ArrayList<VmInfo> remoteVms = new ArrayList<>();
        vmManager.getVmInfoSet().forEach(info -> {
            if (info.getVmPid() > 0) {
                localVms.add(info);
            } else {
                remoteVms.add(info);
            }
        });
        mainFrameView.setLocalVmList(localVms.toArray(new VmInfo[0]));
        mainFrameView.setRemoteVmList(remoteVms.toArray(new VmInfo[0]));
    }

    private void changeVm(ActionEvent event) {
            JList<VmInfo> vmList = (JList<VmInfo>) event.getSource();
            VmInfo selectedVmInfo = vmList.getSelectedValue();
            mainFrameView.switchPanel(selectedVmInfo != null);
            clearOtherList(vmList);
            haltAgent();
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
     * @param vmList list that doesn't get cleared containing the VM that user wants to attach.
     */
    private void clearOtherList(JList<VmInfo> vmList) {
        switch (vmList.getName()) {
            case "remoteVmList":
                mainFrameView.clearLocalListSelection();
                break;
            case "localVmList":
                mainFrameView.clearRemoteListSelection();
                break;
        }
    }

    private void showLoadingDialog() {
        SwingUtilities.invokeLater(() -> {
            loadingDialog = new LoadingDialog();
            loadingDialog.setAbortActionListener(e ->
                    abortAndCleanup());
            loadingDialog.setVisible(true);
        });
    }

    private void hideLoadingDialog() {
        // Avoid race-conditions by keeping dialog open for 100ms.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
        loadingDialog.setVisible(false);

    }

    private void abortAndCleanup() {
        mainFrameView.switchPanel(false);
        mainFrameView.getBytecodeDecompilerView().reloadClassList(new String[0]);
        mainFrameView.getBytecodeDecompilerView().reloadTextField("");
        haltAgent();
        updateVmLists();
        mainFrameView.clearLocalListSelection();
        mainFrameView.clearRemoteListSelection();
        hideLoadingDialog();
    }

    public static final String CLASSES_NOPE = "Classes couldn't be loaded. Do you have agent configured?";

    /**
     * Sends request for classes. If "ok" response is received updates classes list.
     * If "error" response is received shows an error dialog.
     */
    private void loadClassNames() {
        showLoadingDialog();
        AgentRequestAction request = createRequest("", RequestAction.CLASSES);
        String response = submitRequest(request);
        if (response.equals("ok")) {
            VmDecompilerStatus vmStatus = vmInfo.getVmDecompilerStatus();
            String[] classes = vmStatus.getLoadedClassNames();
            bytecodeDecompilerView.reloadClassList(classes);
        }
        hideLoadingDialog();
        if (response.equals("error")) {
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(),
                    CLASSES_NOPE,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadClassBytecode(String name) {
        AgentRequestAction request = createRequest(name, RequestAction.BYTES);
        String response = submitRequest(request);
        String decompiledClass = "";
        if (response.equals("error")) {
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(),
                    "Bytecode couldn't be loaded.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        VmDecompilerStatus vmStatus = vmInfo.getVmDecompilerStatus();
        String bytesInString = vmStatus.getLoadedClassBytes();
        byte[] bytes = Base64.getDecoder().decode(bytesInString);
        try {
            decompiledClass = pluginManager.decompile(bytecodeDecompilerView.getSelecteddecompilerWrapperInformation(), bytes);
        } catch (Exception e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
        bytecodeDecompilerView.reloadTextField(decompiledClass);
    }

    private void haltAgent() {
        if (vmInfo == null || !vmInfo.isLocal()) {
            return;
        }
        try {
            AgentRequestAction request = createRequest("", RequestAction.HALT);
            String response = submitRequest(request);
            if (response.equals("ok")) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "Agent closing socket and exiting");
            }
        } catch (Exception e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Error when sending request to halt agent", e));
        }
    }


    private AgentRequestAction createRequest(String className, RequestAction action) {
        return createRequest(vmInfo, className, action);
    }

    public static AgentRequestAction createRequest(VmInfo vmInfo,  String className, RequestAction action) {
        VmDecompilerStatus status = vmInfo.getVmDecompilerStatus();
        int listenPort = AgentRequestAction.NOT_ATTACHED_PORT;
        String hostname = "localhost";
        if (status != null) {
            listenPort = status.getListenPort();
            hostname = status.getHostname();
        }

        AgentRequestAction request;
        if (action == RequestAction.CLASSES) {
            request = AgentRequestAction.create(vmInfo, hostname, listenPort, action);
        } else if (action == RequestAction.BYTES) {
            request = AgentRequestAction.create(vmInfo, hostname, listenPort, action, className);
        } else if (action == RequestAction.HALT) {
            request = AgentRequestAction.create(vmInfo, hostname, listenPort, action);
        } else {
            throw new AssertionError("Unknown action: " + action);
        }
        return request;
    }

    private String submitRequest(AgentRequestAction request) {
        return submitRequest(vmManager, request);
    }

    public static String submitRequest(VmManager vmManager, AgentRequestAction request) {
        //DecompilerAgentRequestResponseListener listener = new DecompilerAgentRequestResponseListener(latch);
        DecompilerRequestReceiver receiver = new DecompilerRequestReceiver(vmManager);
        String response = receiver.processRequest(request);
        // wait for the request processing

        return response; //listener
    }
}
