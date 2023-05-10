package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JavaKeywordsWithHelp implements CompletionItem.CompletionItemSet {

    static final CompletionItem[] EXT_JAVA_KEYWORDS = {
            new CompletionItem(
                    "sout", "abbreviation for std out", "System.out.println(\"String\");"
            ),
            new CompletionItem(
                    "serr", "abbreviation for std err", "System.err.println(\"String\");"
            ),
            new CompletionItem(
                    "ex.printStackTrace();",
                    "abbreviation for printing stack trace",
                    "new RuntimeException(\"my ex\").printStackTrace();"
            ),
    };

    private static final CompletionItem[] BASE_JAVA_KEYWORDS = {

            new CompletionItem(
                    "_", "Added in Java 9, the underscore has become a keyword and cannot be used as a variable name " + "anymore.[3]"
            ),
            new CompletionItem(
                    "abstract",
                    "A method with no definition must be declared as abstract and the class " + "containing" +
                            " it must be declared as abstract. Abstract classes cannot be instantiated. Abstract methods must" +
                            " be implemented in the" +
                            " sub classes. The abstract keyword cannot be used with variables or constructors. Note that an " +
                            "abstract class isn't" + " required to have an abstract method at all."
            ),
            new CompletionItem(
                    "assert",
                    "Assert describes a predicate (a true–false statement) placed in a Java " + "program to" +
                            " indicate that the developer thinks that the predicate is always true at that place. If an " +
                            "assertion evaluates to false at" +
                            " run-time, an assertion failure results, which typically causes execution to abort. Assertions " +
                            "are disabled at runtime by" +
                            " default, but can be enabled through a command-line option or programmatically through a method " +
                            "on the class loader."
            ),
            new CompletionItem(
                    "boolean",
                    "Defines a boolean variable for the values \"true\" or \"false\" only. By " + "default, the" +
                            " value of boolean primitive type is false. This keyword is also used to declare that a method " +
                            "returns a value of the" + " primitive type boolean."
            ), new CompletionItem("break", "Used to end the execution in the current loop body. Used to break out of a " + "switch block."),
            new CompletionItem(
                    "byte",
                    "The byte keyword is used to declare a field that can hold an 8-bit signed " + "two's complement" +
                            " integer.[5][6] This keyword is also used to declare that a method returns a value of the " +
                            "primitive type byte.[7][8]"
            ),
            new CompletionItem(
                    "case",
                    "A statement in the switch block can be labeled with one or more case or " + "default labels." +
                            " The switch statement evaluates its expression, then executes all statements that follow the " +
                            "matching case label;" + " see switch.[9][10]"
            ),
            new CompletionItem(
                    "catch",
                    "Used in conjunction with a try block and an optional finally block. The " + "statements" +
                            " in the catch block specify what to do if a specific type of exception is thrown by the try " + "block."
            ),
            new CompletionItem(
                    "char", "Defines a character variable capable of holding any character of the java" + " source file's character set."
            ),
            new CompletionItem(
                    "class",
                    "A type that defines the implementation of a particular kind of object. A " + "class" +
                            " definition defines instance and class fields, methods, and inner classes as well as specifying " +
                            "the interfaces the" +
                            " class implements and the immediate superclass of the class. If the superclass is not explicitly" +
                            " specified, the" +
                            " superclass is implicitly Object. The class keyword can also be used in the form Class.class to " +
                            "get a Class object" +
                            " without needing an instance of that class. For example, String.class can be used instead of" +
                            " doing new String().getClass()."
            ), new CompletionItem("const", "Unused but reserved."),
            new CompletionItem(
                    "continue",
                    "Used to resume program execution at the end of the current loop body." +
                            " If followed by a label, continue resumes execution at the end of the enclosing labeled loop " + "body."
            ),
            new CompletionItem(
                    "default",
                    "The default keyword can optionally be used in a switch statement to label" +
                            " a block of statements to be executed if no case matches the specified value; see switch.[9][10]" +
                            " Alternatively," +
                            " the default keyword can also be used to declare default values in a Java annotation. From Java " +
                            "8 onwards," +
                            " the default keyword can be used to allow an interface to provide an implementation of a method."
            ),
            new CompletionItem(
                    "do",
                    "The do keyword is used in conjunction with while to create a do-while loop, " + "which" +
                            " executes a block of statements associated with the loop and then tests a boolean expression " +
                            "associated with the" +
                            " while. If the expression evaluates to true, the block is executed again; this continues until " +
                            "the expression" + " evaluates to false.[11][12]"
            ),
            new CompletionItem(
                    "double",
                    "The double keyword is used to declare a variable that can hold a 64-bit" +
                            " double precision IEEE 754 floating-point number.[5][6] This keyword is also used to declare " +
                            "that a method" + " returns a value of the primitive type double.[7][8]"
            ),
            new CompletionItem(
                    "else",
                    "The else keyword is used in conjunction with if to create an if-else " + "statement," +
                            " which tests a boolean expression; if the expression evaluates to true, the block of statements " +
                            "associated with" + " the if are evaluated; if it evaluates to false, the block of statements associated" +
                            " with the else are evaluated.[13][14]"
            ),
            new CompletionItem("enum", "A Java keyword used to declare an enumerated type." + " Enumerations extend the base class Enum."),
            new CompletionItem(
                    "extends",
                    "Used in a class declaration to specify the superclass; used in" +
                            " an interface declaration to specify one or more superinterfaces. Class X extends class Y to add" +
                            " functionality," + " either by adding fields or methods to class Y, or by overriding methods of class Y. An " +
                            "interface Z extends" +
                            " one or more interfaces by adding methods. Class X is said to be a subclass of class Y; " +
                            "Interface Z is said" + " to be a subinterface of the interfaces it extends." +
                            " Also used to specify an upper bound on a type parameter in Generics."
            ),
            new CompletionItem(
                    "final",
                    "Define an entity once that cannot be changed nor derived from later." +
                            " More specifically: a final class cannot be subclassed, a final method cannot be overridden, and" +
                            " a final" + " variable can occur at most once as a left-hand expression on an executed command." +
                            " All methods in a final class are implicitly final."
            ),
            new CompletionItem(
                    "finally",
                    "Used to define a block of statements for a block defined" +
                            " previously by the try keyword. The finally block is executed after execution exits the try" +
                            " block and any associated catch clauses regardless of whether an exception was thrown or" +
                            " caught, or execution left method in the middle of the try or catch blocks using the return " + "keyword."
            ),
            new CompletionItem(
                    "float",
                    "The float keyword is used to declare a variable that can" +
                            " hold a 32-bit single precision IEEE 754 floating-point number.[5][6] This keyword is also" +
                            " used to declare that a method returns a value of the primitive type float.[7][8]"
            ),
            new CompletionItem(
                    "for",
                    "The for keyword is used to create a for loop, which" +
                            " specifies a variable initialization, a boolean expression, and an incrementation." +
                            " The variable initialization is performed first, and then the boolean expression is evaluated." +
                            " If the expression evaluates to true, the block of statements associated with the loop are " + "executed," +
                            " and then the incrementation is performed. The boolean expression is then evaluated again;" +
                            " this continues until the expression evaluates to false.[15], As of J2SE 5.0, the for keyword" +
                            " can also be used to create a so-called \"enhanced for loop\",[16] which specifies an array or" +
                            " Iterable object; each iteration of the loop executes the associated block of" +
                            " statements using a different element in the array or Iterable.[15]"
            ), new CompletionItem("goto", "Unused but reserved."),
            new CompletionItem(
                    "if",
                    "The if keyword is used to create an if statement, which tests a boolean " +
                            "expression; if the expression evaluates to true, the block of statements associated with the if " +
                            "statement is executed. This keyword can also be used to create an if-else statement; see else" + ".[13][14]"
            ),
            new CompletionItem(
                    "implements",
                    "Included in a class declaration to specify one or more interfaces that " +
                            "are implemented by the current class. A class inherits the types and abstract methods declared " +
                            "by the interfaces."
            ),
            new CompletionItem(
                    "import",
                    "Used at the beginning of a source file to specify classes or entire Java " +
                            "packages to be referred to later without including their package names in the reference. Since " +
                            "J2SE 5.0, import statements can import static members of a class."
            ),
            new CompletionItem(
                    "instanceof",
                    "A binary operator that takes an object reference as its first operand " +
                            "and a class or interface as its second operand and produces a boolean result. The instanceof " +
                            "operator evaluates to true if and only if the runtime type of the object is assignment " +
                            "compatible with the class or interface."
            ),
            new CompletionItem(
                    "int",
                    "The int keyword is used to declare a variable that can hold a 32-bit signed " +
                            "two's complement integer.[5][6] This keyword is also used to declare that a method returns a " +
                            "value of the primitive type int.[7][8]"
            ),
            new CompletionItem(
                    "interface",
                    "Used to declare a special type of class that only contains abstract or " +
                            "default methods, constant (static final) fields and static interfaces. It can later be " +
                            "implemented by classes that declare the interface with the implements keyword. As multiple " +
                            "inheritance is not allowed in Java, interfaces are used to circumvent it. An interface can be " +
                            "defined within another interface."
            ),
            new CompletionItem(
                    "long",
                    "The long keyword is used to declare a variable that can hold a 64-bit signed " +
                            "two's complement integer.[5][6] This keyword is also used to declare that a method returns a " +
                            "value of the primitive type long.[7][8]"
            ),
            new CompletionItem(
                    "native",
                    "Used in method declarations to specify that the method is not implemented " +
                            "in the same Java source file, but rather in another language.[8]"
            ),
            new CompletionItem(
                    "new",
                    "Used to create an instance of a class or array object. Using keyword for this " +
                            "end is not completely necessary (as exemplified by Scala), though it serves two purposes: it " +
                            "enables the existence of different namespace for methods and class names, it defines statically " +
                            "and locally that a fresh object is indeed created, and of what runtime type it is (arguably " +
                            "introducing dependency into the code)."
            ),
            new CompletionItem(
                    "package",
                    "Java package is a group of similar classes and interfaces. Packages are " + "declared with the package keyword."
            ),
            new CompletionItem(
                    "private",
                    "The private keyword is used in the declaration of a method, field, or " +
                            "inner class; private members can only be accessed by other members of their own class.[17]"
            ),
            new CompletionItem(
                    "protected",
                    "The protected keyword is used in the declaration of a method, field, or " +
                            "inner class; protected members can only be accessed by members of their own class, that class's " +
                            "subclasses or classes from the same package.[17]"
            ),
            new CompletionItem(
                    "public",
                    "The public keyword is used in the declaration of a class, method, or field;" +
                            " public classes, methods, and fields can be accessed by the members of any class.[17]"
            ),
            new CompletionItem(
                    "return",
                    "Used to finish the execution of a method. It can be followed by a value " +
                            "required by the method definition that is returned to the caller"
            ),
            new CompletionItem(
                    "short",
                    "The short keyword is used to declare a field that can hold a 16-bit signed " +
                            "two's complement integer.[5][6] This keyword is also used to declare that a method returns a " +
                            "value of the primitive type short.[7][8]"
            ),
            new CompletionItem(
                    "static",
                    "Used to declare a field, method, or inner class as a class field. Classes " +
                            "maintain one copy of class fields regardless of how many instances exist of that class. static " +
                            "also is used to define a method as a class method. Class methods are bound to the class instead " +
                            "of to a specific instance, and can only operate on class fields. Classes and interfaces declared" +
                            " as static members of another class or interface are actually top-level classes and are not " +
                            "inner classes."
            ),
            new CompletionItem(
                    "super",
                    "Inheritance basically used to achieve dynamic binding or run-time " +
                            "polymorphism in java. Used to access members of a class inherited by the class in which it " +
                            "appears. Allows a subclass to access overridden methods and hidden members of its superclass. " +
                            "The super keyword is also used to forward a call from a constructor to a constructor in the " +
                            "superclass. Also used to specify a lower bound on a type parameter in Generics."
            ),
            new CompletionItem(
                    "switch",
                    "The switch keyword is used in conjunction with case and default to create a" +
                            " switch statement, which evaluates a variable, matches its value to a specific case, and " +
                            "executes the block of statements associated with that case. If no case matches the value, the " +
                            "optional block labelled by default is executed, if included.[9][10]"
            ),
            new CompletionItem(
                    "synchronized",
                    "Used in the declaration of a method or code block to acquire the " +
                            "mutex lock for an object while the current thread executes the code.[8] For static methods, the " +
                            "object locked is the class's Class. Guarantees that at most one thread at a time operating on " +
                            "the same object executes that code. The mutex lock is automatically released when execution " +
                            "exits the synchronized code. Fields, classes and interfaces cannot be declared as synchronized."
            ),
            new CompletionItem(
                    "this",
                    "Used to represent an instance of the class in which it appears. this can be " +
                            "used to access class members and as a reference to the current instance. The this keyword is " +
                            "also used to forward a call from one constructor in a class to another constructor in the same " + "class."
            ),
            new CompletionItem(
                    "throw",
                    "Causes the declared exception instance to be thrown. This causes execution " +
                            "to continue with the first enclosing exception handler declared by the catch keyword to handle " +
                            "an assignment compatible exception type. If no such exception handler is found in the current " +
                            "method, then the method returns and the process is repeated in the calling method. If no " +
                            "exception handler is found in any method call on the stack, then the exception is passed to the " +
                            "thread's uncaught exception handler."
            ),
            new CompletionItem(
                    "throws",
                    "Used in method declarations to specify which exceptions are not handled " +
                            "within the method but rather passed to the next higher level of the program. All uncaught " +
                            "exceptions in a method that are not instances of RuntimeException must be declared using the " +
                            "throws keyword."
            ),
            new CompletionItem(
                    "transient",
                    "Declares that an instance field is not part of the default serialized " +
                            "form of an object. When an object is serialized, only the values of its non-transient instance " +
                            "fields are included in the default serial representation. When an object is deserialized, " +
                            "transient fields are initialized only to their default value. If the default form is not used, e" +
                            ".g. when a serialPersistentFields table is declared in the class hierarchy, all transient " +
                            "keywords are ignored.[18][19]"
            ),
            new CompletionItem(
                    "try",
                    "Defines a block of statements that have exception handling. If an exception is" +
                            " thrown inside the try block, an optional catch block can handle declared exception types. Also," +
                            " an optional finally block can be declared that will be executed when execution exits the try " +
                            "block and catch clauses, regardless of whether an exception is thrown or not. A try block must " +
                            "have at least one catch clause or a finally block."
            ), new CompletionItem("void", "The void keyword is used to declare that a method does not return any value" + ".[7]"),
            new CompletionItem(
                    "volatile",
                    "Used in field declarations to guarantee visibility of changes to " +
                            "variables across threads. Every read of a volatile variable will be read from main memory, and " +
                            "not from the CPU cache, and that every write to a volatile variable will be written to main " +
                            "memory, and not just to the CPU cache.[20] Methods, classes and interfaces thus cannot be " +
                            "declared volatile, nor can local variables or parameters."
            ),
            new CompletionItem(
                    "while",
                    "The while keyword is used to create a while loop, which tests a boolean " +
                            "expression and executes the block of statements associated with the loop if the expression " +
                            "evaluates to true; this continues until the expression evaluates to false. This keyword can also" +
                            " be used to create a do-while loop; see do.[11][12]"
            ), new CompletionItem("exports", ""), new CompletionItem("module", ""),
            new CompletionItem(
                    "non-sealed",
                    "Used to declare that a class or interface which extends a sealed class " + "can be extended by unknown classes.[21]"
            ), new CompletionItem("open", ""), new CompletionItem("opens", ""),
            new CompletionItem("permits", "The permits clause specifies the classes that are permitted to extend a " + "sealed class.[21]"),
            new CompletionItem("provides", ""), new CompletionItem("record", ""), new CompletionItem("requires", ""),
            new CompletionItem(
                    "sealed",
                    "A sealed class or interface can only be extended or implemented by classes " + "and interfaces permitted to do so.[21]"
            ), new CompletionItem("to", ""), new CompletionItem("transitive", ""), new CompletionItem("uses", ""),
            new CompletionItem("var", "A special identifier that cannot be used as a type name (since Java 10).[22]"),
            new CompletionItem("with", ""),
            new CompletionItem(
                    "yield",
                    "Used to set a value for a switch expression, when using labelled statement " + "groups (for example, case L:).[23]"
            ), new CompletionItem("true", "A boolean literal value."), new CompletionItem("false", "A boolean literal value."),
            new CompletionItem("null", "A reference literal value."),
            new CompletionItem(
                    "const",
                    "Although reserved as a keyword in Java, const is not used and has no " +
                            "function.[2][24] For defining constants in Java, see the final keyword."
            ), new CompletionItem("goto", "Although reserved as a keyword in Java, goto is not used and has no function" + ".[2][24]"),
            new CompletionItem(
                    "strictfp",
                    "Although reserved as a keyword in Java, strictfp is obsolete, and no " +
                            "longer has any function.[25] Previously this keyword was used to restrict the precision and " +
                            "rounding of floating point calculations to ensure portability.[8]"
            )};

    static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private static final CompletionItem[] JAVA_KEYWORDS = concatWithArrayCopy(EXT_JAVA_KEYWORDS, BASE_JAVA_KEYWORDS);

    @Override
    public CompletionItem[] getItemsArray() {
        CompletionItem[] r = Arrays.copyOf(JAVA_KEYWORDS, JAVA_KEYWORDS.length);
        Arrays.sort(r);
        return r;
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = Arrays.asList(JAVA_KEYWORDS);
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        return Pattern.compile("[a-zA-Z_\\-]");
    }

    @Override
    public String toString() {
        return "java keywords";
    }
}
