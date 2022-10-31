package org.jrd.backend.data.cli.utils;

import org.jrd.backend.core.Logger;
import org.jrd.frontend.utility.CommonUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Saving implements CommonUtils.StatusKeeper {
    public static final String DEFAULT = "default";
    public static final String EXACT = "exact";
    public static final String FQN = "fqn";
    public static final String DIR = "dir";
    private final String as;
    private final String like;

    public Saving(String as, String like) {
        this.as = as;
        if (like == null) {
            this.like = DEFAULT;
        } else {
            this.like = like;
        }
    }

    public boolean shouldSave() {
        return getAs() != null;
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
        switch (getLike()) {
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
                throw new RuntimeException("Unknown saving type: " + getLike() + ". Allowed are: " + FQN + "," + DIR + "," + EXACT);
        }
    }

    public PrintStream openPrintStream() throws IOException {
        return new PrintStream(new FileOutputStream(this.getAs()), true, "UTF-8");
    }

    public String getAs() {
        return as;
    }

    public String getLike() {
        return like;
    }
}
