/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jrd.backend.data;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.OutputController;
import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.decompiling.DecompilerWrapperInformation;
import org.jrd.backend.decompiling.PluginManager;
import org.jrd.frontend.MainFrameView;
import org.jrd.frontend.VmDecompilerInformationController;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author pmikova
 */
public class Main {


    public static void main(String[] allargs) throws Exception {
        Config configureAgent = Config.getConfig();
        VmManager manager = new VmManager();
        Cli cli = new Cli(allargs, manager);
        if (cli.shouldBeVerbose()){
            OutputController.getLogger().setVerbose();
        }
        if (cli.isGui()) {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(info.getClassName())) {
                    try {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                        OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, e);
                    }
                    break;
                }
            }
            MainFrameView mainView = new MainFrameView();
            VmDecompilerInformationController controller = new VmDecompilerInformationController(mainView, manager);
        } else {
            cli.consumeCli();
        }

    }

    // possibly some triggers to periodicaly refresh items in vm

}
