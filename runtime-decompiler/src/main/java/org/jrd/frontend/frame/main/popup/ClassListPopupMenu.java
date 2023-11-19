package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.data.DependenciesReader;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.Optional;

public class ClassListPopupMenu<T> extends JListPopupMenu<T> {

    public ClassListPopupMenu(
            JList<T> parentJList, int originallySelected, boolean showCheckboxes, DependenciesReader dr, Optional<String> classloader
    ) {
        super(parentJList, showCheckboxes, dr, classloader);

        addPopupMenuListener(new PopupMenuListener() {
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
    }
}
