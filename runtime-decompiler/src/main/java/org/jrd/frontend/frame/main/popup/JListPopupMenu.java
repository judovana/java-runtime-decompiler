package org.jrd.frontend.frame.main.popup;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JListPopupMenu<T> extends JPopupMenu {

    Map<String, CheckboxGetterPair<T>> checkboxes = new LinkedHashMap<>();
    boolean showCheckBoxes;

    public JListPopupMenu(JList<T> parentJList, boolean showCheckboxes) {
        this.showCheckBoxes = showCheckboxes;

        add(createCopyItem("Copy selected", parentJList.getSelectedValuesList()));
        add(createCopyItem("Copy all", allItems(parentJList.getModel())));

        JMenuItem helpItem = new JMenuItem("TIP: Drag with RMB to multi-select");
        helpItem.setEnabled(false);
        addSeparator();
        add(helpItem);
    }

    public JListPopupMenu<T> addItem(String fieldName, Function<T, String> getter, boolean isSelected) {
        JCheckBox checkBox = new JCheckBox("Copy " + fieldName);

        checkBox.setSelected(isSelected);
        if (showCheckBoxes) {
            add(checkBox, 2 + checkboxes.size()); // keep helpItem at the bottom
        }
        checkboxes.put(getter.toString(), new CheckboxGetterPair<>(checkBox, getter));

        return this;
    }

    protected List<T> allItems(ListModel<T> model) {
        return IntStream.range(0, model.getSize()).mapToObj(model::getElementAt)
                .collect(Collectors.toCollection(() -> new ArrayList<>(model.getSize())));
    }

    protected JMenuItem createCopyItem(String itemTitle, List<T> selectedValues) {
        JMenuItem item = new JMenuItem(itemTitle);

        item.addActionListener(actionEvent -> {
            if (selectedValues == null || selectedValues.isEmpty()) {
                return;
            }

            StringSelection selection =
                    new StringSelection(selectedValues.stream().map(this::stringsFromValue).collect(Collectors.joining("")));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        return item;
    }

    protected String stringsFromValue(T a) {
        StringBuilder s = new StringBuilder();

        for (var entry : checkboxes.entrySet()) {
            var checkbox = checkboxes.get(entry.getKey());

            if (checkbox.isSelected()) {
                s.append(checkbox.apply(a)).append(System.getProperty("line.separator", "\n"));
            }
        }

        return s.toString();
    }

    private static class CheckboxGetterPair<T> {
        JCheckBox checkBox;
        Function<T, String> getter;

        CheckboxGetterPair(JCheckBox checkBox, Function<T, String> getter) {
            this.checkBox = checkBox;
            this.getter = getter;
        }

        boolean isSelected() {
            return checkBox.isSelected();
        }

        String apply(T t) {
            return getter.apply(t);
        }
    }
}
