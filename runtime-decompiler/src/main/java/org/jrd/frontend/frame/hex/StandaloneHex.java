package org.jrd.frontend.frame.hex;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jrd.backend.completion.ClassesAndMethodsProvider;
import org.jrd.frontend.frame.main.decompilerview.HexWithControls;
import org.jrd.frontend.frame.main.decompilerview.LinesProvider;
import org.jrd.frontend.frame.main.decompilerview.TextWithControls;
import org.jrd.frontend.frame.settings.SettingsView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class StandaloneHex extends JFrame {

    private static int counter = 0;
    File lastOpened = new File(System.getProperty("user.dir"));
    private boolean treatAllTabsAsOneBatch = false;
    private final JTabbedPane mainTabs;

    public StandaloneHex(List<String> files, boolean hex, ClassesAndMethodsProvider classesAndMethodsProvider)
            throws HeadlessException, IOException {
        super("JRD's hex diff and editor");
        this.setSize(900, 800);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainTabs = new JTabbedPane();
        for (String s : files) {
            JPanel wrapper;
            if (hex) {
                wrapper = new FeatureFullHex(new File(s), mainTabs, new HexWithControls(null));
            } else {
                wrapper = new FeatureFullHex(
                        new File(s), mainTabs,
                        new TextWithControls(
                                null, SyntaxConstants.SYNTAX_STYLE_JAVA, TextWithControls.CodeCompletionType.STANDALONE,
                                classesAndMethodsProvider
                        )
                );
            }
            mainTabs.add(wrapper);
        }
        JPanel topButtons = new JPanel(new GridLayout(1, 2));
        JButton openHex = new JButton("Open file (hex)");
        openHex.setFont(openHex.getFont().deriveFont(Font.BOLD));
        JButton openEmptyHex = new JButton("Open empty hex");
        topButtons.add(openEmptyHex, BorderLayout.WEST);
        topButtons.add(openHex, BorderLayout.EAST);
        JPanel lowButtons = new JPanel(new GridLayout(1, 2));
        JButton openText = new JButton("Open file (text)... just because we can, it do not mean it is good idea");
        JButton openEmptyText = new JButton("Open empty text");
        openEmptyText.setFont(openEmptyText.getFont().deriveFont(Font.BOLD));
        lowButtons.add(openEmptyText, BorderLayout.WEST);
        lowButtons.add(openText, BorderLayout.EAST);
        final JButton exit = new JButton("exit");
        JLabel hint = new JLabel(
                "<html><div style='text-align: center;'>welcome to Jrd's standalone hex and texts</div></html>", SwingConstants.CENTER
        );
        final JButton settings = new JButton("jrd's settings");
        final JPanel plus = new JPanel(new BorderLayout());
        plus.add(topButtons, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(settings, BorderLayout.NORTH);
        center.add(hint, BorderLayout.CENTER);
        center.add(exit, BorderLayout.SOUTH);
        plus.add(center, BorderLayout.CENTER);
        plus.add(lowButtons, BorderLayout.SOUTH);
        plus.setName("+");
        mainTabs.add(plus);
        this.add(mainTabs);
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new SettingsView(StandaloneHex.this);
            }
        });
        exit.addActionListener(a -> StandaloneHex.this.dispose());
        openHex.addActionListener(a -> {
            addMainPanel(mainTabs, openHex, plus, new HexWithControls(null));
        });

        openText.addActionListener(a -> {
            addMainPanel(
                    mainTabs, openHex, plus,
                    new TextWithControls(
                            null, SyntaxConstants.SYNTAX_STYLE_JAVA, TextWithControls.CodeCompletionType.STANDALONE,
                            classesAndMethodsProvider
                    )
            );
        });

        openEmptyHex.addActionListener(a -> {
            addEmptyMainPanel(mainTabs, openHex, plus, new HexWithControls(null));
        });

        openEmptyText.addActionListener(a -> {
            addEmptyMainPanel(
                    mainTabs, openHex, plus,
                    new TextWithControls(
                            null, SyntaxConstants.SYNTAX_STYLE_JAVA, TextWithControls.CodeCompletionType.STANDALONE,
                            classesAndMethodsProvider
                    )
            );
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                for (Component c : mainTabs.getComponents()) {
                    if (c instanceof FeatureFullHex) {
                        ((FeatureFullHex) c).removeCodecompletion();
                    }
                }
            }
        });
    }

    public static File getNext() {
        counter++;
        return new File(System.getProperty("user.home") + File.separator + "file" + counter);
    }

    private void addMainPanel(JTabbedPane tp, JButton openHex, JPanel plus, final LinesProvider lp) {
        JFileChooser jFileChooser = new JFileChooser(lastOpened);
        int fo = jFileChooser.showOpenDialog(openHex);
        File nwf = jFileChooser.getSelectedFile();
        if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
            try {
                FeatureFullHex ffh = new FeatureFullHex(nwf, tp, lp);
                movePlus(tp, plus, nwf, ffh);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(openHex, ex.getMessage());
            }
        }
    }

    private void movePlus(JTabbedPane tp, JPanel plus, File nwf, JComponent ffh) {
        final int i = tp.getSelectedIndex();
        tp.remove(plus);
        tp.add(ffh);
        tp.add(plus);
        if (nwf != null) {
            lastOpened = nwf;
        }
        tp.setSelectedIndex(i);
    }

    private void addEmptyMainPanel(JTabbedPane tp, JButton openHex, JPanel plus, final LinesProvider lp) {
        try {
            FeatureFullHex ffh = new FeatureFullHex(null, tp, lp);
            movePlus(tp, plus, null, ffh);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(openHex, ex.getMessage());
        }
    }

    public void setTreatAllTabsAsOneBatch(boolean selected) {
        treatAllTabsAsOneBatch = selected;
    }

    public boolean isTreatAllTabsAsOneBatch() {
        return treatAllTabsAsOneBatch;
    }

    public List<String> getAllTexts(FeatureFullHex caller) {
        List<String> r = new ArrayList<>();
        for (Component c : mainTabs.getComponents()) {
            if (c instanceof FeatureFullHex) {
                if (((FeatureFullHex) c).isText()) {
                    if (c == caller) {
                        r.add(0, ((FeatureFullHex) c).getLines().stream().collect(Collectors.joining("\n")));
                    } else {
                        r.add(((FeatureFullHex) c).getLines().stream().collect(Collectors.joining("\n")));
                    }
                }
            }
        }
        return r;
    }
}
