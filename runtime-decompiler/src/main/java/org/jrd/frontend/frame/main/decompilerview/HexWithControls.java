package org.jrd.frontend.frame.main.decompilerview;

import org.fife.ui.hex.swing.HexEditor;
import org.jrd.backend.core.Logger;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HexWithControls extends JPanel {

    private final HexEditor hex;
    private SearchControlsPanel hexSearchControls;

    public HexWithControls(String title) {
        initTabLayers(this, title);
        hex = createHexArea();
        this.add(hex);
        hexSearchControls = SearchControlsPanel.createHexControls(hex);
        this.add(hexSearchControls, BorderLayout.SOUTH);
    }

    public byte[] get() {
        return hex.get();
    }

    public void undo() {
        hex.undo();
    }

    public void redo() {
        hex.redo();
    }

    public void open(byte[] source) {
        try {
            hex.open(new ByteArrayInputStream(source));
        } catch (IOException ex) {
            Logger.getLogger().log(ex);
        }
    }

    private HexEditor createHexArea() {
        HexEditor lhex = new HexEditor();
        lhex.addKeyListenerToTable(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    hexSearchControls.clickNextButton();
                }
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        hexSearchControls.focus();
                    }
                }
            }
        });
        return lhex;
    }

    public static void initTabLayers(JPanel p, String title) {
        p.setName(title);
        p.setLayout(new BorderLayout());
        p.setBorder(new EtchedBorder());
    }

}
