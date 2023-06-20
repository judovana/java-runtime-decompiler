package org.jrd.backend.data.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


class LibTest {

    @Test()
    void guessNameNoPkg() throws IOException {
        RuntimeException thrown1 = Assertions.assertThrows(RuntimeException.class, () -> {
            Lib.guessName("package a;".getBytes(StandardCharsets.UTF_8));
        }, "no class found should thro exception");
        RuntimeException thrown2 = Assertions.assertThrows(RuntimeException.class, () -> {
            Lib.guessName("".getBytes(StandardCharsets.UTF_8));
        }, "no class found should throw exception");
    }

    @Test
    void guessNameJava() throws IOException {
        String s;
        s = Lib.guessName("package a; class b".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("a.b", s);
        s = Lib.guessName("class b".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("b", s);
        s = Lib.guessName(("/*\n" +
                " * Decompiled with CFR 0.151.\n" +
                " */\n" +
                "package com.google.gson;\n" +
                "\n" +
                "import com.google.gson.Gson;\n" +
                "import com.google.gson.TypeAdapter;\n" +
                "import com.google.gson.stream.JsonReader;\n" +
                "import com.google.gson.stream.JsonToken;\n" +
                "import com.google.gson.stream.JsonWriter;\n" +
                "import java.io.IOException;\n" +
                "\n" +
                "class Gson.1\n" +
                "extends TypeAdapter<Number> {").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("com.google.gson.Gson.1", s);
        s = Lib.guessName(("package  com/google/gson;\n" +
                "\n" +
                "super class Gson$1\n" +
                "\textends TypeAdapter\n" +
                "\tversion 50:0\n" +
                "{").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("com.google.gson.Gson$1", s);
        s = Lib.guessName(("package  com/google/gson;\n" +
                "\n" +
                "super \t#15 //class Gson$1\n" +
                "\textends \t#16 //class TypeAdapter\n" +
                "\tversion 50:0\n" +
                "{").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("com.google.gson.Gson$1", s);
        s = Lib.guessName(("class com/google/gson/Gson$1 {\n" +
                "  0xCAFEBABE;\n" +
                "  0; // minor version\n" +
                "  50; // version\n" +
                "  [94] { // Constant Pool").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("com.google.gson.Gson$1", s);
        s = Lib.guessName(("class a {\n" +
                "  0xCAFEBABE;\n" +
                "  0; // minor version\n" +
                "  50; // version\n" +
                "  [94] { // Constant Pool").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("a", s);
    }

    @Test
    void guessNameIMplJava() throws IOException {
        String[] s;
        s = Lib.guessNameImpl("package a; class b".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"a", "b"}, s);
        s = Lib.guessNameImpl("class b".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"b"}, s);
        s = Lib.guessNameImpl(("/*\n" +
                " * Decompiled with CFR 0.151.\n" +
                " */\n" +
                "package com.google.gson;\n" +
                "\n" +
                "import com.google.gson.Gson;\n" +
                "import com.google.gson.TypeAdapter;\n" +
                "import com.google.gson.stream.JsonReader;\n" +
                "import com.google.gson.stream.JsonToken;\n" +
                "import com.google.gson.stream.JsonWriter;\n" +
                "import java.io.IOException;\n" +
                "\n" +
                "class Gson.1\n" +
                "extends TypeAdapter<Number> {").getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"com.google.gson", "Gson.1"}, s);
        s = Lib.guessNameImpl(("package  com/google/gson;\n" +
                "\n" +
                "super class Gson$1\n" +
                "\textends TypeAdapter\n" +
                "\tversion 50:0\n" +
                "{").getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"com.google.gson", "Gson$1"}, s);
        s = Lib.guessNameImpl(("package  com/google/gson;\n" +
                "\n" +
                "super \t#15 //class Gson$1\n" +
                "\textends \t#16 //class TypeAdapter\n" +
                "\tversion 50:0\n" +
                "{").getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"com.google.gson", "Gson$1"}, s);
        s = Lib.guessNameImpl(("class com/google/gson/Gson$1 {\n" +
                "  0xCAFEBABE;\n" +
                "  0; // minor version\n" +
                "  50; // version\n" +
                "  [94] { // Constant Pool").getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"com.google.gson", "Gson$1"}, s);
        s = Lib.guessNameImpl(("class a {\n" +
                "  0xCAFEBABE;\n" +
                "  0; // minor version\n" +
                "  50; // version\n" +
                "  [94] { // Constant Pool").getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(new String[]{"a"}, s);
    }


}