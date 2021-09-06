package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.core.ClassInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public final class ClassListPopupMenu extends JPopupMenu {

    private ClassListPopupMenu() {
    }

    public static ClassListPopupMenu create(ClassInfo classToCopy) {
        ClassListPopupMenu result = new ClassListPopupMenu();

        result.add(createCopyClassNameItem("Copy class name", classToCopy.getName()));
        result.add(createCopyClassNameItem("Copy class location", classToCopy.getLocation()));
        result.add(createCopyClassNameItem("Copy class loader", classToCopy.getClassLoader()));

        return result;
    }

    private static JMenuItem createCopyClassNameItem(String itemTitle, String className) {
        JMenuItem item = new JMenuItem(itemTitle);

        item.addActionListener(actionEvent -> {
            if (className == null || className.isEmpty()) {
                return;
            }

            StringSelection selection = new StringSelection(className);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });

        return item;
    }
}
