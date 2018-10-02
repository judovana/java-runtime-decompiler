/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jrd.backend.data;

import org.jrd.frontend.MainFrameView;
import org.jrd.frontend.VmDecompilerInformationController;

import javax.swing.*;

/**
 *
 * @author pmikova
 */
public class Main {
        
    
    public static void main(String[] args){
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("com.sun.java.frontend.plaf.gtk.GTKLookAndFeel".equals(info.getClassName())) {
                try {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        Config configureAgent = Config.getConfig();
        VmManager manager = new VmManager();
        MainFrameView mainView = new MainFrameView();
        VmDecompilerInformationController controller = new VmDecompilerInformationController(mainView, manager);
        
    }

    
    // possibly some triggers to periodicaly refresh items in vm
    
}
