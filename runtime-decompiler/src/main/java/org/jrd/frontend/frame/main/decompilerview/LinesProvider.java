package org.jrd.frontend.frame.main.decompilerview;

import java.io.File;
import java.util.List;

public interface LinesProvider {

    enum LinesFormat {
        CHARS,
        HEX
    }

    List<String> getLines(LinesFormat type);

    String getName();

    void setLines(LinesFormat type, List<String> nwContent) throws Exception;

    boolean isBin();

    default boolean isText() {
        return ! isBin();
    }

    File getFile();
    void setFile(File f);

}
