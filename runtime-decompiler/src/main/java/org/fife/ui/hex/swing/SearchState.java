package org.fife.ui.hex.swing;

public final class SearchState {
    private final int start;
    private final int end;
    private final boolean found;

    public SearchState(int start, int end, boolean found) {
        this.start = start;
        this.end = end;
        this.found = found;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isFound() {
        return found;
    }
}
