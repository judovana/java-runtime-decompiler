package com.redhat.thermostat.vm.decompiler.core;


import com.redhat.thermostat.vm.decompiler.communication.CallDecompilerAgent;

import com.redhat.thermostat.vm.decompiler.core.AgentRequestAction.RequestAction;
import com.redhat.thermostat.vm.decompiler.data.VmManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class manages the requests that are put in queue by the controller.
 */
public class DecompilerRequestReceiver {

    //private static final Logger logger = LoggingUtils.getLogger(DecompilerRequestReciever.class);

    private final AgentAttachManager attachManager;
    private VmManager vmManager;

    private static final String ERROR_RESPONSE = "error";
    private static final String OK_RESPONSE = "ok";
    private static final int NOT_ATTACHED = -1;


    public DecompilerRequestReceiver(VmManager vmManager) {
        this.attachManager = new AgentAttachManager(vmManager);
        this.vmManager = vmManager;
    }

    public String processRequest(AgentRequestAction request) {
        String vmId = request.getParameter(AgentRequestAction.VM_ID_PARAM_NAME);
        String vmPidStr = request.getParameter(AgentRequestAction.VM_PID_PARAM_NAME);
        String hostname = request.getParameter(AgentRequestAction.HOSTNAME_PARAM_NAME);
        String portStr = request.getParameter(AgentRequestAction.LISTEN_PORT_PARAM_NAME);
        String actionStr = request.getParameter(AgentRequestAction.ACTION_PARAM_NAME);
        RequestAction action;
        int vmPid;
        int port;

        try {
            action = RequestAction.returnAction(actionStr);
        } catch (IllegalArgumentException e) {
            //logger.log(Level.WARNING, "Illegal action in request", e);
            return ERROR_RESPONSE;
        }
        port = tryParseInt(portStr, "Listen port is not an integer!");
        vmPid = tryParseInt(vmPidStr, "VM PID is not a number!");

        //logger.log(Level.FINE, "Processing request. VM ID: " + vmId + ", PID: " + vmPid + ", action: " + action + ", port: " + portStr);
        String response;
        switch (action) {
            case BYTES:
                String className = request.getParameter(AgentRequestAction.CLASS_TO_DECOMPILE_NAME);
                response = getByteCodeAction(hostname, port, vmId, vmPid, className);
                break;
            case CLASSES:
                response = getAllLoadedClassesAction(hostname, port, vmId, vmPid);
                break;
            case HALT:
                response = getHaltAction(hostname, port, vmId, vmPid);
                break;
            default:
                //logger.warning("Unknown action given: " + action);
                return ERROR_RESPONSE;
        }
        return response;

    }

    private int tryParseInt(String intStr, String msg) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            //logger.log(Level.WARNING, msg + " Given: " + intStr) ;
            return NOT_ATTACHED;
        }
    }

    private String getByteCodeAction(String hostname, int listenPort, String vmId, int vmPid, String className) {
        int actualListenPort;
        if ("localhost".equals(hostname)){
            try {
                actualListenPort = checkIfAgentIsLoaded(listenPort, vmId, vmPid);
            } catch (Exception ex) {
                return ERROR_RESPONSE;
            }
        } else {
            actualListenPort = listenPort;
        }
        if (actualListenPort == NOT_ATTACHED) {
            //logger.log(Level.WARNING, "Failed to attach agent.");
            return ERROR_RESPONSE;
        }
        CallDecompilerAgent nativeAgent = new CallDecompilerAgent(actualListenPort, hostname);
        try {
            String bytes = nativeAgent.submitRequest("BYTES\n" + className);
            if ("ERROR".equals(bytes)) {
                return ERROR_RESPONSE;

            }
            VmDecompilerStatus status = new VmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(actualListenPort);
            status.setTimeStamp(System.currentTimeMillis());
            status.setVmId(vmId);
            status.setBytesClassName(className);
            status.setLoadedClassBytes(bytes);
            vmManager.replaceVmDecompilerStatus(vmManager.getVmInfoByID(vmId), status);

        } catch (Exception ex) {
            return ERROR_RESPONSE;
        }
        //logger.info("Request for bytecode sent");

        return OK_RESPONSE;
    }

    private String getAllLoadedClassesAction(String hostname, int listenPort, String vmId, int vmPid) {
        int actualListenPort;
        if ("localhost".equals(hostname)){
            try {
                actualListenPort = checkIfAgentIsLoaded(listenPort, vmId, vmPid);
            } catch (Exception ex) {
                return ERROR_RESPONSE;
            }
        } else {
            actualListenPort = listenPort;
        }

        if (actualListenPort == NOT_ATTACHED) {
            //logger.log(Level.WARNING, "Failed to call decompiler agent.");
            return ERROR_RESPONSE;
        }

        try {
            CallDecompilerAgent nativeAgent = new CallDecompilerAgent(actualListenPort, hostname);
            String classes = nativeAgent.submitRequest("CLASSES");

            if ("ERROR".equals(classes)) {
                return ERROR_RESPONSE;
            }
            String[] arrayOfClasses = parseClasses(classes);
            VmDecompilerStatus status = new VmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(actualListenPort);
            status.setTimeStamp(System.currentTimeMillis());
            status.setVmId(vmId);
            status.setLoadedClassNames(arrayOfClasses);
            vmManager.replaceVmDecompilerStatus(vmManager.getVmInfoByID(vmId), status);

        } catch (Exception ex) {
            //logger.log(Level.SEVERE, "Exception occured while processing request: " + ex.getMessage());
            return ERROR_RESPONSE;
        }
        return OK_RESPONSE;

    }

    private String getHaltAction(String hostname, int listenPort, String vmId, int vmPid) {
        int actualListenPort;
        if ("localhost".equals(hostname)){
            try {
                actualListenPort = checkIfAgentIsLoaded(listenPort, vmId, vmPid);
            } catch (Exception ex) {
                return ERROR_RESPONSE;
            }
        } else {
            actualListenPort = listenPort;
        }

        if (actualListenPort == NOT_ATTACHED) {
            return ERROR_RESPONSE;
        }

        try {
            CallDecompilerAgent nativeAgent = new CallDecompilerAgent(actualListenPort, hostname);
            String halt = nativeAgent.submitRequest("HALT");

            if (halt.equals("ERROR")) {
                return ERROR_RESPONSE;
            }

        } catch (Exception e) {
            System.out.println("Exception when calling halt action");
            e.printStackTrace();
        } finally {
            vmManager.removeVmDecompilerStatus(vmManager.getVmInfoByID(vmId));
        }
        return OK_RESPONSE;
    }

    private int checkIfAgentIsLoaded(int port, String vmId, int vmPid) {
        if (port != NOT_ATTACHED) {
            return port;
        }
        int actualListenPort = NOT_ATTACHED;
        VmDecompilerStatus status = attachManager.attachAgentToVm(vmId, vmPid);
        if (status != null) {
            actualListenPort = status.getListenPort();
        }

        return actualListenPort;
    }

    private String[] parseClasses(String classes){
        String[] array = classes.split(";");
        List<String> list = new ArrayList<>(Arrays.asList(array));
        list.removeAll(Arrays.asList("", null));
        List<String> list1 = new ArrayList<>();
        for (String s : list) {
            if (!(s.contains("Lambda") | s.startsWith("["))){
                list1.add(s);
            }
        }
        java.util.Collections.sort(list1);
        return list1.toArray(new String[]{});

    }

}
