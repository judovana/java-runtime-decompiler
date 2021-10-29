package org.jrd.frontend.frame.plugins;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import org.jrd.backend.decompiling.DecompilerWrapper;

public class PluginListPanel extends JPanel {

    private final JList<DecompilerWrapper> wrapperJList;
    private final JButton addWrapperButton;

    PluginListPanel() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(180, 0));

        wrapperJList = new JList<>();
        wrapperJList.setFixedCellHeight(32);
        wrapperJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addWrapperButton = new JButton("New");
        addWrapperButton.setPreferredSize(new Dimension(0, 28));

        this.add(wrapperJList, BorderLayout.CENTER);
        this.add(addWrapperButton, BorderLayout.NORTH);
    }

    public JList<DecompilerWrapper> getWrapperJList() {
        return wrapperJList;
    }

    public JButton getAddWrapperButton() {
        return addWrapperButton;
    }
}
