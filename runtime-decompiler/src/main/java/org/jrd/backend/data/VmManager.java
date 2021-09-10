package org.jrd.backend.data;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.jrd.backend.core.Logger;
import org.jrd.backend.core.VmDecompilerStatus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is used for creating/removing/updating information about available Java Virtual Machines.
 */
public class VmManager {

    private Set<VmInfo> vmInfoSet;
    private Set<ActionListener> actionListeners = new HashSet<>();
    boolean changed;

    public VmManager() {
        this.vmInfoSet = new HashSet<>();
        updateLocalVMs();
        loadSavedFsVms();

        Thread vmUpdateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Logger.getLogger().log(e);
                }
                updateLocalVMs();
            }
        });
        vmUpdateThread.setDaemon(true);
        vmUpdateThread.start();
    }

    private void loadSavedFsVms() {
        List<VmInfo> savedFsVms;
        try {
            savedFsVms = Config.getConfig().getSavedFsVms();
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger().log(Logger.Level.ALL, "Failed to load saved FS VMs. Cause: ");
            Logger.getLogger().log(Logger.Level.ALL, e);
            return;
        }

        if (savedFsVms.isEmpty()) {
            Logger.getLogger().log("No saved FS VMs to load.");
            return;
        }

        // re-adjust IDs for saved VMs to be at the top of the list
        for (VmInfo savedFsVm : savedFsVms) {
            savedFsVm.setVmPid(getNextAvailableFsVmPid());
            savedFsVm.setVmId(String.valueOf(getNextAvailableFsVmPid()));

            vmInfoSet.add(savedFsVm);
        }

        setChanged();
        notifyListeners();
    }

    /**
     * Obtains list of Virtual Machines.
     * This list is then compared to vmInfoSet. Old Vms are removed and new are added.
     */
    public void updateLocalVMs() {
        Set<VmInfo> newVmInfoSet = new HashSet<>();

        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String vmId = descriptor.id();
            String processId = descriptor.id();
            int pid = Integer.parseInt(processId);
            String name = descriptor.displayName();
            newVmInfoSet.add(new VmInfo(vmId, pid, name, VmInfo.Type.LOCAL, null));
        }

        // Add all new VMs.
        newVmInfoSet.forEach(vmInfo -> {
            boolean isNewVm = vmInfoSet.stream().noneMatch(vmInfo1 -> vmInfo1.getVmId().equals(vmInfo.getVmId()));
            if (isNewVm) {
                setChanged();
                vmInfoSet.add(vmInfo);
            }
        });

        // Remove old VMs that are no longer available.
        Iterator<VmInfo> iterator = vmInfoSet.iterator();
        Set<VmInfo> forRemoval = new HashSet<>();
        while (iterator.hasNext()) {
            VmInfo vmInfo = iterator.next();
            boolean noLongerExists = newVmInfoSet
                    .stream()
                    .noneMatch(info -> info.getVmId().equals(vmInfo.getVmId()));

            if (vmInfo.getType() == VmInfo.Type.LOCAL && noLongerExists) {
                setChanged();
                forRemoval.add(vmInfo);
            }
        }
        vmInfoSet.removeAll(forRemoval);
        notifyListeners();
    }

    public VmInfo createRemoteVM(String hostname, int port) {
        String id = UUID.randomUUID().toString();
        VmInfo vmInfo = new VmInfo(id, 0, hostname, VmInfo.Type.REMOTE, null);
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setVmId(id);
        status.setHostname(hostname);
        status.setListenPort(port);
        vmInfo.setVmDecompilerStatus(status);
        vmInfoSet.add(vmInfo);
        setChanged();
        notifyListeners();
        return vmInfo;
    }


    public VmInfo createFsVM(List<File> cp, String name, boolean shouldBeSaved) {
        int pid = getNextAvailableFsVmPid();
        String stringPid = String.valueOf(pid);

        VmInfo vmInfo = new VmInfo(stringPid, pid, name, VmInfo.Type.FS, cp);
        VmDecompilerStatus status = new VmDecompilerStatus();
        status.setVmId(stringPid);
        status.setHostname(null);
        status.setListenPort(pid);
        vmInfo.setVmDecompilerStatus(status);
        vmInfoSet.add(vmInfo);

        if (shouldBeSaved) {
            try {
                Config.getConfig().addSavedFsVm(vmInfo);
                Config.getConfig().saveConfigFile();
            } catch (IOException e) {
                Logger.getLogger().log(Logger.Level.ALL, "Failed to save FS VM '" + vmInfo + "'.");
                Logger.getLogger().log(Logger.Level.ALL, e);
            }
        }

        setChanged();
        notifyListeners();
        return vmInfo;
    }

    private int getNextAvailableFsVmPid() {
        return vmInfoSet.stream()
                .filter(vmInfo -> vmInfo.getType().equals(VmInfo.Type.FS))
                .map(VmInfo::getVmPid)
                .min(Comparator.naturalOrder())
                .orElse(0) - 1;
    }

    public boolean removeVm(VmInfo target) {
        boolean removed = vmInfoSet.remove(target);

        setChanged();
        notifyListeners();

        return removed;
    }

    public VmInfo findVmFromPid(String param) {
        VmInfo result = findVmFromPidNoException(param);

        if (result == null) {
            throw new RuntimeException("VM with pid of " + param + " not found");
        }

        return result;
    }

    public VmInfo findVmFromPidNoException(String param) {
        int pid = Integer.parseInt(param);
        for (VmInfo vmInfo : vmInfoSet) {
            if (vmInfo.getVmPid() == pid) {
                return vmInfo;
            }
        }

        return null;
    }

    public VmInfo getVmInfoByID(String vmId) {
        for (VmInfo vmInfo : vmInfoSet) {
            if (vmInfo.getVmId().equals(vmId)) {
                return vmInfo;
            }
        }
        throw new NoSuchElementException("VmInfo with VmID" + vmId + "does no exist in VmList");
    }

    public Set<VmInfo> getVmInfoSet() {
        return new HashSet<>(this.vmInfoSet);
    }

    public void subscribeToVMChange(ActionListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        actionListeners.add(listener);
    }

    public void notifyListeners() {
        if (hasChanged()) {
            clearChanged();
            for (ActionListener listener: actionListeners) {
                listener.actionPerformed(new ActionEvent(this, 0, null));
            }
        }
    }

    private boolean hasChanged() {
        return changed;
    }

    private void setChanged() {
        changed = true;
    }

    private void clearChanged() {
        changed = false;
    }
}
