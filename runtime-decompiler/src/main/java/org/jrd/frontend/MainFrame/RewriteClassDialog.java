package org.jrd.frontend.MainFrame;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class RewriteClassDialog extends JDialog {
    private final JPanel inputs;
    private final JPanel buttons;
    private final JLabel validation;
    private final JTextField filePath;
    private final JTextField className;
    private final JButton select;
    private final JLabel nothing;
    private final JButton ok;
    private boolean wasOkPressed;

    public RewriteClassDialog(String name, String lastFile) {
        super((JFrame) null, "Specify class and select its bytecode", true);
        this.setSize(400, 300);
        this.setLayout(new BorderLayout());
        inputs = new JPanel(new GridLayout(3, 1));
        buttons = new JPanel(new GridLayout(3, 1));
        validation = new JLabel("???");
        filePath = new JTextField(lastFile);
        className = new JTextField(name);
        select = new JButton("...");
        nothing = new JLabel();
        ok = new JButton("ok");
        wasOkPressed = false;
        setValidation();
        setSelectListener();
        setOkListener();
        adds();
    }

    private void setValidation() {
        DocumentListener v = new FiletoClassValidator(validation, filePath, className);
        filePath.getDocument().addDocumentListener(v);
        className.getDocument().addDocumentListener(v);
        v.changedUpdate(null);
    }

    private void setSelectListener() {
        select.addActionListener(e -> {
            JFileChooser jf = new JFileChooser(filePath.getText());
            int returnVal = jf.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                filePath.setText(jf.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private void setOkListener() {
        ok.addActionListener(e -> {
            this.wasOkPressed = true;
            this.setVisible(false);
        });
    }

    private void adds() {
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

    public boolean isOkPressed() {
        return this.wasOkPressed;
    }

    public String getClassName() {
        return this.className.getText();
    }

    public String getFilePath() {
        return this.filePath.getText();
    }
}
