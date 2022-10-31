package org.jrd.backend.data;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.cli.Cli;
import org.jrd.frontend.frame.main.decompilerview.DecompilationController;
import org.jrd.frontend.frame.main.MainFrameView;
import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.utility.ImageButtonFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class Main {

    public static void main(String[] allArgs) throws Exception {
        Model model = new Model();
        Cli cli = new Cli(allArgs, model);
        if (cli.isGui()) {
            setLookAndFeel();
            if (cli.isHex()) {
                //FIXME open jsut hex window(s) in tab pane (copy should work)
                //undo will need reimplement
                //todo, handle input files :D with them, isGui do not work :D
                if (true) {
                    System.err.println("standalon hex editor nto yet fully enaabled");
                } else {
                    //todo move wrapper to class
                    JFrame hexview = new JFrame("JRD's hex diff and editor");
                    hexview.setSize(800, 600);
                    hexview.setLocationRelativeTo(null);
                    hexview.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    JPanel wrapper = new JPanel();
                    wrapper.setName("some.file");
                    wrapper.setLayout(new BorderLayout());
                    JPanel tool = new JPanel();
                    tool.setLayout(new GridLayout(1, 5));
                    tool.add(ImageButtonFactory.createUndoButton());
                    tool.add(ImageButtonFactory.createRedoButton());
                    tool.add(new JButton("Diff"));
                    tool.add(new JButton("Save"));
                    tool.add(new JButton("Open"));
                    wrapper.add(tool, BorderLayout.NORTH);
                    HexWithControls hex = new HexWithControls("some.class");
                    wrapper.add(hex, BorderLayout.CENTER);
                    JTabbedPane tp = new JTabbedPane();
                    tp.add(wrapper);
                    hexview.add(tp);
                    hexview.setVisible(true);
                }
            } else {
                MainFrameView mainView = new MainFrameView();
                new DecompilationController(mainView, model, cli.shouldBeVerbose());
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
