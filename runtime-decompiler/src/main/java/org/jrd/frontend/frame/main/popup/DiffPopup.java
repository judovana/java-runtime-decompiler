package org.jrd.frontend.frame.main.popup;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.jrd.frontend.frame.main.decompilerview.LinesProvider;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Function;

public class DiffPopup extends JPopupMenu {

    public static DiffPopup create(Component[] components) {
        DiffPopup p = new DiffPopup();
        JMenuItem oneThree = new JMenuItem(components[0].getName() + " - " + components[2].getName());
        oneThree.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                List<String> l0 = ((LinesProvider) components[0]).getLines(0);
                List<String> l1 = ((LinesProvider) components[2]).getLines(0);
                Patch<String> diff = DiffUtils.diff(l0, l1);
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(components[0].getName(), components[2].getName(), l0, diff, 0);

                unifiedDiff.forEach(System.out::println);

                DiffRowGenerator generator =
                        DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true).oldTag(new Function<Boolean, String>() {
                            @Override
                            public String apply(Boolean a) {
                                return a ? "<strike>" : "</strike>";
                            }
                        }).newTag(new Function<Boolean, String>() {
                            @Override
                            public String apply(Boolean a) {
                                return a ? "<b>" : "</b>";
                            }
                        }).build();
                List<DiffRow> rows = generator.generateDiffRows(l0, l1);
                System.out.println("|original|new|");
                System.out.println("|--------|---|");
                for (DiffRow row : rows) {
                    System.out.println("|" + row.getOldLine() + "|" + row.getNewLine() + "|");
                }
            }
        });
        p.add(oneThree);
        JMenuItem threeFive = new JMenuItem(components[2].getName() + " - " + components[4].getName());
        p.add(threeFive);
        JMenuItem oneFive = new JMenuItem(components[0].getName() + " - " + components[4].getName());
        p.add(oneFive);
        JMenuItem bin1 = new JMenuItem(components[1].getName() + " - " + components[3].getName() + " (ascii)");
        bin1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ((LinesProvider) components[1]).getLines(0).forEach(System.out::println);
            }
        });
        p.add(bin1);
        JMenuItem bin2 = new JMenuItem(components[1].getName() + " - " + components[3].getName() + " (hex)");
        bin2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ((LinesProvider) components[1]).getLines(1).forEach(System.out::println);
            }
        });
        p.add(bin2);
        p.add(new JCheckBox("human readable"));
        p.add(new JCheckBox("invert order"));
        return p;
    }
}
