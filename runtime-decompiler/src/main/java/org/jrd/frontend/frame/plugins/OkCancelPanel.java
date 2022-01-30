package org.jrd.frontend.frame.plugins;

import org.jrd.backend.decompiling.DecompilerWrapper;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Panel with three buttons "Validate", "OK" and "Cancel"
 */
public class OkCancelPanel extends JPanel {

    private final JButton okButton;
    private final JButton cancelButton;
    private final JButton validateButton;
    private final JSplitPane validations = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    OkCancelPanel() {
        this.setPreferredSize(new Dimension(0, 75));
        setBorder(new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.shadow")));
        this.setLayout(new BorderLayout());

        okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(90, 28));
        cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(90, 28));
        validateButton = new JButton("Validate");
        validateButton.setPreferredSize(new Dimension(90, 28));

        JPanel gb = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        gb.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.gridx = 1;
        gb.add(validateButton, gbc);
        gbc.gridx = 2;
        gb.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 3;
        gb.add(okButton, gbc);
        gbc.gridx = 4;
        gb.add(Box.createHorizontalStrut(15), gbc);
        gbc.gridx = 5;
        gb.add(cancelButton, gbc);
        gbc.gridx = 6;
        gb.add(Box.createHorizontalStrut(20), gbc);
        this.add(gb, BorderLayout.NORTH);
        this.add(validations, BorderLayout.CENTER);
    }

    public JButton getValidateButton() {
        return validateButton;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public void removeValidations() {
        validations.removeAll();
        validations.repaint();
        this.validate();
    }

    public void setValidations(boolean ok, String errors, DecompilerWrapper w) {
        removeValidations();
        JPanel p1 = new JPanel(new BorderLayout());
        JPanel p2 = new JPanel(new BorderLayout());
        validations.setLeftComponent(p1);
        validations.setRightComponent(p2);
        if (ok) {
            JLabel l = new JLabel("This plugin is valid.");
            l.setForeground(Color.green);
            p1.add(l, BorderLayout.NORTH);
            if (w.getCompileMethod() != null) {
                p1.add(new JLabel("Have custom compiler"), BorderLayout.CENTER);
            } else {
                p1.add(new JLabel("no special compiler"), BorderLayout.CENTER);
            }
            if (w.getDecompileMethodNoInners() != null) {
                p2.add(new JLabel("have single class decompile method"), BorderLayout.NORTH);
            } else {
                p2.add(new JLabel("!missing! single class decompile method"), BorderLayout.NORTH);
            }
            if (w.getDecompileMethodWithInners() != null) {
                p2.add(new JLabel("have multi class decompile method"), BorderLayout.CENTER);
            } else {
                p2.add(new JLabel("!missing! multi class decompile method"), BorderLayout.CENTER);
            }
            validations.setDividerLocation(0.5);
        } else {
            JLabel l = new JLabel("Validation failed.");
            l.setForeground(Color.red);
            p1.add(l, BorderLayout.NORTH);
            if (errors != null) {
                JTextArea ta = new JTextArea(errors);
                JScrollPane sp = new JScrollPane(ta);
                p2.add(sp, BorderLayout.CENTER);
            }
            validations.setDividerLocation(0.25);
        }
        validations.repaint();
        this.validate();
    }
}
