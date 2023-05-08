package org.jrd.frontend.frame.main.decompilerview;

import javax.swing.JComponent;
import java.io.File;
import java.io.IOException;
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
        return !isBin();
    }

    File getFile();

    void setFile(File f);

    void open(File f) throws IOException;

    void save(File f) throws IOException;

    JComponent asComponent();

    void undo();

    void redo();

    void resetUndoRedo();

    void close();

}
