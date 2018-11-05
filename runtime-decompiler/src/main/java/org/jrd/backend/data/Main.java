/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jrd.backend.data;

import org.jboss.byteman.agent.install.VMInfo;
import org.jrd.frontend.MainFrameView;
import org.jrd.frontend.VmDecompilerInformationController;

import javax.swing.*;

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
            for(String arg:args){
                arg=arg.replaceAll("^--*", "-");
                if (arg.equals(LISTVMS)){
                    if (args.length != 1 ){
                        throw new RuntimeException(LISTVMS+" do not expect argument");
                    }
                    for (VmInfo vm: manager.vmList){
                        System.out.println(vm.getVmPid() + " " + vm.getVmName());
                    }
                }
            }
        }

    }

    
    // possibly some triggers to periodicaly refresh items in vm
    
}
