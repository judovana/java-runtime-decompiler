package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BytecodeKeywordsWithHelp implements CompletionItem.CompletionItemSet {

    private static final CompletionItem[] BYTECODE_KEYWORDS = {
            new CompletionItem("aaload", "32\n0011 0010\n arrayref, index → value\nload onto the stack a reference " + "from an array"),
            new CompletionItem("aastore", "53\n0101 0011\n arrayref, index, value →\nstore a reference in an array"),
            new CompletionItem("aconst_null", "01\n0000 0001\n → null\npush a null reference onto the stack"),
            new CompletionItem(
                    "aload", "19\n0001 1001\n1: index\n→ objectref\nload a reference onto the stack from a local " + "variable #index"
            ), new CompletionItem("aload_0", "2a\n0010 1010\n → objectref\nload a reference onto the stack from local" + " variable 0"),
            new CompletionItem("aload_1", "2b\n0010 1011\n → objectref\nload a reference onto the stack from local " + "variable 1"),
            new CompletionItem("aload_2", "2c\n0010 1100\n → objectref\nload a reference onto the stack from local " + "variable 2"),
            new CompletionItem("aload_3", "2d\n0010 1101\n → objectref\nload a reference onto the stack from local " + "variable 3"),
            new CompletionItem(
                    "anewarray",
                    "bd\n1011 1101\n2: indexbyte1, indexbyte2\ncount → arrayref\ncreate a new array of references of " +
                            "length count and component type identified by the class reference index (indexbyte1 << 8" +
                            " | indexbyte2) in the constant pool"
            ), new CompletionItem("areturn", "b0\n1011 0000\n objectref → [empty]\nreturn a reference from a method"),
            new CompletionItem("arraylength", "be\n1011 1110\n arrayref → length\nget the length of an array"),
            new CompletionItem("astore", "3a\n0011 1010\n1: index\nobjectref →\nstore a reference into a local " + "variable #index"),
            new CompletionItem("astore_0", "4b\n0100 1011\n objectref →\nstore a reference into local variable 0"),
            new CompletionItem("astore_1", "4c\n0100 1100\n objectref →\nstore a reference into local variable 1"),
            new CompletionItem("astore_2", "4d\n0100 1101\n objectref →\nstore a reference into local variable 2"),
            new CompletionItem("astore_3", "4e\n0100 1110\n objectref →\nstore a reference into local variable 3"),
            new CompletionItem(
                    "athrow",
                    "bf\n1011 1111\n objectref → [empty], objectref\nthrows an error or exception (notice that the " +
                            "rest of the stack is cleared, leaving only a reference to the Throwable)"
            ), new CompletionItem("baload", "33\n0011 0011\n arrayref, index → value\nload a byte or Boolean value " + "from an array"),
            new CompletionItem("bastore", "54\n0101 0100\n arrayref, index, value →\nstore a byte or Boolean value " + "into an array"),
            new CompletionItem("bipush", "10\n0001 0000\n1: byte\n→ value\npush a byte onto the stack as an integer " + "value"),
            new CompletionItem(
                    "breakpoint", "ca\n1100 1010\n  reserved for breakpoints in Java debuggers; should not appear in " + "any class file"
            ), new CompletionItem("caload", "34\n0011 0100\n arrayref, index → value\nload a char from an array"),
            new CompletionItem("castore", "55\n0101 0101\n arrayref, index, value →\nstore a char into an array"),
            new CompletionItem(
                    "checkcast",
                    "c0\n1100 0000\n2: indexbyte1, indexbyte2\nobjectref → objectref\nchecks whether an objectref is " +
                            "of a certain type, the class reference of which is in the constant pool at index " +
                            "(indexbyte1 << 8 | indexbyte2)"
            ), new CompletionItem("d2f", "90\n1001 0000\n value → result\nconvert a double to a float"),
            new CompletionItem("d2i", "8e\n1000 1110\n value → result\nconvert a double to an int"),
            new CompletionItem("d2l", "8f\n1000 1111\n value → result\nconvert a double to a long"),
            new CompletionItem("dadd", "63\n0110 0011\n value1, value2 → result\nadd two doubles"),
            new CompletionItem("daload", "31\n0011 0001\n arrayref, index → value\nload a double from an array"),
            new CompletionItem("dastore", "52\n0101 0010\n arrayref, index, value →\nstore a double into an array"),
            new CompletionItem("dcmpg", "98\n1001 1000\n value1, value2 → result\ncompare two doubles, 1 on NaN"),
            new CompletionItem("dcmpl", "97\n1001 0111\n value1, value2 → result\ncompare two doubles, -1 on NaN"),
            new CompletionItem("dconst_0", "0e\n0000 1110\n → 0.0\npush the constant 0.0 (a double) onto the stack"),
            new CompletionItem("dconst_1", "0f\n0000 1111\n → 1.0\npush the constant 1.0 (a double) onto the stack"),
            new CompletionItem("ddiv", "6f\n0110 1111\n value1, value2 → result\ndivide two doubles"),
            new CompletionItem("dload", "18\n0001 1000\n1: index\n→ value\nload a double value from a local variable " + "#index"),
            new CompletionItem("dload_0", "26\n0010 0110\n → value\nload a double from local variable 0"),
            new CompletionItem("dload_1", "27\n0010 0111\n → value\nload a double from local variable 1"),
            new CompletionItem("dload_2", "28\n0010 1000\n → value\nload a double from local variable 2"),
            new CompletionItem("dload_3", "29\n0010 1001\n → value\nload a double from local variable 3"),
            new CompletionItem("dmul", "6b\n0110 1011\n value1, value2 → result\nmultiply two doubles"),
            new CompletionItem("dneg", "77\n0111 0111\n value → result\nnegate a double"),
            new CompletionItem(
                    "drem", "73\n0111 0011\n value1, value2 → result\nget the remainder from a division " + "between two doubles"
            ), new CompletionItem("dreturn", "af\n1010 1111\n value → [empty]\nreturn a double from a method"),
            new CompletionItem("dstore", "39\n0011 1001\n1: index\nvalue →\nstore a double value into a local " + "variable #index"),
            new CompletionItem("dstore_0", "47\n0100 0111\n value →\nstore a double into local variable 0"),
            new CompletionItem("dstore_1", "48\n0100 1000\n value →\nstore a double into local variable 1"),
            new CompletionItem("dstore_2", "49\n0100 1001\n value →\nstore a double into local variable 2"),
            new CompletionItem("dstore_3", "4a\n0100 1010\n value →\nstore a double into local variable 3"),
            new CompletionItem("dsub", "67\n0110 0111\n value1, value2 → result\nsubtract a double from another"),
            new CompletionItem("dup", "59\n0101 1001\n value → value, value\nduplicate the value on top of the stack"),
            new CompletionItem(
                    "dup_x1",
                    "5a\n0101 1010\n value2, value1 → value1, value2, value1\ninsert a copy of the top value into the" +
                            " stack two values from the top. value1 and value2 must not be of the type double or long."
            ),
            new CompletionItem(
                    "dup_x2",
                    "5b\n0101 1011\n value3, value2, value1 → value1, value3, value2, value1\ninsert a copy of the " +
                            "top value into the stack two (if value2 is double or long it takes up the entry of " +
                            "value3, too) or three values (if value2 is neither double nor long) from the top"
            ),
            new CompletionItem(
                    "dup2",
                    "5c\n0101 1100\n {value2, value1} → {value2, value1}, {value2, value1}\nduplicate top two stack " +
                            "words (two values, if value1 is not double nor long; a single value, if value1 is double" + " or long)"
            ),
            new CompletionItem(
                    "dup2_x1",
                    "5d\n0101 1101\n value3, {value2, value1} → {value2, value1}, value3, {value2, value1}\nduplicate" +
                            " two words and insert beneath third word (see explanation above)"
            ),
            new CompletionItem(
                    "dup2_x2",
                    "5e\n0101 1110\n {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}, " +
                            "{value2, value1}\nduplicate two words and insert beneath fourth word"
            ), new CompletionItem("f2d", "8d\n1000 1101\n value → result\nconvert a float to a double"),
            new CompletionItem("f2i", "8b\n1000 1011\n value → result\nconvert a float to an int"),
            new CompletionItem("f2l", "8c\n1000 1100\n value → result\nconvert a float to a long"),
            new CompletionItem("fadd", "62\n0110 0010\n value1, value2 → result\nadd two floats"),
            new CompletionItem("faload", "30\n0011 0000\n arrayref, index → value\nload a float from an array"),
            new CompletionItem("fastore", "51\n0101 0001\n arrayref, index, value →\nstore a float in an array"),
            new CompletionItem("fcmpg", "96\n1001 0110\n value1, value2 → result\ncompare two floats, 1 on NaN"),
            new CompletionItem("fcmpl", "95\n1001 0101\n value1, value2 → result\ncompare two floats, -1 on NaN"),
            new CompletionItem("fconst_0", "0b\n0000 1011\n → 0.0f\npush 0.0f on the stack"),
            new CompletionItem("fconst_1", "0c\n0000 1100\n → 1.0f\npush 1.0f on the stack"),
            new CompletionItem("fconst_2", "0d\n0000 1101\n → 2.0f\npush 2.0f on the stack"),
            new CompletionItem("fdiv", "6e\n0110 1110\n value1, value2 → result\ndivide two floats"),
            new CompletionItem("fload", "17\n0001 0111\n1: index\n→ value\nload a float value from a local variable " + "#index"),
            new CompletionItem("fload_0", "22\n0010 0010\n → value\nload a float value from local variable 0"),
            new CompletionItem("fload_1", "23\n0010 0011\n → value\nload a float value from local variable 1"),
            new CompletionItem("fload_2", "24\n0010 0100\n → value\nload a float value from local variable 2"),
            new CompletionItem("fload_3", "25\n0010 0101\n → value\nload a float value from local variable 3"),
            new CompletionItem("fmul", "6a\n0110 1010\n value1, value2 → result\nmultiply two floats"),
            new CompletionItem("fneg", "76\n0111 0110\n value → result\nnegate a float"),
            new CompletionItem(
                    "frem", "72\n0111 0010\n value1, value2 → result\nget the remainder from a division " + "between two floats"
            ), new CompletionItem("freturn", "ae\n1010 1110\n value → [empty]\nreturn a float"),
            new CompletionItem("fstore", "38\n0011 1000\n1: index\nvalue →\nstore a float value into a local variable" + " #index"),
            new CompletionItem("fstore_0", "43\n0100 0011\n value →\nstore a float value into local variable 0"),
            new CompletionItem("fstore_1", "44\n0100 0100\n value →\nstore a float value into local variable 1"),
            new CompletionItem("fstore_2", "45\n0100 0101\n value →\nstore a float value into local variable 2"),
            new CompletionItem("fstore_3", "46\n0100 0110\n value →\nstore a float value into local variable 3"),
            new CompletionItem("fsub", "66\n0110 0110\n value1, value2 → result\nsubtract two floats"),
            new CompletionItem(
                    "getfield",
                    "b4\n1011 0100\n2: indexbyte1, indexbyte2\nobjectref → value\nget a field value of an object " +
                            "objectref, where the field is identified by field reference in the constant pool index " +
                            "(indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "getstatic",
                    "b2\n1011 0010\n2: indexbyte1, indexbyte2\n→ value\nget a static field value of a class, where " +
                            "the field is identified by field reference in the constant pool index (indexbyte1 << 8 |" + " indexbyte2)"
            ),
            new CompletionItem(
                    "goto",
                    "a7\n1010 0111\n2: branchbyte1, branchbyte2\n[no change]\ngoes to another instruction at " +
                            "branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "goto_w",
                    "c8\n1100 1000\n4: branchbyte1, branchbyte2, branchbyte3, branchbyte4\n[no change]\ngoes to " +
                            "another instruction at branchoffset (signed int constructed from unsigned bytes " +
                            "branchbyte1 << 24 | branchbyte2 << 16 | branchbyte3 << 8 | branchbyte4)"
            ), new CompletionItem("i2b", "91\n1001 0001\n value → result\nconvert an int into a byte"),
            new CompletionItem("i2c", "92\n1001 0010\n value → result\nconvert an int into a character"),
            new CompletionItem("i2d", "87\n1000 0111\n value → result\nconvert an int into a double"),
            new CompletionItem("i2f", "86\n1000 0110\n value → result\nconvert an int into a float"),
            new CompletionItem("i2l", "85\n1000 0101\n value → result\nconvert an int into a long"),
            new CompletionItem("i2s", "93\n1001 0011\n value → result\nconvert an int into a short"),
            new CompletionItem("iadd", "60\n0110 0000\n value1, value2 → result\nadd two ints"),
            new CompletionItem("iaload", "2e\n0010 1110\n arrayref, index → value\nload an int from an array"),
            new CompletionItem("iand", "7e\n0111 1110\n value1, value2 → result\nperform a bitwise AND on two " + "integers"),
            new CompletionItem("iastore", "4f\n0100 1111\n arrayref, index, value →\nstore an int into an array"),
            new CompletionItem("iconst_m1", "02\n0000 0010\n → -1\nload the int value −1 onto the stack"),
            new CompletionItem("iconst_0", "03\n0000 0011\n → 0\nload the int value 0 onto the stack"),
            new CompletionItem("iconst_1", "04\n0000 0100\n → 1\nload the int value 1 onto the stack"),
            new CompletionItem("iconst_2", "05\n0000 0101\n → 2\nload the int value 2 onto the stack"),
            new CompletionItem("iconst_3", "06\n0000 0110\n → 3\nload the int value 3 onto the stack"),
            new CompletionItem("iconst_4", "07\n0000 0111\n → 4\nload the int value 4 onto the stack"),
            new CompletionItem("iconst_5", "08\n0000 1000\n → 5\nload the int value 5 onto the stack"),
            new CompletionItem("idiv", "6c\n0110 1100\n value1, value2 → result\ndivide two integers"),
            new CompletionItem(
                    "if_acmpeq",
                    "a5\n1010 0101\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif references are equal, branch to" +
                            " instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 " +
                            "<< 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_acmpne",
                    "a6\n1010 0110\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif references are not equal, " +
                            "branch to instruction at branchoffset (signed short constructed from unsigned bytes " +
                            "branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmpeq",
                    "9f\n1001 1111\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif ints are equal, branch to " +
                            "instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 <<" + " 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmpge",
                    "a2\n1010 0010\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif value1 is greater than or equal" +
                            " to value2, branch to instruction at branchoffset (signed short constructed from " +
                            "unsigned bytes branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmpgt",
                    "a3\n1010 0011\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif value1 is greater than value2, " +
                            "branch to instruction at branchoffset (signed short constructed from unsigned bytes " +
                            "branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmple",
                    "a4\n1010 0100\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif value1 is less than or equal to" +
                            " value2, branch to instruction at branchoffset (signed short constructed from unsigned " +
                            "bytes branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmplt",
                    "a1\n1010 0001\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif value1 is less than value2, " +
                            "branch to instruction at branchoffset (signed short constructed from unsigned bytes " +
                            "branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "if_icmpne",
                    "a0\n1010 0000\n2: branchbyte1, branchbyte2\nvalue1, value2 →\nif ints are not equal, branch to " +
                            "instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 <<" + " 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "ifeq",
                    "99\n1001 1001\n2: branchbyte1, branchbyte2\nvalue →\nif value is 0, branch to instruction at " +
                            "branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "ifge",
                    "9c\n1001 1100\n2: branchbyte1, branchbyte2\nvalue →\nif value is greater than or equal to 0, " +
                            "branch to instruction at branchoffset (signed short constructed from unsigned bytes " +
                            "branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "ifgt",
                    "9d\n1001 1101\n2: branchbyte1, branchbyte2\nvalue →\nif value is greater than 0, branch to " +
                            "instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 <<" + " 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "ifle",
                    "9e\n1001 1110\n2: branchbyte1, branchbyte2\nvalue →\nif value is less than or equal to 0, branch" +
                            " to instruction at branchoffset (signed short constructed from unsigned bytes " +
                            "branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "iflt",
                    "9b\n1001 1011\n2: branchbyte1, branchbyte2\nvalue →\nif value is less than 0, branch to " +
                            "instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 <<" + " 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "ifne",
                    "9a\n1001 1010\n2: branchbyte1, branchbyte2\nvalue →\nif value is not 0, branch to instruction at" +
                            " branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | " + "branchbyte2)"
            ),
            new CompletionItem(
                    "ifnonnull",
                    "c7\n1100 0111\n2: branchbyte1, branchbyte2\nvalue →\nif value is not null, branch to instruction" +
                            " at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | " + "branchbyte2)"
            ),
            new CompletionItem(
                    "ifnull",
                    "c6\n1100 0110\n2: branchbyte1, branchbyte2\nvalue →\nif value is null, branch to instruction at " +
                            "branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)"
            ),
            new CompletionItem(
                    "iinc", "84\n1000 0100\n2: index, const\n[No change]\nincrement local variable #index " + "by signed byte const"
            ), new CompletionItem("iload", "15\n0001 0101\n1: index\n→ value\nload an int value from a local variable " + "#index"),
            new CompletionItem("iload_0", "1a\n0001 1010\n → value\nload an int value from local variable 0"),
            new CompletionItem("iload_1", "1b\n0001 1011\n → value\nload an int value from local variable 1"),
            new CompletionItem("iload_2", "1c\n0001 1100\n → value\nload an int value from local variable 2"),
            new CompletionItem("iload_3", "1d\n0001 1101\n → value\nload an int value from local variable 3"),
            new CompletionItem(
                    "impdep1",
                    "fe\n1111 1110\n  reserved for implementation-dependent operations within debuggers; should not " +
                            "appear in any class file"
            ),
            new CompletionItem(
                    "impdep2",
                    "ff\n1111 1111\n  reserved for implementation-dependent operations within debuggers; should not " +
                            "appear in any class file"
            ), new CompletionItem("imul", "68\n0110 1000\n value1, value2 → result\nmultiply two integers"),
            new CompletionItem("ineg", "74\n0111 0100\n value → result\nnegate int"),
            new CompletionItem(
                    "instanceof",
                    "c1\n1100 0001\n2: indexbyte1, indexbyte2\nobjectref → result\ndetermines if an object objectref " +
                            "is of a given type, identified by class reference index in constant pool (indexbyte1 << " + "8 | indexbyte2)"
            ),
            new CompletionItem(
                    "invokedynamic",
                    "ba\n1011 1010\n4: indexbyte1, indexbyte2, 0, 0\n[arg1, arg2, ...] → result\ninvokes a dynamic " +
                            "method and puts the result on the stack (might be void); the method is identified by " +
                            "method reference index in constant pool (indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "invokeinterface",
                    "b9\n1011 1001\n4: indexbyte1, indexbyte2, count, 0\nobjectref, [arg1, arg2, ...] → " +
                            "result\ninvokes an interface method on object objectref and puts the result on the stack" +
                            " (might be void); the interface method is identified by method reference index in " +
                            "constant pool (indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "invokespecial",
                    "b7\n1011 0111\n2: indexbyte1, indexbyte2\nobjectref, [arg1, arg2, ...] → result\ninvoke instance" +
                            " method on object objectref and puts the result on the stack (might be void); the method" +
                            " is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "invokestatic",
                    "b8\n1011 1000\n2: indexbyte1, indexbyte2\n[arg1, arg2, ...] → result\ninvoke a static method and" +
                            " puts the result on the stack (might be void); the method is identified by method " +
                            "reference index in constant pool (indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "invokevirtual",
                    "b6\n1011 0110\n2: indexbyte1, indexbyte2\nobjectref, [arg1, arg2, ...] → result\ninvoke virtual " +
                            "method on object objectref and puts the result on the stack (might be void); the method " +
                            "is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)"
            ), new CompletionItem("ior", "80\n1000 0000\n value1, value2 → result\nbitwise int OR"),
            new CompletionItem("irem", "70\n0111 0000\n value1, value2 → result\nlogical int remainder"),
            new CompletionItem("ireturn", "ac\n1010 1100\n value → [empty]\nreturn an integer from a method"),
            new CompletionItem("ishl", "78\n0111 1000\n value1, value2 → result\nint shift left"),
            new CompletionItem("ishr", "7a\n0111 1010\n value1, value2 → result\nint arithmetic shift right"),
            new CompletionItem("istore", "36\n0011 0110\n1: index\nvalue →\nstore int value into variable #index"),
            new CompletionItem("istore_0", "3b\n0011 1011\n value →\nstore int value into variable 0"),
            new CompletionItem("istore_1", "3c\n0011 1100\n value →\nstore int value into variable 1"),
            new CompletionItem("istore_2", "3d\n0011 1101\n value →\nstore int value into variable 2"),
            new CompletionItem("istore_3", "3e\n0011 1110\n value →\nstore int value into variable 3"),
            new CompletionItem("isub", "64\n0110 0100\n value1, value2 → result\nint subtract"),
            new CompletionItem("iushr", "7c\n0111 1100\n value1, value2 → result\nint logical shift right"),
            new CompletionItem("ixor", "82\n1000 0010\n value1, value2 → result\nint xor"),
            new CompletionItem(
                    "jsr†",
                    "a8\n1010 1000\n2: branchbyte1, branchbyte2\n→ address\njump to subroutine at branchoffset " +
                            "(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2) and place " +
                            "the return address on the stack"
            ),
            new CompletionItem(
                    "jsr_w†",
                    "c9\n1100 1001\n4: branchbyte1, branchbyte2, branchbyte3, branchbyte4\n→ address\njump to " +
                            "subroutine at branchoffset (signed int constructed from unsigned bytes branchbyte1 << 24" +
                            " | branchbyte2 << 16 | branchbyte3 << 8 | branchbyte4) and place the return address on " + "the stack"
            ), new CompletionItem("l2d", "8a\n1000 1010\n value → result\nconvert a long to a double"),
            new CompletionItem("l2f", "89\n1000 1001\n value → result\nconvert a long to a float"),
            new CompletionItem("l2i", "88\n1000 1000\n value → result\nconvert a long to a int"),
            new CompletionItem("ladd", "61\n0110 0001\n value1, value2 → result\nadd two longs"),
            new CompletionItem("laload", "2f\n0010 1111\n arrayref, index → value\nload a long from an array"),
            new CompletionItem("land", "7f\n0111 1111\n value1, value2 → result\nbitwise AND of two longs"),
            new CompletionItem("lastore", "50\n0101 0000\n arrayref, index, value →\nstore a long to an array"),
            new CompletionItem(
                    "lcmp",
                    "94\n1001 0100\n value1, value2 → result\npush 0 if the two longs are the same, 1 if value1 is " +
                            "greater than value2, -1 otherwise"
            ), new CompletionItem("lconst_0", "09\n0000 1001\n → 0L\npush 0L (the number zero with type long) onto " + "the stack"),
            new CompletionItem("lconst_1", "0a\n0000 1010\n → 1L\npush 1L (the number one with type long) onto the " + "stack"),
            new CompletionItem(
                    "ldc",
                    "12\n0001 0010\n1: index\n→ value\npush a constant #index from a constant pool (String, int, " +
                            "float, Class, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, or a " +
                            "dynamically-computed constant) onto the stack"
            ),
            new CompletionItem(
                    "ldc_w",
                    "13\n0001 0011\n2: indexbyte1, indexbyte2\n→ value\npush a constant #index from a constant pool " +
                            "(String, int, float, Class, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, " +
                            "or a dynamically-computed constant) onto the stack (wide index is constructed as " +
                            "indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "ldc2_w",
                    "14\n0001 0100\n2: indexbyte1, indexbyte2\n→ value\npush a constant #index from a constant pool " +
                            "(double, long, or a dynamically-computed constant) onto the stack (wide index is " +
                            "constructed as indexbyte1 << 8 | indexbyte2)"
            ), new CompletionItem("ldiv", "6d\n0110 1101\n value1, value2 → result\ndivide two longs"),
            new CompletionItem("lload", "16\n0001 0110\n1: index\n→ value\nload a long value from a local variable " + "#index"),
            new CompletionItem("lload_0", "1e\n0001 1110\n → value\nload a long value from a local variable 0"),
            new CompletionItem("lload_1", "1f\n0001 1111\n → value\nload a long value from a local variable 1"),
            new CompletionItem("lload_2", "20\n0010 0000\n → value\nload a long value from a local variable 2"),
            new CompletionItem("lload_3", "21\n0010 0001\n → value\nload a long value from a local variable 3"),
            new CompletionItem("lmul", "69\n0110 1001\n value1, value2 → result\nmultiply two longs"),
            new CompletionItem("lneg", "75\n0111 0101\n value → result\nnegate a long"),
            new CompletionItem(
                    "lookupswitch",
                    "ab\n1010 1011\n8+: <0–3 bytes padding>, defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, " +
                            "npairs1, npairs2, npairs3, npairs4, match-offset pairs...\nkey →\na target address is " +
                            "looked up from a table using a key and execution continues from the instruction at that " + "address"
            ), new CompletionItem("lor", "81\n1000 0001\n value1, value2 → result\nbitwise OR of two longs"),
            new CompletionItem("lrem", "71\n0111 0001\n value1, value2 → result\nremainder of division of two longs"),
            new CompletionItem("lreturn", "ad\n1010 1101\n value → [empty]\nreturn a long value"),
            new CompletionItem(
                    "lshl", "79\n0111 1001\n value1, value2 → result\nbitwise shift left of a long value1 by int " + "value2 positions"
            ),
            new CompletionItem(
                    "lshr", "7b\n0111 1011\n value1, value2 → result\nbitwise shift right of a long value1 by int " + "value2 positions"
            ), new CompletionItem("lstore", "37\n0011 0111\n1: index\nvalue →\nstore a long value in a local variable" + " #index"),
            new CompletionItem("lstore_0", "3f\n0011 1111\n value →\nstore a long value in a local variable 0"),
            new CompletionItem("lstore_1", "40\n0100 0000\n value →\nstore a long value in a local variable 1"),
            new CompletionItem("lstore_2", "41\n0100 0001\n value →\nstore a long value in a local variable 2"),
            new CompletionItem("lstore_3", "42\n0100 0010\n value →\nstore a long value in a local variable 3"),
            new CompletionItem("lsub", "65\n0110 0101\n value1, value2 → result\nsubtract two longs"),
            new CompletionItem(
                    "lushr",
                    "7d\n0111 1101\n value1, value2 → result\nbitwise shift right of a long value1 by int value2 " + "positions, unsigned"
            ), new CompletionItem("lxor", "83\n1000 0011\n value1, value2 → result\nbitwise XOR of two longs"),
            new CompletionItem(
                    "monitorenter",
                    "c2\n1100 0010\n objectref →\nenter monitor for object (\"grab the lock\" – start of synchronized" + "() section)"
            ),
            new CompletionItem(
                    "monitorexit",
                    "c3\n1100 0011\n objectref →\nexit monitor for object (\"release the lock\" – end of synchronized" + "() section)"
            ),
            new CompletionItem(
                    "multianewarray",
                    "c5\n1100 0101\n3: indexbyte1, indexbyte2, dimensions\ncount1, [count2,...] → arrayref\ncreate a " +
                            "new array of dimensions dimensions of type identified by class reference in constant " +
                            "pool index (indexbyte1 << 8 | indexbyte2); the sizes of each dimension is identified by " +
                            "count1, [count2, etc.]"
            ),
            new CompletionItem(
                    "new",
                    "bb\n1011 1011\n2: indexbyte1, indexbyte2\n→ objectref\ncreate new object of type identified by " +
                            "class reference in constant pool index (indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "newarray",
                    "bc\n1011 1100\n1: atype\ncount → arrayref\ncreate new array with count elements of primitive " +
                            "type identified by atype"
            ), new CompletionItem("nop", "00\n0000 0000\n [No change]\nperform no operation"),
            new CompletionItem("pop", "57\n0101 0111\n value →\ndiscard the top value on the stack"),
            new CompletionItem(
                    "pop2",
                    "58\n0101 1000\n {value2, value1} →\ndiscard the top two values on the stack (or one value, if it" +
                            " is a double or long)"
            ),
            new CompletionItem(
                    "putfield",
                    "b5\n1011 0101\n2: indexbyte1, indexbyte2\nobjectref, value →\nset field to value in an object " +
                            "objectref, where the field is identified by a field reference index in constant pool " +
                            "(indexbyte1 << 8 | indexbyte2)"
            ),
            new CompletionItem(
                    "putstatic",
                    "b3\n1011 0011\n2: indexbyte1, indexbyte2\nvalue →\nset static field to value in a class, where " +
                            "the field is identified by a field reference index in constant pool (indexbyte1 << 8 | " + "indexbyte2)"
            ),
            new CompletionItem(
                    "ret†",
                    "a9\n1010 1001\n1: index\n[No change]\ncontinue execution from address taken from a local " +
                            "variable #index (the asymmetry with jsr is intentional)"
            ), new CompletionItem("return", "b1\n1011 0001\n → [empty]\nreturn void from method"),
            new CompletionItem("saload", "35\n0011 0101\n arrayref, index → value\nload short from array"),
            new CompletionItem("sastore", "56\n0101 0110\n arrayref, index, value →\nstore short to array"),
            new CompletionItem("sipush", "11\n0001 0001\n2: byte1, byte2\n→ value\npush a short onto the stack as an " + "integer value"),
            new CompletionItem(
                    "swap",
                    "5f\n0101 1111\n value2, value1 → value1, value2\nswaps two top words on the stack (note that " +
                            "value1 and value2 must not be double or long)"
            ),
            new CompletionItem(
                    "tableswitch",
                    "aa\n1010 1010\n16+: [0–3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4," +
                            " lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, " +
                            "jump offsets...\nindex →\ncontinue execution from an address in the table at offset index"
            ),
            new CompletionItem(
                    "wide",
                    "c4\n1100 0100\n3/5: opcode, indexbyte1, indexbyte2 or iinc, indexbyte1, indexbyte2, countbyte1, " +
                            "countbyte2\n[same as for corresponding instructions]\nexecute opcode, where opcode is " +
                            "either iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore, or " +
                            "ret, but assume the index is 16 bit; or execute iinc, where the index is 16 bits and the" +
                            " constant to increment by is a signed 16 bit short"
            )};

    @Override
    public CompletionItem[] getItemsArray() {
        CompletionItem[] r = Arrays.copyOf(BYTECODE_KEYWORDS, BYTECODE_KEYWORDS.length);
        Arrays.sort(r);
        return r;
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = Arrays.asList(BYTECODE_KEYWORDS);
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        return CompletionItem.CompletionItemSet.delimiterStrictSet3();
    }

    @Override
    public String toString() {
        return "Bytecode assembler - good to concat with java";
    }
}
