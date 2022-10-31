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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Main {

    public static void main(String[] allArgs) throws Exception {
        Model model = new Model();
        Cli cli = new Cli(allArgs, model);
        if (cli.isGui()) {
            setLookAndFeel();
            if (cli.isHex()) {
                System.err.println("standalon hex editor nto yet fully enaabled");
                //todo move wrapper to class
                JFrame hexview = new JFrame("JRD's hex diff and editor");
                hexview.setSize(800, 600);
                hexview.setLocationRelativeTo(null);
                hexview.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                JTabbedPane tp = new JTabbedPane();
                for (String s : cli.getFilteredArgs()) {
                    JPanel wrapper = getFeatureFullTab(new File(s));
                    tp.add(wrapper);
                }
                JButton open = new JButton("Open file");
                JButton exit = new JButton("exit");
                JPanel plus = new JPanel(new BorderLayout());
                plus.add(open, BorderLayout.NORTH);
                plus.add(exit, BorderLayout.SOUTH);
                plus.setName("+");
                tp.add(plus);
                hexview.add(tp);
                hexview.setVisible(true);
            } else {
                MainFrameView mainView = new MainFrameView();
                new DecompilationController(mainView, model, cli.shouldBeVerbose());
            }
        } else {
            cli.consumeCli();
        }

    }

    private static JPanel getFeatureFullTab(File f) throws IOException {
        JPanel wrapper = new JPanel();
        wrapper.setName(f.getName());
        wrapper.setLayout(new BorderLayout());
        JPanel tool = new JPanel();
        tool.setLayout(new GridLayout(1, 6));
        tool.add(ImageButtonFactory.createUndoButton());
        tool.add(ImageButtonFactory.createRedoButton());
        tool.add(new JButton("Diff"));
        tool.add(new JButton("Save"));
        tool.add(new JButton("Open"));
        tool.add(new JButton("Close"));
        wrapper.add(tool, BorderLayout.NORTH);
        HexWithControls hex = new HexWithControls("some.class");
        hex.open(Files.readAllBytes(f.toPath()));
        wrapper.add(hex, BorderLayout.CENTER);
        return wrapper;
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
