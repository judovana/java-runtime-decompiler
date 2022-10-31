package org.jrd.frontend.frame.hex;

import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.utility.ImageButtonFactory;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JPanel;

public class FeatureFullHex extends JPanel {

    public FeatureFullHex(File f) throws IOException {
        this.setName(f.getName());
        this.setLayout(new BorderLayout());
        JPanel tool = new JPanel();
        tool.setLayout(new GridLayout(1, 6));
        tool.add(ImageButtonFactory.createUndoButton());
        tool.add(ImageButtonFactory.createRedoButton());
        tool.add(new JButton("Diff"));
        tool.add(new JButton("Save"));
        tool.add(new JButton("Open"));
        tool.add(new JButton("Close"));
        this.add(tool, BorderLayout.NORTH);
        HexWithControls hex = new HexWithControls("some.class");
        hex.open(Files.readAllBytes(f.toPath()));
        this.add(hex, BorderLayout.CENTER);
    }
}
