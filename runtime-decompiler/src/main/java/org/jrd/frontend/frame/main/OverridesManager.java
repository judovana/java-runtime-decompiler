package org.jrd.frontend.frame.main;

import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

public final class OverridesManager {

    private static final OverridesManager DIALOG = new OverridesManager();

    private static final String VM_TITLE = "Currently overridden classes in";
    private final JLabel activeOverridesLabel = new JLabel(VM_TITLE + " XXXXXXXXXXXXXXXXXXX");
    private final JList activeOverrides = new JList();
    private final JButton refresh = new JButton("refresh");
    private final JLabel removalLabel = new JLabel("Remove all overrides matching regex: ");
    private final JTextField removalRegex = new JTextField(".*");
    private final JButton remove = new JButton("Do!");
    private final JButton close = new JButton("Close");
    private final JDialog window = new JDialog((JFrame) null, "Manage overridden classes");
    private DecompilationController dc;

    private OverridesManager() {
        window.setLayout(new BorderLayout());
        window.add(activeOverridesLabel, BorderLayout.NORTH);
        window.add(new JScrollPane(activeOverrides));
        activeOverrides.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    if (activeOverrides.getSelectedValue() != null) {
                        removalRegex.setText(activeOverrides.getSelectedValue().toString());
                    } else {
                        removalRegex.setText(".*");
                    }
                }
            }
        });
        activeOverrides.setModel(new DefaultComboBoxModel() {
            @Override
            public Object getElementAt(int index) {
                return "java.lang.hell.Hello";
            }

            @Override
            public int getSize() {
                return 10;
            }
        });
        JPanel southPanel = new JPanel(new GridLayout(5, 1));
        window.add(southPanel, BorderLayout.SOUTH);
        southPanel.add(refresh);
        refresh.addActionListener(a -> load());
        southPanel.add(removalLabel);
        southPanel.add(removalRegex);
        final Color originalForeground = removalRegex.getForeground();
        final Color originalBackground = removalRegex.getBackground();
        removalRegex.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                removeUpdate(documentEvent);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                try {
                    Pattern.compile(removalRegex.getText());
                    removalRegex.setForeground(originalForeground);
                    removalRegex.setBackground(originalBackground);
                    removalRegex.repaint();
                } catch (Exception ex) {
                    removalRegex.setForeground(Color.RED);
                    removalRegex.setBackground(Color.WHITE);
                    removalRegex.repaint();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                removeUpdate(documentEvent);
            }
        });
        southPanel.add(remove);
        remove.addActionListener(a -> doRemove());
        southPanel.add(close);
        close.addActionListener(a -> hide());
        window.pack();
        activeOverrides.setModel(new DefaultComboBoxModel());
        ScreenFinder.centerWindowsToCurrentScreen(window);
    }

    public void hide() {
        window.setVisible(false);
    }

    public static void showFor(JFrame parent, DecompilationController dc) {
        if (dc == null || dc.getVm() == null) {
            JOptionPane.showMessageDialog(parent, "No VM selected!");
        } else {
            DIALOG.setDc(dc);
            DIALOG.load();
            // always per vm, so modal
            DIALOG.window.setModal(true);
            DIALOG.window.setVisible(true);
        }
    }

    private void doRemove() {
        try {
            dc.removeOverrides(Pattern.compile(removalRegex.getText()).toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
        load();
    }

    private void load() {
        try {
            String[] classes = dc.getOverrides();
            activeOverrides.setModel(new DefaultComboBoxModel(classes));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

    }

    private void setDc(DecompilationController dc) {
        this.dc = dc;
        activeOverridesLabel.setText(VM_TITLE + " " + dc.getVm());
    }
}
