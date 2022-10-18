package org.jrd.frontend.frame.main.decompilerview;

import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.hex.swing.HexTableModel;
import org.jrd.backend.core.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HexWithControls extends JPanel implements LinesProvider {

    private static byte[] fakeClip;

    private final HexEditor hex;
    private SearchControlsPanel hexSearchControls;

    public HexWithControls(String title) {
        initTabLayers(this, title);
        hex = createHexArea();
        this.add(hex);
        hexSearchControls = SearchControlsPanel.createHexControls(hex);
        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.add(hexSearchControls, BorderLayout.CENTER);
        JPanel southeEastPanel = new JPanel(new BorderLayout());
        JButton copy = new JButton("copy");
        copy.addActionListener(a -> fakeClip = hex.get());
        southeEastPanel.add(copy, BorderLayout.WEST);
        JButton paste = new JButton("paste");
        paste.addActionListener(a -> {
            try {
                hex.set(fakeClip);
            } catch (Exception ex) {
                Logger.getLogger().log(ex);
            }
        });
        southeEastPanel.add(paste, BorderLayout.EAST);
        southWrapper.add(southeEastPanel, BorderLayout.EAST);
        String hint = BytecodeDecompilerView.styleTooltip() + "This is simple helper to trasnfer bytebuffers without clipboard.<br>" +
                "Undo/redo should still work.";
        paste.setToolTipText(hint);
        copy.setToolTipText(hint);
        this.add(southWrapper, BorderLayout.SOUTH);
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

    @Override
    public List<String> getLines(LinesFormat type) {
        if (type == LinesFormat.CHARS) {
            StringBuilder sb = new StringBuilder();
            for (byte b : hex.get()) {
                sb.append(HexTableModel.byteToAsci(b));
            }
            return split(sb.toString(), 16);
        } else {
            return bytesToStrings(hex.get());
        }
    }

    public static List<String> bytesToStrings(byte[] bytes) {
        //TODO, move to utils
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (byte b : bytes) {
            if (count % 16 != 0) {
                sb.append(" ");
            }
            sb.append(HexTableModel.byteToHexString(b));
            count++;
        }
        return split(sb.toString(), 32 + 15);
    }

    public static List<String> split(String text, int n) {
        String[] results = text.split("(?<=\\G.{" + n + "})");
        return Arrays.asList(results);
    }

    @Override
    public void setLines(LinesFormat type, List<String> nwContent) throws Exception {
        if (type == LinesFormat.CHARS) {
            throw new RuntimeException("only hex can be pasted in");
        }
        hex.set(hexToBytes(hexLinesToHexString(nwContent)));
    }

    public static String hexLinesToHexString(List<String> s) {
        return s.stream().collect(Collectors.joining(""));
    }

    public static byte[] hexToBytes(String s) {
        s = s.replaceAll("\\s+", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
