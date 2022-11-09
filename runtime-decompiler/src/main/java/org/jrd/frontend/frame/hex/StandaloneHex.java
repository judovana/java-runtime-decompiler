package org.jrd.frontend.frame.hex;

import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.frame.main.decompilerview.TextWithControls;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public class StandaloneHex extends JFrame {

    File lastOpened = new File(System.getProperty("user.dir"));

    public StandaloneHex(List<String> files) throws HeadlessException, IOException {
        super("JRD's hex diff and editor");
        this.setSize(900, 800);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final JTabbedPane tp = new JTabbedPane();
        for (String s : files) {
            JPanel wrapper = new FeatureFullHex(new File(s), tp, new HexWithControls(null));
            tp.add(wrapper);
        }
        JButton openHex = new JButton("Open file (hex)");
        openHex.setFont(openHex.getFont().deriveFont(Font.BOLD));
        JButton exit = new JButton("exit");
        JButton openText = new JButton("Open file (text)... just because we can, it do not mean it is good idea");
        final JPanel plus = new JPanel(new BorderLayout());
        plus.add(openHex, BorderLayout.NORTH);
        plus.add(exit, BorderLayout.CENTER);
        plus.add(openText, BorderLayout.SOUTH);
        plus.setName("+");
        tp.add(plus);
        this.add(tp);
        exit.addActionListener(a -> StandaloneHex.this.dispose());
        openHex.addActionListener(a -> {
            JFileChooser jFileChooser = new JFileChooser(lastOpened);
            int fo = jFileChooser.showOpenDialog(openHex);
            File nwf = jFileChooser.getSelectedFile();
            if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
                try {
                    FeatureFullHex ffh = new FeatureFullHex(nwf, tp, new HexWithControls(null));
                    extracted(tp, plus, nwf, ffh);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(openHex, ex.getMessage());
                }
            }
        });

        openText.addActionListener(a -> {
            JFileChooser jFileChooser = new JFileChooser(lastOpened);
            int fo = jFileChooser.showOpenDialog(openHex);
            File nwf = jFileChooser.getSelectedFile();
            if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
                try {
                    FeatureFullHex ffh = new FeatureFullHex(nwf, tp, new TextWithControls(null));
                    extracted(tp, plus, nwf, ffh);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(openHex, ex.getMessage());
                }
            }
        });
    }

    private void extracted(JTabbedPane tp, JPanel plus, File nwf, JComponent ffh) {
        tp.remove(plus);
        tp.add(ffh);
        tp.add(plus);
        lastOpened = nwf;
    }


}
