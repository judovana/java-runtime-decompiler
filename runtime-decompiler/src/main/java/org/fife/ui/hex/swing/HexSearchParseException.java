package org.fife.ui.hex.swing;

public class HexSearchParseException extends Exception {
    public HexSearchParseException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public String getName() {
        return "HexSearch parse error";
    }
}