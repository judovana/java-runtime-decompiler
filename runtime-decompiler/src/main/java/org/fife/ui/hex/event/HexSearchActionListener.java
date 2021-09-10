package org.fife.ui.hex.event;

import org.fife.ui.hex.swing.HexSearch;
import org.jrd.backend.core.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HexSearchActionListener implements ActionListener {


    private final HexSearch hexSearchEngine;
    private final JTextField hexSearch;
    private final JComboBox<HexSearch.HexSearchOptions> hexSearchType;
    private final Method method;

    public enum Method {
        NEXT,
        PREV
    }

    public HexSearchActionListener(
            HexSearch hexSearchEngine,
            JTextField hexSearch,
            JComboBox<HexSearch.HexSearchOptions> hexSearchType,
            Method method
    ) {
        this.hexSearchEngine = hexSearchEngine;
        this.hexSearch = hexSearch;
        this.hexSearchType = hexSearchType;
        this.method = method;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            switch (method) {
                case NEXT:
                    hexSearchEngine.next(
                            hexSearch.getText(), (HexSearch.HexSearchOptions) hexSearchType.getSelectedItem()
                    );
                    break;
                case PREV:
                    hexSearchEngine.previous(
                            hexSearch.getText(), (HexSearch.HexSearchOptions) hexSearchType.getSelectedItem()
                    );
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Logger.getLogger().log(e);
        }
    }
}
