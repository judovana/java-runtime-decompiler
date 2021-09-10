package org.fife.ui.hex.event;

import org.fife.ui.hex.swing.HexSearch;
import org.jrd.backend.core.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;

public class HexSearchDocumentListener implements DocumentListener {

    private final HexSearch hexSearchEngine;
    private final JTextField hexSearch;
    private final JComboBox<HexSearch.HexSearchOptions> hexSearchType;
    private ActionListener wasNotFoundListener;

    public HexSearchDocumentListener(
            HexSearch hexSearchEngine, JTextField hexSearch, JComboBox<HexSearch.HexSearchOptions> hexSearchType
    ) {
        this.hexSearchEngine = hexSearchEngine;
        this.hexSearch = hexSearch;
        this.hexSearchType = hexSearchType;
    }

    public HexSearchDocumentListener(
            HexSearch hexSearchEngine,
            JTextField hexSearch,
            JComboBox<HexSearch.HexSearchOptions> hexSearchType,
            ActionListener wasNotFoundListener
    ) {
        this(hexSearchEngine, hexSearch, hexSearchType);
        this.wasNotFoundListener = wasNotFoundListener;
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        find();
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        find();
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        find();
    }

    private void find() {
        try {
            boolean wasFound = hexSearchEngine.searchHexCode(
                    hexSearch.getText(), (HexSearch.HexSearchOptions) hexSearchType.getSelectedItem()
            );

            if (!wasFound && wasNotFoundListener != null) {
                wasNotFoundListener.actionPerformed(null);
            }
        } catch (Exception e) {
            Logger.getLogger().log(e);
        }
    }
}
