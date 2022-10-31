package org.jrd.frontend.frame.hex;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public class StandaloneHex extends JFrame {

    public StandaloneHex(List<String> files) throws HeadlessException, IOException {
        super("JRD's hex diff and editor");
        this.setSize(900, 800);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JTabbedPane tp = new JTabbedPane();
        for (String s : files) {
            JPanel wrapper = new FeatureFullHex(new File(s));
            tp.add(wrapper);
        }
        JButton open = new JButton("Open file");
        JButton exit = new JButton("exit");
        JPanel plus = new JPanel(new BorderLayout());
        plus.add(open, BorderLayout.NORTH);
        plus.add(exit, BorderLayout.SOUTH);
        plus.setName("+");
        tp.add(plus);
        this.add(tp);
    }

}
