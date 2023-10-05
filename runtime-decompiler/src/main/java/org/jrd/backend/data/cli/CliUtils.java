package org.jrd.backend.data.cli;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.frontend.frame.filesystem.NewFsVmController;

public final class CliUtils {

    private CliUtils() {
    }

    public static VmInfo.Type guessType(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Unable to interpret PUC because it is empty.");
        }

        try {
            Integer.valueOf(input);
            Logger.getLogger().log("Interpreting '" + input + "' as PID. To use numbers as filenames, try './" + input + "'.");
            return VmInfo.Type.LOCAL;
        } catch (NumberFormatException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as PID failed because it is not a number.");
        }

        try {
            if (input.split(":").length == 2) {
                Integer.valueOf(input.split(":")[1]);
                Logger.getLogger().log("Interpreting '" + input + "' as hostname:port. To use colons as filenames, try './" + input + "'.");
                return VmInfo.Type.REMOTE;
            } else {
                Logger.getLogger()
                        .log("Interpretation of '" + input + "' as hostname:port failed because it cannot be correctly split on ':'.");
            }
        } catch (NumberFormatException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as hostname:port failed because port is not a number.");
        }

        try {
            NewFsVmController.cpToFiles(input);
            Logger.getLogger().log("Interpreting " + input + " as FS VM classpath.");
            return VmInfo.Type.FS;
        } catch (NewFsVmController.InvalidClasspathException e) {
            Logger.getLogger().log("Interpretation of '" + input + "' as FS VM classpath. failed. Cause: " + e.getMessage());
        }

        throw new RuntimeException("Unable to interpret '" + input + "' as any component of PUC.");
    }

    public static VmInfo getVmInfo(String param, VmManager vmManager) {
        VmInfo.Type puc = guessType(param);
        switch (puc) {
            case LOCAL:
                return vmManager.findVmFromPid(param);
            case FS:
                return vmManager.createFsVM(NewFsVmController.cpToFilesCaught(param), null, false);
            case REMOTE:
                String[] hostnamePort = param.split(":");
                return vmManager.createRemoteVM(hostnamePort[0], Integer.parseInt(hostnamePort[1]), false);
            default:
                throw new RuntimeException("Unknown VmInfo.Type.");
        }
    }

    public static String cleanParameter(String param) {
        if (param.startsWith("-")) {
            param = param.replaceAll("^--*", "-");
            if ("-R".equals(param)) {
                return param;
            } else {
                return param.toLowerCase();
            }
        }

        return param; // do not make regexes and filenames lowercase
    }

    public static String invalidityToString(boolean invalidWrapper) {
        if (invalidWrapper) {
            return "invalid";
        } else {
            return "valid";
        }
    }

    public static void returnNonzero(int failures, int total) {
        if (total == 0) {
            throw new RuntimeException("No class found to save.");
        }
        if (failures > 0) {
            throw new RuntimeException("Failed to save " + failures + "classes.");
        }
    }

}
