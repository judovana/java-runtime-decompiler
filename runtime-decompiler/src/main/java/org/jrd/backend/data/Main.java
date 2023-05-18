package org.jrd.backend.data;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.cli.Cli;
import org.jrd.backend.data.cli.Help;
import org.jrd.frontend.frame.hex.StandaloneHex;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.MainFrameView;

public class Main {

    public static void main(String[] allArgs) throws Exception {
        Model model = new Model();
        Cli cli = new Cli(allArgs, model);
        if (cli.isGui()) {
            setLookAndFeel();
            if (!cli.getFilteredArgs().isEmpty()) {
                Help.printHelpText();
                StandaloneHex hexview = new StandaloneHex(cli.getFilteredArgs(), cli.isHex(),
                        null/*FIXME base on FS additional cp/sp from settings*/);
                hexview.setVisible(true);
            } else {
                if (cli.isHex() && cli.getFilteredArgs().isEmpty()) {
                    Help.printHelpText();
                    StandaloneHex hexview = new StandaloneHex(cli.getFilteredArgs(), cli.isHex(),
                            null/*FIXME base on FS additional cp/sp from settings*/);
                    hexview.setVisible(true);
                } else {
                    MainFrameView mainView = new MainFrameView();
                    new DecompilationController(mainView, model, cli.shouldBeVerbose());
                }
            }
        } else {
            cli.consumeCli();
        }

    }

    public static void setLookAndFeel() {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(info.getClassName()) ||
                    Directories.isOsWindows() && "com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(info.getClassName())) {
                try {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                } catch (Exception e) {
                    Logger.getLogger().log(Logger.Level.DEBUG, e);
                }
                break;
            }
        }
    }

}
