package org.kcc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KeywordBasedCodeCompletionTest {

    @Test
    void getAfterLines() {
        String[] a;
        a = KeywordBasedCodeCompletion.getAfterLines(0, 10, "some text");
        Assertions.assertArrayEquals(new String[]{""}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 9, "some text");
        Assertions.assertArrayEquals(new String[]{""}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 8, "some text");
        Assertions.assertArrayEquals(new String[]{"t"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 0, "some text");
        Assertions.assertArrayEquals(new String[]{"some text"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(1, 1, "some text");
        Assertions.assertArrayEquals(new String[]{"ome text"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 0, "some text\nfirst line");
        Assertions.assertArrayEquals(new String[]{"some text"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(1, 2, "some text\nfirst line");
        Assertions.assertArrayEquals(new String[]{"me text", "first line"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(2, 4, "some text\nfirst line");
        Assertions.assertArrayEquals(new String[]{" text", "first line"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 1, "some text\nfirst line\n");
        Assertions.assertArrayEquals(new String[]{"ome text"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(1, 3, "some text\nfirst line\n");
        Assertions.assertArrayEquals(new String[]{"e text", "first line"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(2, 5, "some text\nfirst line\n");
        Assertions.assertArrayEquals(new String[]{"text", "first line"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(0, 2, "some text\nfirst line\nsecond line");
        Assertions.assertArrayEquals(new String[]{"me text"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(1, 3, "some text\nfirst line\nsecond line");
        Assertions.assertArrayEquals(new String[]{"e text", "first line"}, a);
        a = KeywordBasedCodeCompletion.getAfterLines(2, 4, "some text\nfirst line\nsecond line");
        Assertions.assertArrayEquals(new String[]{" text", "first line", "second line"}, a);

    }

    @Test
    void getBeforeLines() {
        String[] a;
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 10, "ext", "some text");
        Assertions.assertArrayEquals(new String[]{"some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 9, "ext", "some text");
        Assertions.assertArrayEquals(new String[]{"some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 8, "ext", "some text");
        Assertions.assertArrayEquals(new String[]{"some "}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 7, "tex", "some text");
        Assertions.assertArrayEquals(new String[]{"some"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 0, "tex", "some text");
        Assertions.assertArrayEquals(new String[]{""}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 1, "tex", "some text");
        Assertions.assertArrayEquals(new String[]{""}, a);

        a = KeywordBasedCodeCompletion.getBeforeLines(0, 19, "ex", "first line\nsome text");
        Assertions.assertArrayEquals(new String[]{"some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(1, 19, "ex", "first line\nsome text");
        Assertions.assertArrayEquals(new String[]{"first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(2, 19, "ex", "first line\nsome text");
        Assertions.assertArrayEquals(new String[]{"first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 20, "ex", "\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(1, 20, "ex", "\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(2, 20, "ex", "\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"", "first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(0, 31, "ex", "second line\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(1, 31, "ex", "second line\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(2, 31, "ex", "second line\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"second line", "first line", "some t"}, a);
        a = KeywordBasedCodeCompletion.getBeforeLines(3, 31, "ex", "second line\nfirst line\nsome text");
        Assertions.assertArrayEquals(new String[]{"second line", "first line", "some t"}, a);

    }
}
