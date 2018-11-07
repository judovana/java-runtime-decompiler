/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jrd.backend.data;

import org.jboss.byteman.agent.install.VMInfo;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.frontend.MainFrameView;
import org.jrd.frontend.VmDecompilerInformationController;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author pmikova
 */
public class Main {
        
    
    public static void main(String[] args){
        Config configureAgent = Config.getConfig();
        VmManager manager = new VmManager();
        if (args.length == 0) {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(info.getClassName())) {
                    try {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
           MainFrameView mainView = new MainFrameView();
            VmDecompilerInformationController controller = new VmDecompilerInformationController(mainView, manager);
        } else {
            String LISTVMS="-listvms";
            String LISTCLASSES="-listclasses";
            for(int i= 0 ; i < args.length; i++){
                String arg = args[i];
                arg=arg.replaceAll("^--*", "-").toLowerCase();
                if (arg.equals(LISTVMS)){
                    if (args.length != 1 ){
                        throw new RuntimeException(LISTVMS+" do not expect argument");
                    }
                    for (VmInfo vm: manager.vmList){
                        System.out.println(vm.getVmPid() + " " + vm.getVmName());
                    }
                }
                if (arg.equals(LISTCLASSES)){
                    if (args.length != 2 ){
                        throw new RuntimeException(LISTCLASSES+" expect exactly one argument - pid or url");
                    }
                    String param = args[i+1];
                    try {
                        int pid = Integer.valueOf(param);
                        VmInfo vmInfo = null;
                        for (VmInfo vm: manager.vmList){
                          if (vm.getVmPid() == pid){
                              vmInfo = vm;
                              break;
                          }
                        }
                        if (vmInfo == null){
                            throw new RuntimeException("VM with pid of " + pid + " not found");
                        }
                        AgentRequestAction request = VmDecompilerInformationController.createRequest(manager, vmInfo, null, AgentRequestAction.RequestAction.CLASSES);
                        String response = VmDecompilerInformationController.submitRequest(manager, request);
                        if (response.equals("ok")) {
                            VmDecompilerStatus vmStatus = manager.getVmDecompilerStatus(vmInfo);
                            String[] classes = vmStatus.getLoadedClassNames();
                            for (String clazz: classes){
                                System.out.println(clazz);

                            }
                        }
                        if (response.equals("error")) {
                            System.out.println(VmDecompilerInformationController.CLASSES_NOPE);
                            throw new RuntimeException(VmDecompilerInformationController.CLASSES_NOPE);

                        }
                    }catch (NumberFormatException e){
                        try {
                            URL u = new URL(param);
                            System.out.println("Remote VM not yet implemeted");
                            throw new RuntimeException("Remote VM not yet implemeted");
                        }catch (MalformedURLException ee){
                            throw new RuntimeException("Second param was supposed to be URL or PID");
                        }
                    }

                }
            }
        }

    }

    
    // possibly some triggers to periodicaly refresh items in vm
    
}
