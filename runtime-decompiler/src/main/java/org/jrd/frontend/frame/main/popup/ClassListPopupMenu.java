package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.core.ClassInfo;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ClassListPopupMenu extends JPopupMenu {

    private ClassListPopupMenu() {
    }

    public static ClassListPopupMenu create(JList<ClassInfo> parentJList, int originallySelected, boolean doShowClassInfo) {
        List<ClassInfo> classesToCopy = parentJList.getSelectedValuesList();
        ClassListPopupMenu result = new ClassListPopupMenu();

        result.add(createCopyItem("Copy class name", classesToCopy, ClassInfo::getName));

        if (doShowClassInfo) {
            result.add(createCopyItem("Copy class location", classesToCopy, ClassInfo::getLocation));
            result.add(createCopyItem("Copy class loader", classesToCopy, ClassInfo::getClassLoader));
        }

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

    private static JMenuItem createCopyItem(String itemTitle, List<ClassInfo> classProperties, Function<ClassInfo, String> mapFunction) {
        JMenuItem item = new JMenuItem(itemTitle);

        item.addActionListener(actionEvent -> {
            if (classProperties == null || classProperties.isEmpty()) {
                return;
            }

            StringSelection selection = new StringSelection(
                classProperties.stream().map(mapFunction).collect(Collectors.joining(System.getProperty("line.separator", "\n")))
            );
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        return item;
    }
}
