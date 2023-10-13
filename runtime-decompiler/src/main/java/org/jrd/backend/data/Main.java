package org.jrd.backend.data;

import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.cli.Cli;
import org.jrd.backend.data.cli.Help;
import org.jrd.frontend.frame.hex.StandaloneHex;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.MainFrameView;

import javax.swing.UIManager;

public class Main {

    public static void main(String[] allArgs) throws Exception {
        Cli cli = new Cli(allArgs);
        if (cli.isGui()) {
            setLookAndFeel();
            if (!cli.getFilteredArgs().isEmpty()) {
                Help.printHelpText();
                StandaloneHex hexview = new StandaloneHex(
                        cli.getFilteredArgs(), cli.isHex(), new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider()
                );
                hexview.setVisible(true);
            } else {
                if (cli.isHex() && cli.getFilteredArgs().isEmpty()) {
                    Help.printHelpText();
                    StandaloneHex hexview = new StandaloneHex(
                            cli.getFilteredArgs(), cli.isHex(), new ClassesAndMethodsProvider.SettingsClassesAndMethodsProvider()
                    );
                    hexview.setVisible(true);
                } else {
                    MainFrameView mainView = new MainFrameView();
                    DecompilationController dec = new DecompilationController(mainView, cli.shouldBeVerbose());
                    mainView.getBytecodeDecompilerView().setCompletionHelper(dec);
                }
            }
        } else {
            cli.consumeCli();
        }

    }

    public static void setLookAndFeel() {
        try {
            String laf = Config.getConfig().getLaF();
            if (laf == null) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(laf);
            }
        } catch (Exception e) {
            Logger.getLogger().log(Logger.Level.DEBUG, e);
        }
    }

}
