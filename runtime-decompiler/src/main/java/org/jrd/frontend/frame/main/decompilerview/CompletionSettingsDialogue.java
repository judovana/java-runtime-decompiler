package org.jrd.frontend.frame.main.decompilerview;

import org.kcc.CompletionItem;
import org.kcc.CompletionSettings;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.BytemanKeywords;
import org.kcc.wordsets.ConnectedKeywords;
import org.kcc.wordsets.JavaKeywordsWithHelp;
import org.kcc.wordsets.JrdApiKeywords;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class CompletionSettingsDialogue extends JDialog {
    private final JLabel status;
    private final  JList<CompletionItem.CompletionItemSet> completions;
    private ConnectedKeywords result;

    public CompletionSettingsDialogue() {
        this.setLayout(new GridLayout(1,2));
        this.setModal(true);
        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel("Select completion(s) - multiselect supported"), BorderLayout.NORTH);
        completions = new JList(new CompletionItem.CompletionItemSet[]{
                new JrdApiKeywords(),
                new BytemanKeywords(),
                new BytecodeKeywordsWithHelp(),
                new JavaKeywordsWithHelp()
        });
        completions.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (completions.getSelectedValuesList().isEmpty()){
                    status.setText("Nothing selected - codecompeltion will be disabled");
                } else {
                    operate();
                    status.setText(completions.getSelectedValuesList().size() + " selected: " + result.getItemsList().size()+" keywords");
                }
            }
        });
        p1.add(completions);
        status = new JLabel("...");
        p1.add(status, BorderLayout.SOUTH);
        JPanel p2 = new JPanel(new GridLayout(8,1));
        p2.add(new JLabel("Select sensitivity"));
        JRadioButton rb11 = new JRadioButton("starts with");
        JRadioButton rb12 = new JRadioButton("contains");
        JRadioButton rb13 = new JRadioButton("contains sparse");
        JRadioButton rb14 = new JRadioButton("mayhem");
        ButtonGroup b1 = new ButtonGroup();
        b1.add(rb11);
        b1.add(rb12);
        b1.add(rb13);
        b1.add(rb14);
        p2.add(rb11);
        p2.add(rb12);
        p2.add(rb13);
        p2.add(rb14);
        p2.add(new JLabel("case sensitivity"));
        JRadioButton rb21 = new JRadioButton("case sensitive");
        JRadioButton rb22 = new JRadioButton("case non sensitive");
        ButtonGroup b2 = new ButtonGroup();
        b2.add(rb21);
        b2.add(rb22);
        p2.add(rb21);
        p2.add(rb22);
        this.add(p1);
        this.add(p2);
        this.pack();

    }

    private void operate() {
        if (completions.getSelectedValuesList().isEmpty()){
            result = null;
        } else {
            result = new ConnectedKeywords(completions.getSelectedValuesList().toArray(new CompletionItem.CompletionItemSet[0]));
        }
    }

    public CompletionSettings showForResults() {
        this.setVisible(true);
        return null;
    }


}
