package org.jrd.frontend.frame.main.popup;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.decompilerview.LinesProvider;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class DiffPopup extends JPopupMenu {
    private final String fqn;
    private final Component[] components;
    JCheckBox human = new JCheckBox("human readable");
    JCheckBox invert = new JCheckBox("invert order");

    public DiffPopup(Component[] components, String fqn) {
        this.fqn = fqn;
        this.components = components;
        JMenuItem oneThree = new JMenuItem(components[0].getName() + " - " + components[2].getName());
        oneThree.addActionListener(actionEvent -> {
            processText(0, 2);
        });
        this.add(oneThree);
        JMenuItem threeFive = new JMenuItem(components[2].getName() + " - " + components[4].getName());
        threeFive.addActionListener(actionEvent -> {
            processText(2, 4);
        });
        this.add(threeFive);
        JMenuItem oneFive = new JMenuItem(components[0].getName() + " - " + components[4].getName());
        oneFive.addActionListener(actionEvent -> {
            processText(2, 4);
        });
        this.add(oneFive);
        JMenuItem bin1 = new JMenuItem(components[1].getName() + " - " + components[3].getName() + " (ascii)");
        bin1.addActionListener(actionEvent -> {
            processBin(LinesProvider.LinesFormat.CHARS, 1, 3);
        });
        this.add(bin1);
        JMenuItem bin2 = new JMenuItem(components[1].getName() + " - " + components[3].getName() + " (hex)");
        bin2.addActionListener(actionEvent -> {
            processBin(LinesProvider.LinesFormat.HEX, 1, 3);
        });
        this.add(bin2);
        this.add(human);
        this.add(invert);
        JMenu applyPatch = new JMenu();
        applyPatch.setText("Apply patch");
        applyPatch.add(createPatchAction(0, LinesProvider.LinesFormat.CHARS));
        applyPatch.add(createPatchAction(1, LinesProvider.LinesFormat.HEX));
        applyPatch.add(createPatchAction(2, LinesProvider.LinesFormat.CHARS));
        applyPatch.add(createPatchAction(3, LinesProvider.LinesFormat.HEX));
        applyPatch.add(createPatchAction(4, LinesProvider.LinesFormat.CHARS));
        this.add(applyPatch);
    }

    private JMenuItem createPatchAction(int id, final LinesProvider.LinesFormat suffix) {
        LinesProvider component = (LinesProvider) components[id];
        JMenuItem item = new JMenuItem(component.getName() + " " + (suffix == LinesProvider.LinesFormat.CHARS ? "" : " " + suffix));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JDialog d = new JDialog((JFrame) null, "paste patch to apply to " + item.getText() + "/" + fqn);
                d.setSize(new Dimension(800, 600));
                d.setLocationRelativeTo(null);
                RSyntaxTextArea t = new RSyntaxTextArea("");
                d.add(new JScrollPane(t));
                d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                JButton apply = new JButton("apply");
                apply.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        List<String> orig = component.getLines(suffix);
                        Patch<String> importedPatch = UnifiedDiffUtils.parseUnifiedDiff(Arrays.asList(t.getText().split("\n")));
                        try {
                            List<String> patchedText =
                                    invert.isSelected() ? DiffUtils.unpatch(orig, importedPatch) : DiffUtils.patch(orig, importedPatch);
                            component.setLines(suffix, patchedText);
                        } catch (Exception e) {
                            Logger.getLogger().log(Logger.Level.ALL, e);
                            t.setText("press ctrl+z to return to patch\n" + Logger.exToString(e));
                        }
                    }
                });
                d.add(apply, BorderLayout.SOUTH);
                d.setVisible(true);
            }
        });

        return item;
    }

    private void processBin(LinesProvider.LinesFormat format, int x, int y) {
        List<String> l0 = ((LinesProvider) components[x]).getLines(format);
        List<String> l1 = ((LinesProvider) components[y]).getLines(format);
        process(l0, l1, components[x].getName(), components[y].getName(), invert.isSelected(), human.isSelected(), fqn);
    }

    private void processText(int x, int y) {
        processBin(null, x, y);
    }

    private static void process(List<String> l0, List<String> l1, String n0, String n1, boolean invert, boolean human, String fqn) {
        if (invert) {
            List<String> l = l0;
            l0 = l1;
            l1 = l;
            String n = n0;
            n0 = n1;
            n1 = n;
        }
        if (human) {
            String html = "<html>" + getHtml(l0, l1) + "</html>";
            JDialog d = new JDialog((JFrame) null, "diff " + n0 + "/" + fqn + " x " + n1 + "/" + fqn);
            d.setSize(new Dimension(800, 600));
            d.setLocationRelativeTo(null);
            JEditorPane t = new JEditorPane("text/html", html);
            t.setFont(new Font("Monospaced", t.getFont().getStyle(), t.getFont().getSize()));
            d.add(new JScrollPane(t));
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setVisible(true);
        } else {
            String patch = getPatch(l0, l1, n0 + "/" + fqn, n1 + "/" + fqn);
            JDialog d = new JDialog((JFrame) null, "patch " + n0 + "/" + fqn + " x " + n1 + "/" + fqn);
            d.setSize(new Dimension(800, 600));
            d.setLocationRelativeTo(null);
            JTextArea t = new JTextArea(patch);
            t.setFont(new Font("Monospaced", t.getFont().getStyle(), t.getFont().getSize()));
            d.add(new JScrollPane(t));
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setVisible(true);
        }
    }

    public static String getPatch(List<String> l0, List<String> l1, String name0, String name1) {
        Patch<String> diff = DiffUtils.diff(l0, l1);
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(name0, name1, l0, diff, 3);

        StringBuilder sb = new StringBuilder();
        for (String s : unifiedDiff) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    public static String getHtml(List<String> l0, List<String> l1) {
        DiffRowGenerator generator = DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true)
                .oldTag(a -> a ? "<strike>" : "</strike>").newTag(a -> a ? "<b>" : "</b>").build();
        List<DiffRow> rows = generator.generateDiffRows(l0, l1);
        StringBuilder sb = new StringBuilder();
        for (DiffRow row : rows) {
            if (row.getOldLine().equals(row.getNewLine())) {
                sb.append(row.getOldLine()).append("<br/>").append("\n");
            } else {
                if (!row.getOldLine().isEmpty()) {
                    sb.append("<span color='red'>").append(row.getOldLine()).append("</span><br/>\n");
                }
                if (!row.getNewLine().isEmpty()) {
                    sb.append("<span color='blue'>").append(row.getNewLine()).append("</span><br/>\n");
                }
            }
        }
        return sb.toString().replace(" ", "&nbsp;");
    }
}
