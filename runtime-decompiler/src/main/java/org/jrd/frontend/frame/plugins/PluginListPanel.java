package org.jrd.frontend.frame.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginListPanel extends JPanel {

    private final JList wrapperJList;
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

    public JList getWrapperJList() {
        return wrapperJList;
    }

    public JButton getAddWrapperButton() {
        return addWrapperButton;
    }
}
