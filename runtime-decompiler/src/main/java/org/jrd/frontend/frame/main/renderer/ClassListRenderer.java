package org.jrd.frontend.frame.main.renderer;

import org.jrd.backend.core.ClassInfo;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EtchedBorder;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ClassListRenderer extends JPanel implements ListCellRenderer<ClassInfo> {

    JLabel name;
    JLabel location;
    JLabel classLoader;
    JLabel module;
    JLabel moduleLoader;
    boolean doShowInfo;

    public ClassListRenderer() {
        name = new JLabel();
        location = new JLabel();
        classLoader = new JLabel();
        module = new JLabel();
        moduleLoader = new JLabel();

        Font infoFont = location.getFont().deriveFont(10.0F);

        location.setFont(infoFont);
        classLoader.setFont(infoFont);
        module.setFont(infoFont);
        moduleLoader.setFont(infoFont);

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        this.add(name, gbc);

        gbc.weighty = 0.5;
        gbc.gridy = 1;
        this.add(location, gbc);

        gbc.weighty = 0.5;
        gbc.gridy = 2;
        this.add(classLoader, gbc);

        gbc.weighty = 0.5;
        gbc.gridy = 3;
        this.add(module, gbc);

        gbc.weighty = 0.5;
        gbc.gridy = 4;
        this.add(moduleLoader, gbc);

        this.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ClassInfo> list, ClassInfo classInfo, int i, boolean isSelected, boolean cellHasFocus
    ) {
        name.setText(classInfo.getName());

        if (doShowInfo) {
            location.setText("Location: " + classInfo.getLocation() + " "); // trailing space to prevent font cutoff
            location.setVisible(true);
            classLoader.setText("Class loader: " + classInfo.getClassLoader() + " ");
            classLoader.setVisible(true);
            module.setText("Module: " + classInfo.getModule() + " ");
            module.setVisible(true);
            moduleLoader.setText("Module loader: " + classInfo.getModuleLoader() + " ");
            moduleLoader.setVisible(true);
            this.setPreferredSize(null);
        } else {
            location.setVisible(false);
            classLoader.setVisible(false);
            module.setVisible(false);
            moduleLoader.setVisible(false);
            this.setPreferredSize(null);
        }

        if (isSelected) {
            this.setBackground(list.getSelectionBackground());
            this.setForeground(list.getSelectionForeground());
        } else {
            this.setBackground(list.getBackground());
            this.setForeground(list.getForeground());
        }

        return this;
    }

    public void setDoShowInfo(boolean doShowInfo) {
        this.doShowInfo = doShowInfo;
    }

}
