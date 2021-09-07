package org.fife.ui.hex.swing;

public class HexSearchParseException extends RuntimeException {
    public HexSearchParseException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public String getName() {
        return "HexSearch parse error";
    }
}
