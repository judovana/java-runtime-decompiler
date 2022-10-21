package org.jrd.frontend.frame.main.popup;

public class SingleFilePatch {
    private final int start;
    private final int end;

    public SingleFilePatch(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
