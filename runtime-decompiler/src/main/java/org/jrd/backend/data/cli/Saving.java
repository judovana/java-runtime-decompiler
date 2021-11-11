package org.jrd.backend.data.cli;

import org.jrd.backend.core.Logger;
import org.jrd.frontend.utility.CommonUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class Saving implements CommonUtils.StatusKeeper {
    static final String DEFAULT = "default";
    static final String EXACT = "exact";
    static final String FQN = "fqn";
    static final String DIR = "dir";
    final String as;
    final String like;

    Saving(String as, String like) {
        this.as = as;
        if (like == null) {
            this.like = DEFAULT;
        } else {
            this.like = like;
        }
    }

    public boolean shouldSave() {
        return as != null;
    }

    @Override
    public void setText(String s) {
        if (shouldSave()) {
            System.out.println(s);
        } else {
            System.err.println(s);
        }
    }

    @Override
    public void onException(Exception ex) {
        Logger.getLogger().log(ex);
    }

    @SuppressWarnings("ReturnCount") // returns in switch cases
    public int toInt(String suffix) {
        switch (like) {
            case FQN:
                return CommonUtils.FULLY_QUALIFIED_NAME;
            case EXACT:
                return CommonUtils.CUSTOM_NAME;
            case DIR:
                return CommonUtils.SRC_SUBDIRS_NAME;
            case DEFAULT:
                if (".java".equals(suffix)) {
                    return CommonUtils.FULLY_QUALIFIED_NAME;
                }
                if (".class".equals(suffix)) {
                    return CommonUtils.SRC_SUBDIRS_NAME;
                }
                return CommonUtils.CUSTOM_NAME;
            default:
                throw new RuntimeException("Unknown saving type: " + like + ". Allowed are: " + FQN + "," + DIR + "," + EXACT);
        }
    }

    public PrintStream openPrintStream() throws IOException {
        return new PrintStream(new FileOutputStream(this.as), true, "UTF-8");
    }
}
