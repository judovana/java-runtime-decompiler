package org.jrd.backend.data;

import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.DecompilationController;
import org.jrd.frontend.frame.main.MainFrameView;

public class Main {

    public static void main(String[] allArgs) throws Exception {
        Model model = new Model();
        Cli cli = new Cli(allArgs, model);
        if (cli.shouldBeVerbose()) {
            Logger.getLogger().setVerbose();
        }
        if (cli.isGui()) {
            setLookAndFeel();
            MainFrameView mainView = new MainFrameView();
            new DecompilationController(mainView, model);
        } else {
            cli.consumeCli();
        }

    }

    public static void setLookAndFeel() {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(info.getClassName())) {
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
