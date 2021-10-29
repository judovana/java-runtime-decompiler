package org.jrd.frontend.frame.main;

import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.BorderFactory;
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

public final class OverridesManager {

    private static final OverridesManager DIALOG = new OverridesManager();

    private static final String VM_TITLE = "Currently overridden classes in ";
    private final JLabel activeOverridesLabel = new JLabel(VM_TITLE);
    private final JList<String> activeOverrides;
    private final JTextField removalRegex;
    private final JDialog window = new JDialog((JFrame) null, "Manage overridden classes");
    private DecompilationController dc;

    private OverridesManager() {
        window.setLayout(new BorderLayout(5, 5));
        window.getRootPane().setBorder(BorderFactory.createCompoundBorder(window.getRootPane().getBorder(), new EmptyBorder(5, 5, 5, 5)));
        window.setMinimumSize(new Dimension(400, 600));

        activeOverrides = new JList<>();
        activeOverrides.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    if (activeOverrides.getSelectedValue() != null) {
                        removalRegex.setText(activeOverrides.getSelectedValue());
                    } else {
                        removalRegex.setText(".*");
                    }
                }
            }
        });
        activeOverrides.setMinimumSize(new Dimension(400, 400));
        activeOverrides.setModel(new DefaultComboBoxModel<>());

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(a -> loadOverrides());

        removalRegex = new JTextField(".*");
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

        JButton remove = new JButton("Remove!");
        remove.addActionListener(a -> removeOverride());

        JButton close = new JButton("Close");
        close.addActionListener(a -> hide());

        JPanel southPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        southPanel.add(refresh);
        southPanel.add(new JLabel("Remove all overrides matching regex: "));
        southPanel.add(removalRegex);
        southPanel.add(remove);
        southPanel.add(close);

        window.add(activeOverridesLabel, BorderLayout.NORTH);
        window.add(new JScrollPane(activeOverrides), BorderLayout.CENTER);
        window.add(southPanel, BorderLayout.SOUTH);
        window.pack();
        ScreenFinder.centerWindowToCurrentScreen(window);
    }

    public void hide() {
        window.setVisible(false);
    }

    public static void showFor(JFrame parent, DecompilationController dc) {
        if (dc == null || dc.getVm() == null) {
            JOptionPane.showMessageDialog(parent, "No VM selected!");
        } else {
            DIALOG.setDc(dc);
            DIALOG.loadOverrides();
            // always per vm, so modal
            DIALOG.window.setModal(true);
            DIALOG.window.setVisible(true);
        }
    }

    private void removeOverride() {
        try {
            dc.removeOverrides(Pattern.compile(removalRegex.getText()).toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

        loadOverrides();
    }

    private void loadOverrides() {
        try {
            String[] classes = dc.getOverrides();
            activeOverrides.setModel(new DefaultComboBoxModel<>(classes));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }

    }

    private void setDc(DecompilationController dc) {
        this.dc = dc;
        activeOverridesLabel.setText(VM_TITLE + dc.getVm() + ":");
    }
}
