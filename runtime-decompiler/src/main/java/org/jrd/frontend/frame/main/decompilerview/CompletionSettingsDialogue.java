package org.jrd.frontend.frame.main.decompilerview;

import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.wordsets.ConnectedKeywords;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class CompletionSettingsDialogue extends JDialog {
    private final JLabel status = new JLabel("...");
    private final JList<CompletionItem.CompletionItemSet> completions;
    private ConnectedKeywords result;
    private boolean okState = false;

    JRadioButton rb11 = new JRadioButton("starts with");
    JRadioButton rb12 = new JRadioButton("contains");
    JRadioButton rb13 = new JRadioButton("contains sparse");
    JRadioButton rb14 = new JRadioButton("mayhem");
    JRadioButton rb21 = new JRadioButton("case sensitive");
    JRadioButton rb22 = new JRadioButton("case non sensitive");
    JButton ok = new JButton("ok");
    JButton cancel = new JButton("cancel");
    JCheckBox showHelp = new JCheckBox("Show help if available");

    public CompletionSettingsDialogue() {
        this.setLayout(new GridLayout(1, 2));
        this.setModal(true);
        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel("Select completion(s) - multiselect supported"), BorderLayout.NORTH);
        completions = new JList(SupportedKeySets.JRD_KEY_SETS.getSets());
        completions.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (completions.getSelectedValuesList().isEmpty() || result == null) {
                    status.setText("Nothing selected - code compeltion will be disabled");
                } else {
                    status.setText(completions.getSelectedValuesList().size() + " selected: " + result.getItemsList().size() + " keywords");
                }
                operate();
            }
        });
        p1.add(completions);
        p1.add(status, BorderLayout.NORTH);
        JPanel help = new JPanel(new GridLayout(5, 2));
        help.add(new JLabel("CLASS keyword"));
        help.add(new JLabel("will offer list of classes"));
        help.add(new JLabel("in JRD"));
        help.add(new JLabel("from currently connected vm"));
        help.add(new JLabel("in standalone editor"));
        help.add(new JLabel("from settings additional CP"));
        help.add(new JLabel("METHOD keyword"));
        help.add(new JLabel("will offer list of methods from last CLASS"));
        help.add(new JLabel("This is originally for byteman"));
        help.add(new JLabel("but works everywhere f completion is on"));
        p1.add(help, BorderLayout.SOUTH);
        JPanel p2 = new JPanel(new GridLayout(10, 1));
        p2.add(new JLabel("Select sensitivity"));
        ButtonGroup b1 = new ButtonGroup();
        b1.add(rb11);
        b1.add(rb12);
        b1.add(rb13);
        b1.add(rb14);
        p2.add(rb11);
        p2.add(rb12);
        p2.add(rb13);
        p2.add(rb14);
        rb11.setSelected(true);
        p2.add(new JLabel("case sensitivity"));
        ButtonGroup b2 = new ButtonGroup();
        rb21.setSelected(true);
        b2.add(rb21);
        b2.add(rb22);
        p2.add(rb21);
        p2.add(rb22);
        p2.add(showHelp);
        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(ok);
        buttons.add(cancel);
        p2.add(buttons);

        ok.addActionListener(actionEvent -> {
            okState = true;
            setVisible(false);
        });
        cancel.addActionListener(actionEvent -> {
            setVisible(false);
        });

        this.add(p1);
        this.add(p2);
        this.pack();

    }

    private void operate() {
        if (completions.getSelectedValuesList().isEmpty()) {
            result = null;
        } else {
            result = new ConnectedKeywords(completions.getSelectedValuesList().toArray(new CompletionItem.CompletionItemSet[]{}));
        }
    }

    public CompletionSettings showForResults(Component parent, CompletionSettings settings) {
        pre(settings);
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
        if (okState) {
            return post();
        } else {
            return null;
        }
    }

    private CompletionSettings post() {
        boolean caseSensitive = true;
        if (rb22.isSelected()) {
            caseSensitive = false;
        }
        CompletionSettings.OP op = CompletionSettings.OP.STARTS;
        if (rb12.isSelected()) {
            op = CompletionSettings.OP.CONTAINS;
        }
        if (rb13.isSelected()) {
            op = CompletionSettings.OP.SPARSE;
        }
        if (rb14.isSelected()) {
            op = CompletionSettings.OP.MAYHEM;
        }
        return new CompletionSettings(result, op, caseSensitive, showHelp.isSelected());
    }

    private void pre(CompletionSettings settings) {
        showHelp.setSelected(settings.isShowHelp());
        if (!settings.isCaseSensitive()) {
            rb22.setSelected(true);
        }
        if (CompletionSettings.OP.CONTAINS == settings.getOp()) {
            rb12.setSelected(true);
        }
        if (CompletionSettings.OP.SPARSE == settings.getOp()) {
            rb13.setSelected(true);
        }
        if (CompletionSettings.OP.MAYHEM == settings.getOp()) {
            rb14.setSelected(true);
        }
        CompletionItem.CompletionItemSet[] sets;
        if (settings.getSet() instanceof ConnectedKeywords) {
            sets = ((ConnectedKeywords) settings.getSet()).getOriginalSets();
        } else {
            sets = new CompletionItem.CompletionItemSet[]{settings.getSet()};
        }
        List<Integer> selected = new ArrayList<>();
        for (CompletionItem.CompletionItemSet set : sets) {
            for (int i = 0; i < completions.getModel().getSize(); i++) {
                if (completions.getModel().getElementAt(i).toString().equals(set.toString())) {
                    selected.add(i);
                }
            }
        }
        int[] indices = new int[selected.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = selected.get(i);
        }
        completions.setSelectedIndices(indices);
    }

}
