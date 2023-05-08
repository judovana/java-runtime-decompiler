package org.jrd.frontend.frame.hex;

import org.jrd.frontend.frame.main.decompilerview.LinesProvider;
import org.jrd.frontend.frame.main.popup.DiffPopup;
import org.jrd.frontend.utility.ImageButtonFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class FeatureFullHex extends JPanel {

    private final LinesProvider hex;

    public FeatureFullHex(final File f, final JTabbedPane parent, LinesProvider impl) throws IOException {
        this.setLayout(new BorderLayout());
        JPanel tool = new JPanel();
        tool.setLayout(new GridLayout(1, 6));
        JButton undo = ImageButtonFactory.createUndoButton();
        tool.add(undo);
        JButton redo = ImageButtonFactory.createRedoButton();
        tool.add(redo);
        JButton diff = new JButton("Diff");
        diff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new DiffPopup(toLines(parent), Optional.empty(), true).show(diff, 0, 0);
            }

            private List<LinesProvider> toLines(JTabbedPane parent) {
                //selcted have to be first, so the  onlyOne can do its job proeprly
                int selected = parent.getSelectedIndex();
                Component[] comps = parent.getComponents();
                List<LinesProvider> r = new ArrayList<>();
                for (int i = selected; i < comps.length - 1/*the plus button*/; i++) {
                    LinesProvider featureFullHex = (LinesProvider) (((JPanel) comps[i]).getComponents()[1]);
                    r.add(featureFullHex);
                }
                for (int i = 0; i < selected; i++) {
                    LinesProvider featureFullHex = (LinesProvider) (((JPanel) comps[i]).getComponents()[1]);
                    r.add(featureFullHex);
                }
                return r;
            }
        });
        tool.add(diff);
        JButton save = new JButton("Save");
        JButton open = new JButton("Open");
        JButton close = new JButton("Close");
        tool.add(save);
        tool.add(open);
        tool.add(close);
        this.add(tool, BorderLayout.NORTH);
        hex = impl;
        if (f != null) {
            this.setName(f.getName());
            hex.setFile(f);
            hex.open(f);
        } else {
            File ff = StandaloneHex.getNext();
            this.setName(ff.getName());
            hex.setFile(ff);
        }
        this.add(hex.asComponent(), BorderLayout.CENTER);
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent a) {
                JFileChooser jFileChooser = new JFileChooser(hex.getFile());
                int fo = jFileChooser.showSaveDialog(hex.asComponent());
                File nwf = jFileChooser.getSelectedFile();
                if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
                    try {
                        hex.save(nwf);
                        hex.setFile(nwf);
                        FeatureFullHex.this.setName(nwf.getName());
                        updateTitles(parent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(hex.asComponent(), ex.getMessage());
                    }
                }
            }
        });
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent a) {
                JFileChooser jFileChooser = new JFileChooser(hex.getFile());
                int fo = jFileChooser.showOpenDialog(hex.asComponent());
                File nwf = jFileChooser.getSelectedFile();
                if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
                    try {
                        hex.setFile(nwf);
                        hex.open(nwf);
                        FeatureFullHex.this.setName(nwf.getName());
                        updateTitles(parent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(hex.asComponent(), ex.getMessage());
                    }
                }
            }
        });
        close.addActionListener(a -> {
            FeatureFullHex.this.hex.close();
            parent.remove(FeatureFullHex.this);
        });
        undo.addActionListener(a -> {
            hex.undo();
        });
        redo.addActionListener(a -> {
            hex.redo();
        });
    }

    private static void updateTitles(JTabbedPane parent) {
        for (int x = 0; x < parent.getComponentCount(); x++) {
            parent.setTitleAt(x, parent.getComponent(x).getName());
        }
    }
}
