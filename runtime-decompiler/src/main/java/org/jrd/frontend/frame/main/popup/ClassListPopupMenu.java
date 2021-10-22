package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.core.ClassInfo;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ClassListPopupMenu extends JPopupMenu {

    private ClassListPopupMenu() {
    }

    public static ClassListPopupMenu create(JList<ClassInfo> parentJList, int originallySelected, boolean doShowClassInfo) {
        ClassListPopupMenu result = new ClassListPopupMenu();

        JCheckBox copyName = new JCheckBox("Copy name(s)");
        copyName.setSelected(true);
        JCheckBox copyLocation = new JCheckBox("Copy class location(s)");
        copyLocation.setSelected(false);
        JCheckBox copyLoader = new JCheckBox("Copy class loader(s)");
        copyLoader.setSelected(false);

        result.add(createCopyItem("Copy selected", parentJList.getSelectedValuesList(), copyName, copyLocation, copyLoader));
        result.add(createCopyItem("Copy all", allItems(parentJList.getModel()), copyName, copyLocation, copyLoader));

        if (doShowClassInfo) {
            result.add(copyName);
            result.add(copyLocation);
            result.add(copyLoader);
        }

        JMenuItem helpItem = new JMenuItem("TIP: Drag with RMB to multi-select");
        helpItem.setEnabled(false);
        result.addSeparator();
        result.add(helpItem);

        result.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                parentJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                parentJList.setSelectedIndex(originallySelected);
                parentJList.requestFocusInWindow();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                // ...Canceled is called only when it gets dismissed, whereas ...WillBecomeInvisible is called everytime the menu disappears
            }
        });

        return result;
    }

    private static List<ClassInfo> allItems(ListModel<ClassInfo> model) {
        List<ClassInfo> result = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            result.add(model.getElementAt(i));
        }
        return result;
    }

    private static JMenuItem createCopyItem(
            String itemTitle,
            List<ClassInfo> classProperties,
            JCheckBox names,
            JCheckBox locations,
            JCheckBox loaders
    ) {
        JMenuItem item = new JMenuItem(itemTitle);

        item.addActionListener(actionEvent -> {
            if (classProperties == null || classProperties.isEmpty()) {
                return;
            }

            StringSelection selection = new StringSelection(
                    classProperties.stream()
                            .map(a -> classInfoToSelectedStrings(a, names, locations, loaders))
                            .collect(Collectors.joining(""))
            );
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        return item;
    }

    private static String classInfoToSelectedStrings(ClassInfo a, JCheckBox names, JCheckBox locations, JCheckBox loaders) {
        String s = "";
        if (names.isSelected()) {
            s = s + a.getName() + System.getProperty("line.separator", "\n");
        }
        if (locations.isSelected()) {
            s = s + a.getLocation() + System.getProperty("line.separator", "\n");
        }
        if (loaders.isSelected()) {
            s = s + a.getClassLoader() + System.getProperty("line.separator", "\n");
        }
        return s;
    }
}
