package org.jrd.frontend.frame.settings;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public interface ChangeReporter {

    /**
     * Passes a listener to the implementing class to allow for future reporting.
     * @param listener the change listener
     */
    void setChangeReporter(ActionListener listener);

    /**
     * Convenience method to make a document change listener perform an action on the listener parameter.
     * @param listener the listener which will be triggered
     * @param textComponent the component whose document will trigger an event
     */
    static void addTextChangeListener(ActionListener listener, JTextComponent textComponent) {
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changedUpdate(documentEvent);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changedUpdate(documentEvent);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                listener.actionPerformed(createChangeActionEvent(textComponent));
            }
        });
    }

    static void addCheckboxListener(ActionListener listener, JToggleButton checkBox) {
        checkBox.addActionListener(checked -> listener.actionPerformed(createChangeActionEvent(checkBox)));
    }

    static void addJListListener(ActionListener listener, JList list) {
        list.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                contentsChanged(listDataEvent);
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                contentsChanged(listDataEvent);
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
                listener.actionPerformed(createChangeActionEvent(list));
            }
        });
    }

    static ActionEvent createChangeActionEvent(Object source) {
        return new ActionEvent(source, 0, "changed");
    }
}
