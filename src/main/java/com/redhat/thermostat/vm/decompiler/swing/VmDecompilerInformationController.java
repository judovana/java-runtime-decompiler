package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction;
import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction.RequestAction;
import com.redhat.thermostat.vm.decompiler.core.DecompilerRequestReceiver;
import com.redhat.thermostat.vm.decompiler.core.VmDecompilerStatus;
import com.redhat.thermostat.vm.decompiler.data.Config;
import com.redhat.thermostat.vm.decompiler.data.VmInfo;
import com.redhat.thermostat.vm.decompiler.data.VmManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * This class provides Action listeners and result processing for the GUI.
 */
public class VmDecompilerInformationController {

    private final MainFrameView mainFrameView;
    private final BytecodeDecompilerView bytecodeDecompilerView;
    private NewConnectionView newConnectionDialog;
    private LoadingDialog loadingDialog;
    private NewConnectionController newConnectionController;
    private VmManager vmManager;
    private VmInfo vmInfo;
    private boolean listsUpdating = false;

    public VmDecompilerInformationController(MainFrameView mainFrameView, VmManager vmManager) {
        this.mainFrameView = mainFrameView;
        this.bytecodeDecompilerView = mainFrameView.getBytecodeDecompilerView();
        this.vmManager = vmManager;

        updateVmLists();

        vmManager.setUpdateVmListsListener(e -> updateVmLists());

        mainFrameView.setCreateNewConnectionDialogListener(e -> createNewConnectionDialog());

        bytecodeDecompilerView.setClassesActionListener(e -> loadClassNames());

        bytecodeDecompilerView.setBytesActionListener(e -> loadClassBytecode(e.getActionCommand()));

        mainFrameView.setVmChanging(this::changeVm);

        mainFrameView.setHaltAgentListener(e -> haltAgent());

    }

    private void createNewConnectionDialog() {
        newConnectionDialog = new NewConnectionView(mainFrameView);
        newConnectionController = new NewConnectionController(newConnectionDialog, vmManager);
        newConnectionDialog.setVisible(true);
    }

    private void updateVmLists() {
        listsUpdating = true;
        ArrayList<VmInfo> localVms = new ArrayList<>();
        ArrayList<VmInfo> remoteVms = new ArrayList<>();
        vmManager.getAllVm().forEach(info -> {
            if (info.getVmPid() > 0) {
                localVms.add(info);
            } else {
                remoteVms.add(info);
            }
        });
        mainFrameView.setLocalVmList(localVms.toArray(new VmInfo[0]));
        mainFrameView.setRemoteVmList(remoteVms.toArray(new VmInfo[0]));
        listsUpdating = false;
    }

    private void changeVm(ActionEvent event) {
        if (!listsUpdating) {
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
    }

    /**
     * If selected list is remoteVmList clears localVmList and vice versa.<br>
     * Effectively merging them into one.
     *
     * @param vmList list that doesn't get cleared containing the VM that user wants to attach.
     */
    private void clearOtherList(JList<VmInfo> vmList) {
        listsUpdating = true;
        switch (vmList.getName()) {
            case "remoteVmList":
                mainFrameView.clearLocalListSelection();
                break;
            case "localVmList":
                mainFrameView.clearRemoteListSelection();
                break;
        }
        listsUpdating = false;
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
            e.printStackTrace();
        }
        loadingDialog.setVisible(false);
    }

    private void abortAndCleanup() {
        mainFrameView.switchPanel(false);
        mainFrameView.getBytecodeDecompilerView().reloadClassList(new String[0]);
        mainFrameView.getBytecodeDecompilerView().reloadTextField("");
        haltAgent();
        listsUpdating = true;
        updateVmLists();
        mainFrameView.clearLocalListSelection();
        mainFrameView.clearRemoteListSelection();
        listsUpdating = false;
        hideLoadingDialog();
    }

    /**
     * Sends request for classes. If "ok" response is received updates classes list.
     * If "error" response is received shows an error dialog.
     */
    private void loadClassNames() {
        showLoadingDialog();
        AgentRequestAction request = createRequest("", RequestAction.CLASSES);
        String response = submitRequest(request);
        if (response.equals("ok")) {
            VmDecompilerStatus vmStatus = vmManager.getVmDecompilerStatus(vmInfo);
            String[] classes = vmStatus.getLoadedClassNames();
            bytecodeDecompilerView.reloadClassList(classes);
        }
        hideLoadingDialog();
        if (response.equals("error")) {
            JOptionPane.showMessageDialog(mainFrameView.getMainFrame(),
                    "Classes couldn't be loaded.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadClassBytecode(String name) {
        AgentRequestAction request = createRequest(name, RequestAction.BYTES);
        //DecompilerAgentRequestResponseListener listener =
        submitRequest(request);
        String decompiledClass = "";
        boolean success = true;//!listener.isError();
        if (success) {
            VmDecompilerStatus vmStatus = vmManager.getVmDecompilerStatus(vmInfo);
            String expectedClass = "";
            while (!expectedClass.equals(name)) {
                vmStatus = vmManager.getVmDecompilerStatus(vmInfo);
                expectedClass = vmStatus.getBytesClassName();

            }
            String bytesInString = vmStatus.getLoadedClassBytes();

            byte[] bytes = Base64.getDecoder().decode(bytesInString);
            try {
                String path = bytesToFile("temporary-byte-file", bytes);
                Process proc = Runtime.getRuntime().exec("java -jar " + Config.getConfig().getDecompilerPath() + " " + path);
                InputStream in = proc.getInputStream();
                decompiledClass = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));
                ;

            } catch (IOException e) {
                //bytecodeDecompilerView.handleError(new LocalizedString(listener.getErrorMessage()));
            }
            bytecodeDecompilerView.reloadTextField(decompiledClass);
        } else {
            //bytecodeDecompilerView.handleError(new LocalizedString(listener.getErrorMessage()));
        }
    }

    private void haltAgent() {
        if (vmInfo == null) {
            return;
        }
        try {
            AgentRequestAction request = createRequest("", RequestAction.HALT);
            String response = submitRequest(request);
            if (response.equals("ok")) {
                System.out.println("Agent closing socket and exiting");
            }
        } catch (Exception e) {
            System.out.println("Error when sending request to halt agent");
        }
        vmInfo = null;
    }

    private AgentRequestAction createRequest(String className, RequestAction action) {
        VmDecompilerStatus status = vmManager.getVmDecompilerStatus(vmInfo);
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
        //DecompilerAgentRequestResponseListener listener = new DecompilerAgentRequestResponseListener(latch);
        DecompilerRequestReceiver receiver = new DecompilerRequestReceiver(vmManager);
        String response = receiver.processRequest(request);
        // wait for the request processing

        return response; //listener
    }

    private String bytesToFile(String name, byte[] bytes) throws IOException {
        String path = "/tmp/" + name + ".class";
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(bytes);
        fos.close();
        return path;
    }

}
