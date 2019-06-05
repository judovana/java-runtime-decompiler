package org.jrd.frontend.MainFrame;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RewriteClassDialog extends JDialog
{
    private final JPanel inputs;
    private final JPanel buttons;
    private final JLabel validation;
    private final JTextField filePath;
    private final JTextField className;
    private final JButton select;
    private final JLabel nothing;
    private final JButton ok;
    private final boolean[] ook;

    public RewriteClassDialog(String name, String lastFile)
    {
        super((JFrame) null, "Specify class and select its bytecode", true);
        this.setSize(400 ,300);
        this.setLayout(new BorderLayout());
        inputs = new JPanel(new GridLayout(3, 1));
        buttons = new JPanel(new GridLayout(3, 1));
        validation  = new JLabel("???");
        filePath = new JTextField(lastFile);
        className = new JTextField(name);
        select = new JButton("...");
        nothing = new JLabel();
        ok = new JButton("ok");
        ook = new boolean[]{false};
        setValidation();
        setListeners();
        adds();
        setVisible(true);
    }

    private void setValidation()
    {
        DocumentListener v = new FiletoClassValidator(validation, filePath, className);
        filePath.getDocument().addDocumentListener(v);
        className.getDocument().addDocumentListener(v);
        v.changedUpdate(null);
    }

    private void setListeners()
    {
        select.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jf = new JFileChooser(filePath.getText());
                int returnVal = jf.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filePath.setText(jf.getSelectedFile().getAbsolutePath());
                }
            }
        });

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ook[0] = true;
                setVisible(false);
            }
        });
    }

    private void adds()
    {
        inputs.add(filePath);
        inputs.add(className);
        inputs.add(className);
        inputs.add(validation);
        buttons.add(select);
        buttons.add(nothing);
        buttons.add(ok);
        this.add(inputs);
        this.add(buttons, BorderLayout.EAST);
        this.pack();
    }

    public boolean[] getOok()
    {
        return ook;
    }

    public String getClassName()
    {
        return this.className.getText();
    }

    public String getFilePath()
    {
        return this.filePath.getText();
    }
}
