package org.jrd.frontend.PluginMangerFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import java.util.ArrayList;

public class ConfigPanel extends JPanel {

    GridBagConstraints gbc;

    public ConfigPanel() {
        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        // addComponent inserts here
        gbc.gridy = 99999;
        gbc.weighty = 1;
        this.add(Box.createVerticalGlue(), gbc);
    }

    private List<Component> getAllComponents ( final Container c){
        Component[] comps = c.getComponents();
        List<Component> compList = new ArrayList<>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container) {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }

    public void addComponent(Component component) {
        gbc.gridy++;
        this.add(component, gbc);
    }
}