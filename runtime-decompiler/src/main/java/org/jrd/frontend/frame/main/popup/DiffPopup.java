package org.jrd.frontend.frame.main.popup;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.main.decompilerview.LinesProvider;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class DiffPopup extends JPopupMenu {

    private static File lastOpened = new File(System.getProperty("user.dir"));

    private final Optional<String> fqn;
    private final LinesProvider[] linesProviders;
    JCheckBox human = new JCheckBox("human readable");
    JCheckBox invert = new JCheckBox("invert order");

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "be aware, this constrctor throws")
    public DiffPopup(List<LinesProvider> linesProviders, Optional<String> fqn, boolean onlyOne) {
        this(linesProviders.toArray(new LinesProvider[0]), fqn, onlyOne);
    }

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "be aware, this constrctor throws")
    public DiffPopup(LinesProvider[] linesProviders, Optional<String> fqn, boolean onlyOne) {
        this.fqn = fqn;
        this.linesProviders = linesProviders;
        int iterateTo = linesProviders.length;
        if (onlyOne) {
            iterateTo = 1;
        }
        for (int x = 0; x < iterateTo; x++) {
            if (linesProviders[x].isText()) {
                for (int y = x + 1; y < linesProviders.length; y++) {
                    if (linesProviders[y].isText()) {
                        int finalX = x;
                        int finalY = y;
                        JMenuItem oneThree = new JMenuItem(linesProviders[x].getName() + " - " + linesProviders[y].getName());
                        oneThree.addActionListener(actionEvent -> {
                            processText(finalX, finalY);
                        });
                        this.add(oneThree);
                    }
                }
            } else if (linesProviders[x].isBin()) {
                for (int y = x + 1; y < linesProviders.length; y++) {
                    if (linesProviders[y].isBin()) {
                        int finalX = x;
                        int finalY = y;
                        JMenuItem bin1 = new JMenuItem(linesProviders[x].getName() + " - " + linesProviders[y].getName() + " (ascii)");
                        bin1.addActionListener(actionEvent -> {
                            processBin(LinesProvider.LinesFormat.CHARS, finalX, finalY);
                        });
                        this.add(bin1);
                        JMenuItem bin2 = new JMenuItem(linesProviders[x].getName() + " - " + linesProviders[y].getName() + " (hex)");
                        bin2.addActionListener(actionEvent -> {
                            processBin(LinesProvider.LinesFormat.HEX, finalX, finalY);
                        });
                        this.add(bin2);
                    }
                }
            } else {
                throw new RuntimeException("unknown text/bin");
            }
        }
        this.add(human);
        this.add(invert);

        JMenu applyPatch = new JMenu();
        applyPatch.setText("Apply patch");
        for (int x = 0; x < iterateTo; x++) {
            if (linesProviders[x].isText()) {
                applyPatch.add(createPatchAction(x, LinesProvider.LinesFormat.CHARS));
            } else if (linesProviders[x].isBin()) {
                applyPatch.add(createPatchAction(x, LinesProvider.LinesFormat.HEX));
            } else {
                throw new RuntimeException("unknown text/bin");
            }
            this.add(applyPatch);
        }
    }

    public static String parseClassFromHeader(String s) {
        String clazz = s.trim().replaceAll(".*/", "").replaceAll(".*\\\\", "");
        clazz = clazz.replaceAll("\\.class$", "");
        clazz = clazz.replaceAll("\\.java$", "");
        clazz = clazz.replaceAll(".*\\s+", "");
        return clazz;
    }

    public static boolean isAddDevNull(String line) {
        return line.startsWith("+++ ") && line.endsWith("/dev/null");
    }

    public static boolean isDevNull(String line) {
        return (line.startsWith("+++ ") || line.startsWith("--- ")) && line.endsWith("/dev/null");
    }

    public static boolean isRemoveDevNull(String line) {
        return line.startsWith("--- ") && line.endsWith("/dev/null");
    }

    public static boolean isAddFile(String line) {
        return line.startsWith("+++ ") && !line.endsWith("/dev/null");
    }

    public static boolean isRemoveFile(String line) {
        return line.startsWith("--- ") && !line.endsWith("/dev/null");
    }

    private JMenuItem createPatchAction(int id, final LinesProvider.LinesFormat suffix) {
        LinesProvider component = linesProviders[id];
        JMenuItem item = new JMenuItem(component.getName() + " " + (suffix == LinesProvider.LinesFormat.CHARS ? "" : " " + suffix));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JDialog d = new JDialog((JFrame) null, "paste patch to apply to " + patchTitle(component, fqn));
                d.setSize(new Dimension(800, 600));
                d.setLocationRelativeTo(null);
                RSyntaxTextArea t = new RSyntaxTextArea("");
                d.add(new JScrollPane(t));
                d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                JButton apply = new JButton("apply");
                apply.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            List<String> patchedText =
                                    patch(component.getLines(suffix), Arrays.asList(t.getText().split("\n")), invert.isSelected());
                            component.setLines(suffix, patchedText);
                        } catch (Exception e) {
                            Logger.getLogger().log(Logger.Level.ALL, e);
                            t.setText("press ctrl+z to return to patch\n" + Logger.exToString(e));
                        }
                    }
                });
                d.add(apply, BorderLayout.SOUTH);
                JButton open = new JButton("Load patch file");
                open.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        loadPatch(t, open);
                    }
                });
                d.add(open, BorderLayout.NORTH);
                d.setVisible(true);
            }
        });
        return item;
    }

    private void loadPatch(RSyntaxTextArea t, Component open) {
        JFileChooser jFileChooser = new JFileChooser(lastOpened);
        int fo = jFileChooser.showOpenDialog(open);
        File nwf = jFileChooser.getSelectedFile();
        if (fo == JFileChooser.APPROVE_OPTION && nwf != null) {
            try {
                t.setText(Files.readString(nwf.toPath()));
                lastOpened = nwf;
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(open, ex.getMessage());
            }
        }
    }

    public static List<String> dummyCreate(List<String> buffer, List<String> patch, boolean revert) throws PatchFailedException {
        //pathc was preprepared by getIndividualPatches
        //lets expect it did its j ob, and do not recreate the terirbel logic here
        if (!buffer.isEmpty()) {
            throw new PatchFailedException("trying to create new file in non empty original");
        }
        if (!patch.get(2).startsWith("@@")) {
            throw new PatchFailedException("missing @@ header, although it is not yet checked for corrrectness");
        }
        String start = "+";
        if (revert) {
            start = "-";
        }

        for (int x = 3; x < patch.size(); x++) {
            if (!patch.get(x).startsWith(start)) {
                throw new PatchFailedException("Invalid line, all should star with " + start + " was: " + patch.get(x));
            }
            buffer.add(patch.get(x).substring(1));
        }
        return buffer;
    }

    public static List<String> patch(List<String> origFile, List<String> patch, boolean revert) throws PatchFailedException {
        Patch<String> importedPatch = UnifiedDiffUtils.parseUnifiedDiff(patch);
        List<String> patchedText = revert ? DiffUtils.unpatch(origFile, importedPatch) : DiffUtils.patch(origFile, importedPatch);
        return patchedText;
    }

    private void processBin(LinesProvider.LinesFormat format, int x, int y) {
        List<String> l0 = linesProviders[x].getLines(format);
        List<String> l1 = linesProviders[y].getLines(format);
        if (linesProviders[x].getFile() != null) {
            //we assume both are files then
            //todo, make relative?
            process(
                    l0, l1, linesProviders[x].getFile().getAbsolutePath(), linesProviders[y].getFile().getAbsolutePath(),
                    invert.isSelected(), human.isSelected(), fqn
            );
        } else {
            process(l0, l1, linesProviders[x].getName(), linesProviders[y].getName(), invert.isSelected(), human.isSelected(), fqn);
        }
    }

    private void processText(int x, int y) {
        processBin(null, x, y);
    }

    private static
            void
            process(List<String> l0, List<String> l1, String n0, String n1, boolean invert, boolean human, Optional<String> fqn) {
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
            JDialog d = new JDialog((JFrame) null, "diff " + toTitle(n0, n1, fqn));
            d.setSize(new Dimension(800, 600));
            d.setLocationRelativeTo(null);
            JEditorPane t = new JEditorPane("text/html", html);
            t.setFont(new Font("Monospaced", t.getFont().getStyle(), t.getFont().getSize()));
            d.add(new JScrollPane(t));
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setVisible(true);
        } else {
            String patch = getPatch(l0, l1, toPatchName(n0, fqn), toPatchName(n1, fqn));
            JDialog d = new JDialog((JFrame) null, "patch " + toTitle(n0, n1, fqn));
            d.setSize(new Dimension(800, 600));
            d.setLocationRelativeTo(null);
            JTextArea t = new JTextArea(patch);
            t.setFont(new Font("Monospaced", t.getFont().getStyle(), t.getFont().getSize()));
            d.add(new JScrollPane(t));
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setVisible(true);
        }
    }

    private static String toPatchName(String n, Optional<String> fqn) {
        if (fqn.isPresent()) {
            return n + "/" + fqn.get();
        } else {
            return n;
        }
    }

    private static String toTitle(String n0, String n1, Optional<String> fqn) {
        if (fqn.isPresent()) {
            return n0 + "/" + fqn.get() + " x " + n1 + "/" + fqn.get();
        } else {
            return n0 + " x " + n1;
        }
    }

    private static String patchTitle(LinesProvider component, Optional<String> fqn) {
        if (fqn.isPresent()) {
            return component.getName() + "/" + fqn.get();
        } else {
            return component.getName();
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

    @SuppressWarnings({"ModifiedControlVariable", "CyclomaticComplexity"})
    public static List<SingleFilePatch> getIndividualPatches(List<String> patches) {
        List<SingleFilePatch> r = new ArrayList<>(patches.size() / 7);
        int start = -1;
        int end = -1;
        for (int i = 0; i < patches.size(); i++) {
            if ((patches.get(i).startsWith("+++") || patches.get(i).startsWith("---")) && i < patches.size() - 1 &&
                    (patches.get(i + 1).startsWith("---") || patches.get(i + 1).startsWith("+++"))) {
                start = i;
                i++;
                i++;
                while (true) {
                    if (i >= patches.size()) {
                        i--;
                        break;
                    }
                    if (patches.get(i).startsWith("+") || patches.get(i).startsWith("-") ||
                            patches.get(i).startsWith(" ") ||
                            patches.get(i).startsWith("@")) {
                        if (patches.get(i).equals("-- ")) { //this is how diff-email ends files
                            i--;
                            break;
                        }
                        i++;
                    } else {
                        i--;
                        break;
                    }
                }
                end = i;
                r.add(new SingleFilePatch(start, end));
            }
        }
        return r;
    }
}
