package org.jrd.frontend;

import org.jc.api.ClassesProvider;
import org.jc.api.IdentifiedSource;
import org.jc.api.InMemoryCompiler;
import org.jrd.backend.communication.RuntimeCompilerConnector;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrame.RewriteClassDialog;
import org.jrd.frontend.MainFrame.VmDecompilerInformationController;

import java.io.File;
import java.nio.file.Files;

public class Utils {
    public static int FULLY_QUALIFIED_NAME = 0;
    public static int SRC_SUBDIRS_NAME = 1;
    public static int CUSTOM_NAME = 2;

    public static interface  StatusKeeper {
        public void setText(String s);
        public void onException(Exception ex);
    }

    public static boolean saveByGui(String fileNameBase, int naming, String suffix, StatusKeeper status, String clazz, byte[] content) {
        String name = "???";
        String ss = "Error to save: ";
        boolean r = true;
        try {
            name = cheatName(fileNameBase, naming, suffix, clazz);
            File f = new File(name);
            if (naming == SRC_SUBDIRS_NAME) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), content);
            ss = "Saved: ";
        } catch (Exception ex) {
            status.onException(ex);
            r = false;
        }
        status.setText(ss + name);
        return r;
    }

    public static boolean uploadByGui(VmInfo vmInfo, VmManager vmManager, StatusKeeper status, String clazz, byte[] content) {
        String name = "???";
        String ss = "Error to upload: ";
        boolean r = true;
        try {
            String respomse = uploadBytecode(clazz, vmManager, vmInfo, content);
            if (respomse.equals(DecompilerRequestReceiver.ERROR_RESPONSE)) {
                throw new Exception("Agent returned error");
            }
            ss = "uploaded: ";
        } catch (Exception ex) {
            status.onException(ex);
            r = false;
        }
        status.setText(ss + clazz);
        return r;
    }

    public static String cheatName(String base, int selectedIndex, String suffix, String fullyClasifiedName) {
        if (selectedIndex == CUSTOM_NAME) {
            return base;
        }
        if (selectedIndex == FULLY_QUALIFIED_NAME) {
            return base + "/" + fullyClasifiedName + suffix;
        }
        if (selectedIndex == SRC_SUBDIRS_NAME) {
            return base + "/" + fullyClasifiedName.replaceAll("\\.", "/") + suffix;
        }
        throw new RuntimeException("Unknown name target " + selectedIndex);
    }


    public static String uploadBytecode(String clazz, VmManager vmManager, VmInfo vmInfo, byte[] bytes) {
        final String body = VmDecompilerInformationController.bytesToBase64(bytes);
        AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo, AgentRequestAction.RequestAction.OVERWRITE, clazz, body);
        return VmDecompilerInformationController.submitRequest(vmManager, request);
    }




}
