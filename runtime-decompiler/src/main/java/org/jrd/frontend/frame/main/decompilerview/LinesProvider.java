package org.jrd.frontend.frame.main.decompilerview;

import java.util.List;

public interface LinesProvider {

    enum LinesFormat {
        CHARS,
        HEX
    }

    List<String> getLines(LinesFormat type);

    String getName();

    void setLines(LinesFormat type, String nwContent);
}
