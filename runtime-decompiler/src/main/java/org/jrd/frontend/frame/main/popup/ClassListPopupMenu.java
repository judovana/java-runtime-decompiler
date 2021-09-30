package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.core.ClassInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public final class ClassListPopupMenu extends JPopupMenu {

    private ClassListPopupMenu() {
    }

    public static ClassListPopupMenu create(ClassInfo classToCopy, boolean doShowClassInfo) {
        ClassListPopupMenu result = new ClassListPopupMenu();

        result.add(createCopyItem("Copy class name", classToCopy.getName()));

        if (doShowClassInfo) {
            result.add(createCopyItem("Copy class location", classToCopy.getLocation()));
            result.add(createCopyItem("Copy class loader", classToCopy.getClassLoader()));
        }

        return result;
    }

    private static JMenuItem createCopyItem(String itemTitle, String classProperty) {
        JMenuItem item = new JMenuItem(itemTitle);

        item.addActionListener(actionEvent -> {
            if (classProperty == null || classProperty.isEmpty()) {
                return;
            }

            StringSelection selection = new StringSelection(classProperty);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });

        return item;
    }
}
