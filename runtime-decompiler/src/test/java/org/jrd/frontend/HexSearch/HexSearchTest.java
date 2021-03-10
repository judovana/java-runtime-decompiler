package org.jrd.frontend.HexSearch;

import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.hex.swing.HexSearch;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HexSearchTest {
    @Test
    void hexSearchTestsString() {
        // Create test data
        byte[] testData = "test 123 456 789 tset +-*+// te te te 123 123 56 56".getBytes(StandardCharsets.UTF_8);

        try {
            HexEditor hex = new HexEditor();
            hex.open(new ByteArrayInputStream(testData));
            HexSearch hexSearchEngine = new HexSearch(hex);

            HexSearch.HexSearchOptions type = HexSearch.HexSearchOptions.TEXT;
            String testString = "test";

            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 0);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 4);
            try {
                testString = "";
                hexSearchEngine.searchHexCode(testString, type);
                fail("Did not throw exception on empty TEXT");
            } catch (Exception e) {
                // Good
            }
            assertFalse(hexSearchEngine.getSearchState().isFound());

            testString = "te te";
            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 29);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 34);
        } catch (IOException ex) {
            // Fail in case of an exception
            fail("hex.open() failed");
        }
    }

    @Test
    void hexSearchTestsHex() {
        // Create test data
        byte[] testData = "test 123 456 789 tset +-*+// te te te 123 123 56 56".getBytes(StandardCharsets.UTF_8);

        try {
            HexEditor hex = new HexEditor();
            hex.open(new ByteArrayInputStream(testData));
            HexSearch hexSearchEngine = new HexSearch(hex);

            HexSearch.HexSearchOptions type = HexSearch.HexSearchOptions.HEX;
            String testString = "74 65 73 74";

            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 0);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 4);
            try {
                testString = "";
                hexSearchEngine.searchHexCode(testString, type);
                fail("Did not throw exception on empty HEX");
            } catch (Exception e) {
                // Good
            }
            assertFalse(hexSearchEngine.getSearchState().isFound());

            testString = "74 65 20 74 65";
            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 29);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 34);
        } catch (IOException ex) {
            // Fail in case of an exception
            fail("hex.open() failed");
        }
    }

    @Test
    void hexSearchTestsInt() {
        // Create test data
        byte[] testData = "test 123 456 789 tset +-*+// te te te 123 123 56 56".getBytes(StandardCharsets.UTF_8);

        try {
            HexEditor hex = new HexEditor();
            hex.open(new ByteArrayInputStream(testData));
            HexSearch hexSearchEngine = new HexSearch(hex);

            HexSearch.HexSearchOptions type = HexSearch.HexSearchOptions.INT;
            String testString = "116 101 115 116";

            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 0);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 4);
            try {
                testString = "";
                hexSearchEngine.searchHexCode(testString, type);
                fail("Did not throw exception on empty INT");
            } catch (Exception e) {
                // Good
            }
            assertFalse(hexSearchEngine.getSearchState().isFound());

            testString = "116 101 32 116 101";
            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 29);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 34);
        } catch (IOException ex) {
            // Fail in case of an exception
            fail("hex.open() failed");
        }
    }

    @Test
    void hexSearchTestNext() {
        // Create test data
        byte[] testData = "test 123 456 789 tset/ te te te 123 123 56 56".getBytes(StandardCharsets.UTF_8);

        try {
            HexEditor hex = new HexEditor();
            hex.open(new ByteArrayInputStream(testData));
            HexSearch hexSearchEngine = new HexSearch(hex);

            HexSearch.HexSearchOptions type = HexSearch.HexSearchOptions.TEXT;
            String testString = "123";

            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 5);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 8);

            hexSearchEngine.next(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 32);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 35);

            hexSearchEngine.next(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 36);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 39);

            hexSearchEngine.next(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 36);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 39);
        } catch (IOException ex) {
            // Fail in case of an exception
            fail("hex.open() failed");
        }
    }

    @Test
    void hexSearchTestPrev() {
        // Create test data
        byte[] testData = "test 123 456 789 tset/ te te te 123 123 56 56".getBytes(StandardCharsets.UTF_8);

        try {
            HexEditor hex = new HexEditor();
            hex.open(new ByteArrayInputStream(testData));
            HexSearch hexSearchEngine = new HexSearch(hex);

            HexSearch.HexSearchOptions type = HexSearch.HexSearchOptions.TEXT;
            String testString = "123";

            hexSearchEngine.searchHexCode(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 5);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 8);

            hexSearchEngine.next(testString, type);
            hexSearchEngine.next(testString, type);
            hexSearchEngine.next(testString, type);

            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 36);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 39);

            hexSearchEngine.previous(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 32);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 35);

            hexSearchEngine.previous(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 5);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 8);

            hexSearchEngine.previous(testString, type);
            assertTrue(hexSearchEngine.getSearchState().isFound());
            assertEquals(hexSearchEngine.getSearchState().getStart(), 5);
            assertEquals(hexSearchEngine.getSearchState().getEnd(), 8);
        } catch (IOException ex) {
            // Fail in case of an exception
            fail("hex.open() failed");
        }
    }
}
