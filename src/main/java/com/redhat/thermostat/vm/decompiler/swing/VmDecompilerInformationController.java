package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmInfo;
import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction;
import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction.RequestAction;
import java.io.FileOutputStream;
import java.io.IOException;
import com.redhat.thermostat.vm.decompiler.core.VmDecompilerStatus;

//import com.redhat.thermostat.vmInfo.decompiler.core.DecompilerAgentRequestResponseListener;
import com.redhat.thermostat.vm.decompiler.core.DecompilerRequestReciever;
import com.redhat.thermostat.vm.decompiler.data.VmManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * This class provides Action listeners and result processing for the GUI.
 */
public class VmDecompilerInformationController {

    private final VmInfo vmInfo;
    //private final AgentInfoDAO agentInfoDao;
    //private final VmInfoDAO vmInfoDao;
    //private final RequestQueue requestQueue;
    //private final VmDecompilerDAO vmDecompilerDao;
    private final BytecodeDecompilerView view;
    private VmManager vmManager;
    //private static final Translate<LocaleResources> translateResources = LocaleResources.createLocalizer();
    private static final String PATH_TO_DECOMPILER_ENV_VAR = "PATH_TO_GIVEN_DECOMPILER_JAR";

    VmDecompilerInformationController(final BytecodeDecompilerView view, VmInfo vmInfo, VmManager vmManager) {
        this.vmInfo = vmInfo;
        this.view = view;
        this.vmManager = vmManager;

        loadClassNames(); //FIXME

        view.setClassesActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                loadClassNames();

            }

        });

        view.setBytesActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadClassBytecode(e.getActionCommand());
            }
        });

        view.setHaltActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                haltAgent();
            }
        });

    }

    private void loadClassNames() {
        AgentRequestAction request = createRequest("", RequestAction.CLASSES);
        //DecompilerAgentRequestResponseListener listener = 
        String response = submitRequest(request);
        if (response.equals("ok")) {
            VmDecompilerStatus vmStatus = vmManager.getVmDecompilerStatus(vmInfo);
            String[] classes = vmStatus.getLoadedClassNames();
            while (classes.length == 0) {
                vmStatus = vmManager.getVmDecompilerStatus(vmInfo);
                classes = vmStatus.getLoadedClassNames();
            }
            view.reloadClassList(classes);
        } else {
            System.err.println("Classes couldn't be loaded");
            //view.handleError(new LocalizedString(listener.getErrorMessage()));
        }
        return;// listener;
    }

    private void haltAgent(){
        AgentRequestAction request = createRequest("", RequestAction.HALT);
        String response = submitRequest(request);
        if (response.equals("ok")){
            System.out.println("Agent closing socket and exiting");
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

            byte[] bytes = parseBytes(bytesInString);
            try {
                String path = bytesToFile("temporary-byte-file", bytes);
                Process proc = Runtime.getRuntime().exec("java -jar " + System.getenv(PATH_TO_DECOMPILER_ENV_VAR) + " " + path);
                InputStream in = proc.getInputStream();
                decompiledClass = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));;

            } catch (IOException e) {
                //view.handleError(new LocalizedString(listener.getErrorMessage()));
            }
            view.reloadTextField(decompiledClass);
        } else {
            //view.handleError(new LocalizedString(listener.getErrorMessage()));
        }

        return; //listener;
    }

    private AgentRequestAction createRequest(String className, RequestAction action) {
        VmDecompilerStatus status = vmManager.getVmDecompilerStatus(vmInfo);
        int listenPort = AgentRequestAction.NOT_ATTACHED_PORT;
        if (status != null) {
            listenPort = status.getListenPort();
        }

        AgentRequestAction request;
        if (action == RequestAction.CLASSES) {
            request = AgentRequestAction.create(vmInfo, action, listenPort);
        } else if (action == RequestAction.BYTES) {
            request = AgentRequestAction.create(vmInfo, action, listenPort, className);
        } else if (action == RequestAction.HALT) {
            request = AgentRequestAction.create(vmInfo, action, listenPort);
        } else {
            throw new AssertionError("Unknown action: " + action);
        }

        return request;
    }

//    private VmInfo createVmInfo(VmRef vmRef) {
//        VmInfo vmInfo = new VmInfo();
//        vmInfo.setVmId(this.vmInfo.getVmId());
//        vmInfo.setVmPid(this.vmInfo.getPid());
//        return vmInfo;
//    }

    private String submitRequest(AgentRequestAction request) {
        //DecompilerAgentRequestResponseListener listener = new DecompilerAgentRequestResponseListener(latch);
        DecompilerRequestReciever receiver = new DecompilerRequestReciever(vmManager);
        System.out.println("Decompiler ok");
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

    /**
     * Returns instance of BytecodeDecompilerView for the GUI.
     *
     * @return instance of BytecodeDecompilerView
     *
     * @Override public UIComponent getView() { return view;
     *
     * }
     *
     * @Override public LocalizedString getLocalizedName() { return
     * translateResources.localize(LocaleResources.VM_DECOMPILER_TAB_NAME); }
     */
    private byte[] parseBytes(String bytes) {
        byte[] decoded = Base64.getDecoder().decode(bytes);
        return decoded;
    }

}
