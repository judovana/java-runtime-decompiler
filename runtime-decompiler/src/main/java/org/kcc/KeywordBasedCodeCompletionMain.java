package org.kcc;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.kcc.wordsets.BytecodeKeywordsWithHelp;
import org.kcc.wordsets.ConnectedKeywords;
import org.kcc.wordsets.JavaKeywordsWithHelp;
import org.kcc.wordsets.JrdApiKeywords;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class KeywordBasedCodeCompletionMain {

    private KeywordBasedCodeCompletionMain() {
    }

    /**
     * Experiemntal main method showing as demo for KeywordBasedCodeCompletion;
     * move from mainX to main once moved to separeate project
     * @param args
     * @throws Exception
     */
    public static void mainX(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("keyword based code completion example");
                JLabel l1;
                JButton b1;
                //JTextArea t1;
                RSyntaxTextArea t1;
                frame.setLayout(new BorderLayout());
                l1 = new JLabel("hi");
                b1 = new JButton("by");
                t1 = new RSyntaxTextArea();
                frame.add(l1, BorderLayout.NORTH);
                frame.add(b1, BorderLayout.SOUTH);
                frame.add(t1);
                final KeywordBasedCodeCompletion comp = new KeywordBasedCodeCompletion(
                        t1, new CompletionSettings(new BytecodeKeywordsWithHelp(), CompletionSettings.OP.STARTS, true, true)
                );
                b1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        comp.setCompletionsSet(
                                new ConnectedKeywords(new BytecodeKeywordsWithHelp(), new JavaKeywordsWithHelp(), new JrdApiKeywords())
                        );
                    }
                });
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        comp.dispose();
                    }
                });
                frame.setSize(550, 400);
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setVisible(true);

            }
        });
    }
}
