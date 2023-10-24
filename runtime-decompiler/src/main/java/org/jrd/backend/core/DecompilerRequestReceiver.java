package org.jrd.backend.core;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.communication.CallDecompilerAgent;
import org.jrd.backend.communication.ErrorCandidate;
import org.jrd.backend.communication.FsAgent;
import org.jrd.backend.communication.JrdAgent;
import org.jrd.backend.communication.TopLevelErrorCandidate;
import org.jrd.backend.core.AgentRequestAction.RequestAction;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.data.cli.utils.AgentConfig;
import org.jrd.backend.decompiling.PluginManager;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class manages the requests that are put in queue by the controller.
 */
public class DecompilerRequestReceiver {

    private final AgentAttachManager attachManager;
    private VmManager vmManager;

    public static final String OK_RESPONSE = "ok";

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
            action = RequestAction.fromString(actionStr);
        } catch (IllegalArgumentException e) {
            Logger.getLogger().log(Logger.Level.DEBUG, new RuntimeException("Illegal action in request", e));
            return TopLevelErrorCandidate.toError(e);
        }
        port = tryParseInt(portStr, "Listen port is not an integer!");
        vmPid = tryParseInt(vmPidStr, "VM PID is not a number!");

        if (vmPid >= 0 || port >= 0) {
            Logger.getLogger().log(
                    Logger.Level.DEBUG,
                    "Processing request. VM ID: " + vmId + ", PID: " + vmPid + ", action: " + action + ", port: " + portStr
            );
        } else {
            Logger.getLogger().log(Logger.Level.DEBUG, "Processing request. VM ID: " + vmId + ", action: " + action);
        }
        String response;
        switch (action) {
            case ADD_CLASS:
            case ADD_JAR:
            case OVERWRITE:
                String classNameForOverwrite = request.getParameter(AgentRequestAction.CLASS_NAME_PARAM);
                String classFutureBody = request.getParameter(AgentRequestAction.CLASS_TO_OVERWRITE_BODY);
                String classloader = request.getParameter(AgentRequestAction.CLASS_LOADER);
                response = getOverwriteAction(action, hostname, port, vmId, vmPid, classNameForOverwrite, classFutureBody, classloader);
                break;
            case REMOVE_OVERRIDES:
                String patern = request.getParameter(AgentRequestAction.CLASS_NAME_PARAM);
                response = getRemoveOverrideAction(hostname, port, vmId, vmPid, patern);
                break;
            case INIT_CLASS:
                String fqn = request.getParameter(AgentRequestAction.CLASS_NAME_PARAM);
                response = getInitAction(hostname, port, vmId, vmPid, fqn);
                break;
            case VERSION:
                response = getVersionAction(hostname, port, vmId, vmPid);
                break;
            case BYTES:
                String className = request.getParameter(AgentRequestAction.CLASS_NAME_PARAM);
                String bytesClassloader = request.getParameter(AgentRequestAction.CLASS_LOADER);
                response = getByteCodeAction(hostname, port, vmId, vmPid, className, bytesClassloader);
                break;
            case OVERRIDES:
            case CLASSES:
            case CLASSES_WITH_INFO:
                String listingClassloader = request.getParameter(AgentRequestAction.CLASS_LOADER);
                if (listingClassloader == null) {
                    response = getListAction(hostname, port, vmId, vmPid, action);
                } else {
                    response = getListActionLoader(hostname, port, vmId, vmPid, action, listingClassloader);
                }
                break;
            case SEARCH_CLASSES:
                String substringAndRegex = request.getParameter(AgentRequestAction.CLASS_NAME_PARAM);
                String searchClassloader = request.getParameter(AgentRequestAction.CLASS_LOADER);
                response = getListActionSearch(hostname, port, vmId, vmPid, action, substringAndRegex, searchClassloader);
                break;
            case HALT:
                response = getHaltAction(hostname, port, vmId, vmPid);
                break;
            default:
                String s = "Unknown action given: " + action;
                Logger.getLogger().log(Logger.Level.DEBUG, s);
                return TopLevelErrorCandidate.toError(s);
        }
        return response;

    }

    private String getListActionSearch(
            String hostname, int port, String vmId, int vmPid, RequestAction action, String substringAndRegex, String classloader
    ) {
        try {
            ResponseWithPort reply = getResponse(
                    hostname, port, vmId, vmPid,
                    action.toString() + (classloader == null ? "" : (" " + classloader)) + "\n" + substringAndRegex
            );
            ClassInfo[] arrayOfClasses = parseClasses(reply.response);
            Arrays.sort(arrayOfClasses, new ClassesComparator());

            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            status.setLoadedClasses(arrayOfClasses);

            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getListActionLoader(String hostname, int port, String vmId, int vmPid, RequestAction action, String listingClassloader) {
        try {
            ResponseWithPort reply = getResponse(hostname, port, vmId, vmPid, action.toString() + " " + listingClassloader);
            ClassInfo[] arrayOfClasses = parseClasses(reply.response);
            Arrays.sort(arrayOfClasses, new ClassesComparator());

            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            status.setLoadedClasses(arrayOfClasses);

            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private int tryParseInt(String intStr, String msg) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            Logger.getLogger().log(Logger.Level.DEBUG, msg);
            Logger.getLogger().log(Logger.Level.ALL, e);
            return AgentRequestAction.NOT_ATTACHED_PORT;
        }
    }

    private static int getPort(String hostname, int listenPort, String vmId, int vmPid, AgentAttachManager attachManager) {
        int actualListenPort;
        if (CallDecompilerAgent.DEFAULT_ADDRESS.equals(hostname)) {
            try {
                actualListenPort = checkIfAgentIsLoaded(listenPort, vmId, vmPid, attachManager);
            } catch (Exception ex) {
                throw ex;
            }
        } else {
            actualListenPort = listenPort;
        }
        if (actualListenPort == AgentRequestAction.NOT_ATTACHED_PORT) {
            throw new RuntimeException(
                    "Failed to attach agent. You must run the target process with '-Djdk.attach.allowAttachSelf=true'." +
                            "Or agent is already attached? Or Bad agent?"
            );
        }
        return actualListenPort;
    }

    private static class ResponseWithPort {
        private final String response;
        private final int port;

        ResponseWithPort(String response, int port) {
            this.response = response;
            this.port = port;
        }

    }

    private ResponseWithPort getResponse(String hostname, int listenPort, String vmId, int vmPid, String requestBody) {
        return getResponse(hostname, listenPort, vmId, vmPid, requestBody, attachManager, vmManager);
    }

    private static ResponseWithPort getResponse(
            String hostname, int listenPort, String vmId, int vmPid, String requestBody, AgentAttachManager attachManager,
            VmManager vmManager
    ) {
        int actualListenPort = -1;
        JrdAgent nativeAgent;
        if (listenPort >= 0 || vmPid >= 0) {
            actualListenPort = getPort(hostname, listenPort, vmId, vmPid, attachManager);
            nativeAgent = new CallDecompilerAgent(actualListenPort, hostname);
        } else {
            VmInfo vmInfo = vmManager.findVmFromPid(vmId);
            VmDecompilerStatus status = new VmDecompilerStatus();
            status.setHostname(CallDecompilerAgent.DEFAULT_ADDRESS);
            status.setListenPort(vmPid);
            status.setVmId(vmId);
            vmInfo.setVmDecompilerStatus(status);
            nativeAgent = FsAgent.get(vmInfo);
        }
        String reply = nativeAgent.submitRequest(requestBody);
        ErrorCandidate errorCandidate = new ErrorCandidate(reply);
        if (errorCandidate.isError()) {
            throw new RuntimeException(
                    "Agent returned error response '" + errorCandidate.getErrorMessage() + "' for request '" +
                            requestBody.replace("\n", "\\n") + "'."
            );
        }
        return new ResponseWithPort(reply, actualListenPort);
    }

    private String getOverwriteAction(
            RequestAction action, String hostname, int listenPort, String vmId, int vmPid, String className, String newBody,
            String classloader
    ) {
        try {
            ResponseWithPort reply = getResponse(
                    hostname, listenPort, vmId, vmPid,
                    action + "\n" + className + "\n" + newBody + (classloader == null ? "" : (" " + classloader))
            );
            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            // Note that we have no reply from overwrite. Or better, nothing to do with reply
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getVersionAction(String hostname, int listenPort, String vmId, int vmPid) {
        try {
            ResponseWithPort reply = getResponse(hostname, listenPort, vmId, vmPid, RequestAction.VERSION + "\n");
            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            status.setLoadedClassBytes(reply.response);
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.DEBUG, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getRemoveOverrideAction(String hostname, int listenPort, String vmId, int vmPid, String fqn) {
        return getNoReplyValue(hostname, listenPort, vmId, vmPid, fqn, RequestAction.REMOVE_OVERRIDES);
    }

    private String getInitAction(String hostname, int listenPort, String vmId, int vmPid, String fqn) {
        return getNoReplyValue(hostname, listenPort, vmId, vmPid, fqn, RequestAction.INIT_CLASS);
    }

    private String getNoReplyValue(String hostname, int listenPort, String vmId, int vmPid, String argument, RequestAction action) {
        try {
            ResponseWithPort reply = getResponse(hostname, listenPort, vmId, vmPid, action + "\n" + argument);
            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getByteCodeAction(String hostname, int listenPort, String vmId, int vmPid, String className, String classloader) {
        try {
            ResponseWithPort reply = getResponse(
                    hostname, listenPort, vmId, vmPid,
                    RequestAction.BYTES + (classloader == null ? "" : (" " + classloader)) + "\n" + className
            );
            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            status.setLoadedClassBytes(reply.response);
            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getListAction(String hostname, int listenPort, String vmId, int vmPid, RequestAction type) {
        try {
            ResponseWithPort reply = getResponse(hostname, listenPort, vmId, vmPid, type.toString());
            ClassInfo[] arrayOfClasses = parseClasses(reply.response);
            Arrays.sort(arrayOfClasses, new ClassesComparator());

            VmDecompilerStatus status = vmManager.getVmInfoByID(vmId).getVmDecompilerStatus();
            status.setHostname(hostname);
            status.setListenPort(reply.port);
            status.setVmId(vmId);
            status.setLoadedClasses(arrayOfClasses);

            vmManager.getVmInfoByID(vmId).replaceVmDecompilerStatus(status);
        } catch (Exception ex) {
            Logger.getLogger().log(Logger.Level.ALL, ex);
            return TopLevelErrorCandidate.toError(ex);
        }
        return OK_RESPONSE;
    }

    private String getHaltAction(String hostname, int listenPort, String vmId, int vmPid) {
        return getHaltAction(hostname, listenPort, vmId, vmPid, attachManager, vmManager, true);
    }

    public static String getHaltAction(
            String hostname, int listenPort, String vmId, int vmPid, AgentAttachManager attachManager, VmManager vmManager,
            boolean removeVmDecompilerStatus
    ) {
        try {
            getResponse(hostname, listenPort, vmId, vmPid, "HALT", attachManager, vmManager);
        } catch (Exception e) {
            Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Exception when calling halt action", e));
        } finally {
            KnownAgents.getInstance().markDead(hostname, listenPort, vmPid);
            if (removeVmDecompilerStatus) {
                vmManager.getVmInfoByID(vmId).removeVmDecompilerStatus();
            }
        }
        return OK_RESPONSE;
    }

    private static int checkIfAgentIsLoaded(int port, String vmId, int vmPid, AgentAttachManager attachManager) {
        if (port != AgentRequestAction.NOT_ATTACHED_PORT) {
            return port;
        }
        int actualListenPort = AgentRequestAction.NOT_ATTACHED_PORT;
        VmDecompilerStatus status = attachManager.attachAgentToVm(vmId, vmPid, AgentConfig.getDefaultSinglePermanentAgent());
        if (status != null) {
            actualListenPort = status.getListenPort();
        }
        return actualListenPort;
    }

    private ClassInfo[] parseClasses(String classes) {
        // filter: not null && backwards compatibility && name is not empty
        return Arrays.stream(classes.split(";")).filter(s -> s != null && !s.isEmpty() && !s.startsWith("|")).map(ClassInfo::new)
                .toArray(ClassInfo[]::new);
    }

    private static class ClassesComparator implements Comparator<ClassInfo>, Serializable {

        @SuppressWarnings({"ReturnCount", "CyclomaticComplexity"}) // comparator syntax
        @SuppressFBWarnings(
                value = "NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE",
                justification = "False report of possible NP dereference, despite testing both o1 & o2 for nullness."
        )
        @Override
        public int compare(ClassInfo c1, ClassInfo c2) {
            if (c1 == null && c2 == null) {
                return 0;
            }
            if (c1 == null && c2 != null) {
                return 1;
            }
            if (c1 != null && c2 == null) {
                return -1;
            }

            String o1 = c1.getName();
            String o2 = c2.getName();

            if (PluginManager.isArrayForm(o1) && !PluginManager.isArrayForm(o2)) {
                return 1;
            }
            if (!PluginManager.isArrayForm(o1) && PluginManager.isArrayForm(o2)) {
                return -1;
            }
            if (PluginManager.isUndecompilableLambda(o1) && !PluginManager.isUndecompilableLambda(o2)) {
                return 1;
            }
            if (!PluginManager.isUndecompilableLambda(o1) && PluginManager.isUndecompilableLambda(o2)) {
                return -1;
            }
            if (PluginManager.isLambdaForm(o1) && !PluginManager.isLambdaForm(o2)) {
                return 1;
            }
            if (!PluginManager.isLambdaForm(o1) && PluginManager.isLambdaForm(o2)) {
                return -1;
            }

            return o1.compareTo(o2);
        }
    }
}
