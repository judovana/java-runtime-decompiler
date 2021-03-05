package org.fife.ui.hex.swing;

import java.util.ArrayList;

public class HexSearch {

    private HexEditor hex;
    private SearchState searchState;

    public enum HexSearchOptions {
        HEX,
        INT,
        TEXT
    }

    public HexSearch(HexEditor hex) {
        this.hex = hex;
        this.searchState = new SearchState();
    }

    private ArrayList<Byte> getByteArray(String str, HexSearchOptions type) {
        ArrayList<Byte> arr = new ArrayList<>();
        switch (type) {
            case TEXT:
                byte[] bytesText = str.getBytes();
                if (bytesText.length == 0) {
                    throw new StringIndexOutOfBoundsException();
                }
                for (byte b : bytesText) {
                    arr.add(b);
                }
                break;

            case INT:
                if (str.trim().equals("")) {
                    throw new StringIndexOutOfBoundsException();
                }
                String[] spliced = str.split(" ");
                for (String s : spliced) {
                    arr.add(Integer.valueOf(s).byteValue());
                }
                break;

            case HEX:
                if (str.trim().equals("")) {
                    throw new StringIndexOutOfBoundsException();
                }
                String[] splicedHex = str.split(" ");
                for (String s : splicedHex) {
                    int i = Integer.parseInt(s, 16);
                    arr.add(Integer.valueOf(i).byteValue());
                }
                break;

            default:
                return null;
        }
        return arr;
    }

    public void searchHexCode(String str, HexSearchOptions type) {
        try {
            ArrayList<Byte> arr = getByteArray(str, type);
            int byteCount = hex.getByteCount();
            searchState.setStart(0);
            searchState.setEnd(0);
            searchState.setFound(false);
            for (int i = 0; i < byteCount; i++) {
                if (arr.get(0) == hex.getByte(i)) {
                    if (checkIfMatches(arr, i)) {
                        searchState.setFound(true);
                        searchState.setStart(i);
                        searchState.setEnd(i + arr.size());
                        break;
                    }
                }
            }
            if (searchState.isFound()) {
                hex.setSelectedRange(searchState.getStart(), searchState.getEnd() - 1);
            }
        } catch (NumberFormatException e) {
            System.err.println("NAN");
        } catch (StringIndexOutOfBoundsException e) {
            // Ignore
        }
    }

    private boolean checkIfMatches(ArrayList<Byte> arr, int start) {
        for (int i = 0; i < arr.size(); i++) {
            if (!(arr.get(i) == hex.getByte(i + start))) {
                return false;
            }
        }
        return true;
    }

    public void next(String str, HexSearchOptions type) {
        if (!searchState.isFound()) {
            return;
        }
        ArrayList<Byte> arr = getByteArray(str, type);
        if (arr == null) {
            return;
        }
        searchState.setFound(false);
        int byteCount = hex.getByteCount();
        for (int i = searchState.getStart() + 1; i < byteCount; i++) {
            if (arr.get(0) == hex.getByte(i)) {
                if (checkIfMatches(arr, i)) {
                    searchState.setFound(true);
                    searchState.setStart(i);
                    searchState.setEnd(i + arr.size());
                    break;
                }
            }
        }
        if (searchState.isFound()) {
            hex.setSelectedRange(searchState.getStart(), searchState.getEnd() - 1);
        } else {
            searchState.setFound(true);
        }
    }

    public void previous(String str, HexSearchOptions type) {
        if (!searchState.isFound()) {
            return;
        }
        ArrayList<Byte> arr = getByteArray(str, type);
        if (arr == null) {
            return;
        }
        searchState.setFound(false);
        int byteCount = hex.getByteCount();
        for (int i = searchState.getStart() - 1; i >= 0; i--) {
            if (arr.get(0) == hex.getByte(i)) {
                if (checkIfMatches(arr, i)) {
                    searchState.setFound(true);
                    searchState.setStart(i);
                    searchState.setEnd(i + arr.size());
                    break;
                }
            }
        }
        if (searchState.isFound()) {
            hex.setSelectedRange(searchState.getStart(), searchState.getEnd() - 1);
        } else {
            searchState.setFound(true);
        }
    }
}
