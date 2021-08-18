package org.fife.ui.hex.event;

import org.fife.ui.hex.swing.HexSearch;
import org.jrd.backend.core.OutputController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class HexSearchDocumentListener implements DocumentListener {

    private final HexSearch hexSearchEngine;
    private final JTextField hexSearch;
    private final JComboBox<HexSearch.HexSearchOptions> hexSearchType;

    public HexSearchDocumentListener(HexSearch hexSearchEngine, JTextField hexSearch, JComboBox<HexSearch.HexSearchOptions> hexSearchType) {
        this.hexSearchEngine = hexSearchEngine;
        this.hexSearch = hexSearch;
        this.hexSearchType = hexSearchType;
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
            hexSearchEngine.searchHexCode(hexSearch.getText(), (HexSearch.HexSearchOptions) hexSearchType.getSelectedItem());
        } catch (Exception e) {
            OutputController.getLogger().log(e);
        }
    }
}
