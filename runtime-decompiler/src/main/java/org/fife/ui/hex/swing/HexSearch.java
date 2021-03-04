package org.fife.ui.hex.swing;

import java.util.ArrayList;

public class HexSearch {

    private HexEditor hex;
    private int start = 0;
    int end = 0;
    int counter = 0;
    boolean found = false;

    public enum HexSearchOptions {
        HEX,
        INT,
        TEXT
    }

    public HexSearch(HexEditor hex) {
        this.hex = hex;
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
                    try {
                        arr.add(Integer.valueOf(s).byteValue());
                    } catch (NumberFormatException e) {
                        throw e;
                    }
                }
                break;

            case HEX:
                if (str.trim().equals("")) {
                    throw new StringIndexOutOfBoundsException();
                }
                String[] splicedHex = str.split(" ");
                for (String s : splicedHex) {
                    try {
                        int i = Integer.parseInt(s, 16);
                        arr.add(Integer.valueOf(i).byteValue());
                    } catch (NumberFormatException e) {
                        throw e;
                    }
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
            start = 0;
            end = 0;
            counter = 0;
            found = false;
            for (int i = 0; i < byteCount; i++) {
                if (arr.get(0) == hex.getByte(i)) {
                    if (checkIfMatches(arr, i)) {
                        found = true;
                        start = i;
                        end = i + arr.size();
                        break;
                    }
                }
            }
            if (found) {
                hex.setSelectedRange(start, end - 1);
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
        if (!found) {
            return;
        }
        ArrayList<Byte> arr = getByteArray(str, type);
        if (arr == null) {
            return;
        }
        found = false;
        int byteCount = hex.getByteCount();
        for (int i = start + 1; i < byteCount; i++) {
            if (arr.get(0) == hex.getByte(i)) {
                if (checkIfMatches(arr, i)) {
                    found = true;
                    start = i;
                    end = i + arr.size();
                    break;
                }
            }
        }
        if (found) {
            hex.setSelectedRange(start, end - 1);
        } else {
            found = true;
        }
    }

    public void previous(String str, HexSearchOptions type) {
        if (!found) {
            return;
        }
        ArrayList<Byte> arr = getByteArray(str, type);
        if (arr == null) {
            return;
        }
        found = false;
        int byteCount = hex.getByteCount();
        for (int i = start - 1; i >= 0; i--) {
            if (arr.get(0) == hex.getByte(i)) {
                if (checkIfMatches(arr, i)) {
                    found = true;
                    start = i;
                    end = i + arr.size();
                    break;
                }
            }
        }
        if (found) {
            hex.setSelectedRange(start, end - 1);
        } else {
            found = true;
        }
    }
}
