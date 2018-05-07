package com.redhat.thermostat.vm.decompiler.swing;

import workers.VmId;
import workers.VmInfo;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction;
import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction.RequestAction;
import java.io.FileOutputStream;
import java.io.IOException;
import com.redhat.thermostat.vm.decompiler.core.VmDecompilerStatus;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.vm.decompiler.core.DecompilerAgentRequestResponseListener;
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

    private final VmRef vm;
    //private final AgentInfoDAO agentInfoDao;
    //private final VmInfoDAO vmInfoDao;
    //private final RequestQueue requestQueue;
    //private final VmDecompilerDAO vmDecompilerDao;
    private final BytecodeDecompilerView view;
    private VmManager vmManager;
    //private static final Translate<LocaleResources> translateResources = LocaleResources.createLocalizer();
    private static final String PATH_TO_DECOMPILER_ENV_VAR = "PATH_TO_GIVEN_DECOMPILER_JAR";


    VmDecompilerInformationController(final BytecodeDecompilerView view, VmRef ref, VmManager vmManager) {
        this.vm = ref;
        //this.agentInfoDao = agentInfoDao;
        //this.vmInfoDao = vmInfoDao;
        this.view = view;
        this.vmManager = vmManager;

        view.setClassesActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

            loadClassNames();

            }

        });

        view.setBytesActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                   loadClassBytecode(actionEvent.getClassName());
                }
            }
        );

    }

    private DecompilerAgentRequestResponseListener loadClassNames() {
        AgentRequestAction request = createRequest("", RequestAction.CLASSES);
        DecompilerAgentRequestResponseListener listener = submitRequest(request);
        boolean success = !listener.isError();
        if (success) {
            VmId vmId = new VmId(vm.getVmId());
            VmDecompilerStatus vmStatus = vmManager.getVmDecompilerStatus(vmId);
            String [] classes = vmStatus.getLoadedClassNames();
            while (classes.length == 0) {
                 vmStatus = vmManager.getVmDecompilerStatus(vmId);
                classes = vmStatus.getLoadedClassNames();
            }
            view.reloadClassList(classes);
        } else {
            //view.handleError(new LocalizedString(listener.getErrorMessage()));
        }
        return listener;
    }

    private DecompilerAgentRequestResponseListener loadClassBytecode(String name) {
        AgentRequestAction request = createRequest(name, RequestAction.BYTES);
        DecompilerAgentRequestResponseListener listener = submitRequest(request);
        String decompiledClass = "";
        boolean success = !listener.isError();
        if (success) {

            VmId vmId = new VmId(vm.getVmId());

            VmDecompilerStatus vmStatus = vmManager.getVmDecompilerStatus(vmId);
            String expectedClass = "";
            while (!expectedClass.equals(name)) {     
                vmStatus = vmManager.getVmDecompilerStatus(vmId);
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

        return listener;
    }

    private AgentRequestAction createRequest(String className, RequestAction action) {
        VmId vmId = new VmId(vm.getVmId());
        VmInfo vmInfo = createVmInfo(vm);
        VmDecompilerStatus status = vmManager.getVmDecompilerStatus(vmId);
        int listenPort = AgentRequestAction.NOT_ATTACHED_PORT;
        if (status != null) {
            listenPort = status.getListenPort();
        }

        AgentInformation agentInfo = agentInfoDao.getAgentInformation(new AgentId(vm.getHostRef().getAgentId()));
        InetSocketAddress address = agentInfo.getRequestQueueAddress();

        AgentRequestAction request;
        if (action == RequestAction.CLASSES) {
            request = AgentRequestAction.create(address, vmInfo, action, listenPort);
        } else if (action == RequestAction.BYTES) {
            request = AgentRequestAction.create(address, vmInfo, action, listenPort, className);
        } else {
            throw new AssertionError("Unknown action: " + action);
        }

        return request;
    }

    private VmInfo createVmInfo(VmRef vmRef) {
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId(vm.getHostRef().getAgentId());
        vmInfo.setVmId(vm.getVmId());
        vmInfo.setVmPid(vm.getPid());
        return vmInfo;
    }

    private DecompilerAgentRequestResponseListener submitRequest(AgentRequestAction request) {
        CountDownLatch latch = new CountDownLatch(1);
        DecompilerAgentRequestResponseListener listener = new DecompilerAgentRequestResponseListener(latch);
        request.addListener(listener);
        DecompilerRequestReciever receiver = new DecompilerRequestReciever(vmManager);
        receiver.processRequest(request);
        // wait for the request processing
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore, is not relevant
        }
        return listener;
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
    
    @Override
    public UIComponent getView() {
        return view;

    }

    @Override
    public LocalizedString getLocalizedName() {
        return translateResources.localize(LocaleResources.VM_DECOMPILER_TAB_NAME);
    }
 */
    private byte[] parseBytes(String bytes) {
        byte[] decoded = Base64.getDecoder().decode(bytes);
        return decoded;
    }

}
