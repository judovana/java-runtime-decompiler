package org.jrd.frontend.frame.main.popup;

import org.jrd.backend.core.VmDecompilerStatus;
import org.jrd.backend.data.DependenciesReader;
import org.jrd.backend.data.cli.Lib;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JListPopupMenu<T> extends JPopupMenu {

    Map<String, CheckboxGetterPair<T>> checkboxes = new LinkedHashMap<>();
    final boolean showCheckBoxes;
    final Optional<String> classloader;

    public JListPopupMenu(
            JList<T> parentJList, boolean showCheckboxes, DependenciesReader dependenciesReader, Optional<String> classloader
    ) {
        this.showCheckBoxes = showCheckboxes;
        this.classloader = classloader;
        add(createCopyItem("Copy selected", parentJList.getSelectedValuesList()));
        add(createCopyItem("Copy all", allItems(parentJList.getModel())));

        JMenuItem helpItem = new JMenuItem("In class-list, drag with right-mouse-button to multi-select");
        helpItem.setEnabled(false);
        addSeparator();
        add(helpItem);
        addSeparator();
        JMenuItem deps = new JMenuItem("Print dependencies");
        add(deps);
        deps.addActionListener(new DepndencyResolverListener(dependenciesReader, parentJList.getSelectedValuesList()));
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

        for (Map.Entry entry : checkboxes.entrySet()) {
            CheckboxGetterPair<T> checkbox = checkboxes.get(entry.getKey());

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

    private class DepndencyResolverListener implements ActionListener {
        private final DependenciesReader mDependenciesReader;
        private final List<T> mParentJList;

        DepndencyResolverListener(DependenciesReader dependenciesReader, List<T> parentJList) {
            mDependenciesReader = dependenciesReader;
            mParentJList = parentJList;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            mDependenciesReader.getGui().showLoadingDialog(a -> mDependenciesReader.getGui().hideLoadingDialog(), "Resolving dependencies");
            new ClassResolutionInBackground().execute();
        }

        private class ClassResolutionInBackground extends SwingWorker<String, Void> {
            @Override
            protected String doInBackground() throws Exception {
                final StringBuilder r = new StringBuilder("");
                List<String> classes = new ArrayList<>();
                Set<String> allDeps = new HashSet<>();
                try {
                    classes.addAll(mParentJList.stream().map(JListPopupMenu.this::stringsFromValue).collect(Collectors.toList()));
                    for (String clazz : classes) {
                        VmDecompilerStatus result =
                                Lib.obtainClass(mDependenciesReader.getVmInfo(), clazz, mDependenciesReader.getVmManager(), classloader);
                        Collection<String> deps1 = mDependenciesReader.resolve(clazz, result.getLoadedClassBytes());
                        allDeps.addAll(deps1);
                    }
                    r.append(allDeps.stream().sorted().collect(Collectors.joining("\n")));
                } finally {
                    mDependenciesReader.getGui().hideLoadingDialog();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JFrame wind = new JFrame();
                        wind.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        wind.setSize(new Dimension(800, 600));
                        wind.add(new JScrollPane(new JTextArea(r.toString())));
                        JButton jb = new JButton("close");
                        jb.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                wind.dispose();
                            }
                        });
                        wind.add(jb, BorderLayout.SOUTH);
                        JTextField jtf;
                        if (classes.size() == 1) {
                            jtf = new JTextField(classes.get(0) + " has deps: " + allDeps.size());
                        } else {
                            jtf = new JTextField(classes.size() + " classes have deps: " + allDeps.size());
                        }
                        wind.setTitle(jtf.getText());
                        jtf.setEditable(false);
                        wind.add(jtf, BorderLayout.NORTH);
                        ScreenFinder.centerWindowToCurrentScreen(wind);
                        wind.setVisible(true);
                    }
                });
                return r.toString();
            }
        }
    }
}
