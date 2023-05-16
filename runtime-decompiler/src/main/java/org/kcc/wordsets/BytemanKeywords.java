package org.kcc.wordsets;

import org.kcc.CompletionItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BytemanKeywords implements CompletionItem.CompletionItemSet {
    public static final String ENTRY_DESC = "Following AT, An AT ENTRY specifier normally locates the trigger point " +
            "before the first executable instruction in the trigger method. An exception to this occurs in " +
            "the case of a constructor method in which case the trigger point is located before the first " +
            "instruction following the call to the super constructor or redirection call to an alternative " +
            "constructor. This is necessary to ensure that rules do not attempt to bind and operate on the " +
            "instance before it is constructed.";
    public static final String READ_DESC = "[type .] field [count | ALL ]  or  $var-or-idx [count | ALL ] ; follwoing" + " AT," +
            " an AT READ specifier followed by a field name locates the trigger point before the first " +
            "mention of an object field whose name matches the supplied field name i.e. it corresponds to the" +
            " first occurred of a corresponding getField instruction in the bytecode. If a type is specified " +
            "then the getField instruction will only be matched if the named field is declared by a class " +
            "whose name matches the supplied type. If a count N is supplied then the Nth matching getField " +
            "will be used as the trigger point. Note that the count identifies to the Nth textual occurence " +
            "of the field access, not the Nth field access in a particular execution path at runtime. If the " +
            "keyword ALL is specified in place of a count then the rule will be triggered at all matching " + "getField calls. \n" + "\n" +
            "An AT READ specifier followed by a $-prefixed local variable name, method parameter name or " +
            "method parameter index locates the trigger point before the first instruction which reads the " +
            "corresponding local or method parameter variable i.e. it corresponds to an iload, dload, aload " +
            "etc instruction in the bytecode. If a count N is supplied then the Nth matching read will be " +
            "used as the trigger point. Note that the count identifies to the Nth textual occurrence of a " +
            "read of the variable, not the Nth access in a particular execution path at runtime. If the " +
            "keyword ALL is specified in place of a count then the rule will be triggered before every read " + "of the variable.\n" +
            "\n" + "Note that it is only possible to use local or parameter variable names such as $i, $this or " +
            "$arg1 if the trigger method bytecode includes a local variable table, e.g. if it has been " +
            "compiled with the -g flag. By contrast, it is always possible to refer to parameter variable " +
            "read operations using the index notation $0, $1 etc (however, note that location AT READ $0 will" +
            " only match where the trigger method is an instance method).\n";
    public static final String AFTERREAD_DESC = " [ type .] field [count | ALL ] or  $var-or-idx [count | ALL ] ;  " +
            "follwoing AT, An AFTER READ specification is identical to an AT READ specification except that " +
            "it locates the trigger point after the getField or variable read operation.";
    public static final String WRITE_DESC = "following AT, [ type .] field [count | ALL ] or $var-or-idx [count " + "| ALL ] ; \n" + "\n" +
            "AT WRITE and AFTER WRITE specifiers are the same as the corresponding READ specifiers " +
            "except that they correspond to assignments to the named field or named variable in the " +
            "source code i.e. they identify putField or istore, dstore, etc instructions.\n" + "\n" +
            "Note that location AT WRITE $0 or, equivalently, AT WRITE $this will never match any " +
            "candidate trigger method because the target object for an instance method invocation is " + "never assigned.\n" + "\n" +
            "Note also that for a given local variable, localvar, location AT WRITE $localvar or, " +
            "equivalently, AT WRITE $localvar 1 identifies the location immediately after the local " +
            "variable is initialised i.e. it is treated as if it were specified as AFTER WRITE " +
            "$localvar. This is necessary because the variable is not in scope until after it is " +
            "initialised. This also ensures that the local variable which has been written can be " + "safely accessed in the rule body.\n";
    public static final String AFTERWRITE_DESC = "following AT, [ type .] field [count | ALL ] or $var-or-idx " + "[count | ALL ] ; \n" +
            "\n" + "\n" + "AT WRITE and AFTER WRITE specifiers are the same as the corresponding READ specifiers " +
            "except that they correspond to assignments to the named field or named variable in the " +
            "source code i.e. they identify putField or istore, dstore, etc instructions.\n" + "\n" +
            "Note that location AT WRITE $0 or, equivalently, AT WRITE $this will never match any " +
            "candidate trigger method because the target object for an instance method invocation is " + "never assigned.\n" + "\n" +
            "Note also that for a given local variable, localvar, location AT WRITE $localvar or, " +
            "equivalently, AT WRITE $localvar 1 identifies the location immediately after the local " +
            "variable is initialised i.e. it is treated as if it were specified as AFTER WRITE " +
            "$localvar. This is necessary because the variable is not in scope until after it is " +
            "initialised. This also ensures that the local variable which has been written can be " + "safely accessed in the rule body.\n";
    public static final String INVOKE_DESC = "following AT [ type .] method [ ( argtypes ) ] [count | ALL ]\n" +
            "AT INVOKE and AFTER INVOKE specifiers are like READ and WRITE specifiers except that they " +
            "identify invocations of methods or constructors within the trigger method as the trigger point. " +
            "The method may be identified using a bare method name or the name may be qualified by a, " +
            "possibly package-qualified, type or by a descriptor. A descriptor consists of a comma-separated " +
            "list of type names within brackets. The type names identify the types of the method parameters " +
            "and may be prefixed with package qualifiers and employ array bracket pairs as suffixes.";

    private static final String NEW_DESC = "[ type ] [ [] ] * [count | ALL ]\n" + "\n" + "\n" +
            "AT NEW and AFTER NEW specifiers identify locations in the target method where a new operation " +
            "creates a Java object class or array class. An AT NEW rule is triggered before the object or " +
            "array is allocated. An AFTER NEW rule is triggered after creation and initialization of the " + "object or array.\n" + "\n" +
            "Selection of the NEW trigger location may be constrained by supplying a variety of optional " +
            "arguments, a type name, one or more pairs of square braces and either an integer count or the " +
            "keyword ALL. These arguments may all be specified independently and they each serve to select a " +
            "more or less precise set of matches for points where the rule may be considered for injection " + "into the target method.\n" +
            "\n" + "If a type name is supplied injection is limited to points where an instance (or array) of the " +
            "named type is created. The type name can be supplied without a package qualifier, in which case " +
            "any new operation with a type sharing the same non-package qualified name will match.\n" + "\n" +
            "If the type name is omitted then injection can occur at any point where an instance (or array) " + "is created.\n" + "\n" +
            "Note that extends and implements relationships are ignored when matching. For example, if a rule" +
            " specifies AT NEW Foo then the location will not be matched against operation new Foobar even if" +
            " FooBar extends Foo. Similarly, when Foo implements IFoo specifying location AT NEW IFoo will " +
            "not be matched. Indeed specifying any interface is a mistake. new operations always instantiate " +
            "a specific class and never an interface. So, locations specifying an interface name will never " + "match.\n" + "\n" +
            "If one or more brace pairs are included then injection is limited to points in the method where " +
            "an array with the equivalent number of dimensions is created. So, for example specifying AT NEW " +
            "[][] will match any new operation where a 2d array is created, irrespective of what the array " +
            "base type is, By contrast, specifying AT NEW int[] will only match a new operation where a 1d " +
            "int array is created. If no braces are supplied then matches will be restricted to new " +
            "operations where a Java object class (i.e. a non-array class) is instantiated.\n" + "\n" +
            "When there are multiple canidate injection points in a method an integer count may be supplied " +
            "to pick a specific injection point (count defaults to 1 if it is left unspecified). Keyword ALL " +
            "can be supplied to request injection at all matching injection points.\n";

    public static final String SYNC_DESC =
            "[ count | ALL ]\n" + "AT SYNCHRONIZE and AT AFTER SYNCHRONIZE specifiers identify synchronization blocks in the target" +
                    " method, i.e. they correspond to MONITORENTER instructions in the bytecode. Note that AFTER " +
                    "SYNCHRONIZE identifies the point immediately after entry to the synchronized block rather than " +
                    "the point immediately after exit from the block.";
    public static final String THORW_DESC = " [count | ALL ] \n An AT THROW specifier identifies a throw operation " + "within" +
            " the trigger method as the trigger point. The throw operation may be qualified by a, possibly " +
            "package-qualified, typename identifying the lexical type of the thrown exception. If a count N " +
            "is supplied then the location specifies the Nth textual occurrence of a throw. If the keyword " +
            "ALL is specified in place of a count then the rule will be triggered at all matching occurrences" + " of a throw.";
    public static final String EXC_DESC = "An AT EXCEPTION EXIT specifier identifies the point where a method returns" +
            " control back to its " + "caller via unhandled exceptional control flow. This can happen either because the method itself " +
            "has thrown an exception or because it has called out to some other method which has thrown an " +
            "exception. It can also happen when the method executes certain operations in the Java language, " +
            "for example dereferencing a null object value or indexing beyond the end of an array.\n" + "\n" +
            "A rule injected with this location is triggered at the point where the exception would normally " +
            "propagate back to the caller. Once rule execution completes then normally the exception flow " +
            "resumes. However, the rule may subvert this resumed flow by executing a RETURN. It may also " +
            "explicitly rethrow the original exception or throw some newly created exception by executing a " +
            "THROW (n.b. if the latter is a checked exception then it must be declared as a possible " +
            "exception by the trigger method).\n" + "\n" +
            "n.b. when several rules specify the same location the order of injection of trigger calls " +
            "usually follows the order of the rules in their respective scripts. The exception to this is " +
            "AFTER locations where the the order of injection is the reverse to the order of occurrence.\n" + "\n" +
            "n.b.b. when a location specifier (other than ENTRY or EXIT) is used with an overriding rule the " +
            "rule code is only injected into the original method or overriding methods if the location " +
            "matches the method in question. So, for example, if location AT READ myField 2 is employed then " +
            "the rule will only be injected into implementations of the method which include two loads of " +
            "field myField. Methods which do not match the location are ignored.\n" + "\n" +
            "n.b.b.b. for historical reasons CALL may be used as a synonym for INVOKE, RETURN may be used as " +
            "a synonym for EXIT and the AT in an AT LINE specifier is optional.\n";
    public static final String TRACE_OPEN_DESC = "(Object identifier, String filename) or (Object identifier)" + "\ntraceOpen " +
            "opens the file identified by fileName and associates it with identifier, returning true. " +
            "filename can be either a relative or absolute path. Relative file names are located relative to " +
            "the current working directory of the JVM. If there is already a file associated with identifier " +
            "then traceOpen immediately returns false. If a file with the given name already exists it is " +
            "opened in append mode. If filename is omitted then a unique name is generated for the file which" +
            " is guaranteed not to match any existing trace file in the current working directory.";
    public static final String TRACE_CLOSE_DESC = "(Object identifier, String filename)\ntraceClose closes the file " +
            "associated with identifier and removes the association, returning true. If no open file is " +
            "associated with identifier it returns false.";

    public static final String TRACE_DESC = "(Object identifier, String filename) or (Object identifier)\ntrace " + "prints " +
            "message to file associated with identifier, returning true. If no open file is associated with " +
            "identifier then a file will be opened and associated with identifier as if a call to trace had " +
            "been made with no file name supplied. If identifier is omitted then the output is written to " + "System.out.\n" +
            "A caveat applies to the all trace* descriptions for three special cases. If identifier is null " + "or the" +
            " string \"out\", then trace and traceln write to System.out. If identifier is the string " +
            "\"err\", then trace and traceln write to System.err. traceOpen and traceClose always return " +
            "false immediately if identifier has any of these values. Calls to trace(message) and traceln" +
            "(message) which omit identifier are implemented by calling, respectively, trace(\"out\", " +
            "message) and traceln(\"out\", message).";
    public static final String TRACELN_DESC = "(Object identifier, String filename) or (Object identifier)\ntraceln " +
            "prints message to file associated with identifier and appends a newline to the file, returning " +
            "true. If no open file is associated with identifier then a file will be opened and associated " +
            "with identifier as if a call to trace had been made ";
    private static final String FLAG_DESC = "Flags\n" + "\n" +
            "The rule engine provides a simple mechanism for setting, testing and clearing global flags. The API " +
            "defined by the helper class is\n" + "\n" + "  public boolean flag(Object identifier)\n" +
            "  public boolean flagged(Object identifier)\n" + "  public boolean clear(Object identifier)\n" + "\n" +
            "As before, Flags are identified by an arbitrary object. All three methods are designed to be used either" +
            " in conditions or actions.\n" + "\n" +
            "flag can be called to ensure that the Flag identified by identifier is set. It returns true if the Flag " +
            "was previously clear otherwise false. Note that the API is designed to ensure that race conditions " +
            "between multiple threads trying to set a Flag from rule conditions can only have one winner.\n" + "\n" +
            "flagged tests whether the Flag identified by identifier is set. It returns true if the Flag is set " + "otherwise false.\n" +
            "\n" + "clear can be called to ensure that the Flag identified by identifier is clear. It returns true if the " +
            "Flag was previously set otherwise false. Note that the API is designed to ensure that race conditions " +
            "between multiple threads trying to clear a Flag from rule conditions can only have one winner.\n";
    private static final String COUNTER_DESC = "Counters\n" + "\n" +
            "The rule engine provides Counters which maintain global counts across independent rule triggerings. They" +
            " can be created and initialised, read, incremented and decremented in order track and respond to the " +
            "number of times various triggerings or firings have happened. Note that unlike CountDowns there are no " +
            "special semantics associated with decrementing a Counter to zero. They may even have negative values. " +
            "The API defined by the helper class is\n" + "\n" + "  public boolean createCounter(Object o)\n" +
            "  public boolean createCounter(Object o, int count)\n" + "  public boolean deleteCounter(Object o)\n" +
            "  public int incrementCounter(Object o, int amount)\n" + "  public int incrementCounter(Object o)\n" +
            "  public int decrementCounter(Object o)\n" + "  public int readCounter(Object o)\n" +
            "  public int readCounter(Object o, boolean zero)\n" + "\n" +
            "As before, Counters are identified by an arbitrary object. All methods are designed to be used in rule " +
            "conditions or actions.\n" + "\n" +
            "createCounter can be called to create a new Counter associated with o. If argument count is not supplied" +
            " then the value of the new Counter defaults to o. createCounter returns true if a new Counter was " +
            "created and false if a Counter associated with o already exists. Note that the API is designed to ensure" +
            " that race conditions between multiple threads trying to create a Counter from rule conditions can only " +
            "have one winner.\n" + "\n" +
            "deleteCounter can be called to delete any existing Counter associated with o. It returns true if the " +
            "Counter was deleted and false if no Counter was associated with o. Note that the API is designed to " +
            "ensure that race conditions between multiple threads trying to delete a Counter from rule conditions can" +
            " only have one winner.\n" + "\n" +
            "incrementCounter can be called to increment the Counter associated with o. If no such Counter exists it " +
            "will create one with value 0 before incrementing it. incrementCounter returns the new value of the " +
            "Counter. If amount is omitted it defaults to 1.\n" + "\n" +
            "decrementCounter is equivalent to calling incrementCounter(o, -1) i.e. it adds -1 to the value of the " + "counter.\n" + "\n" +
            "readCounter can be called to read the value of the Counter associated with o. If no such Counter exists " +
            "it will create one with value 0. If the optional flag argument zero is passed as true the counter is " +
            "atomically read and zeroed. zero defaults to false.\n";
    private static final String TIMER_DESC =
            "Timers\n" + "\n" + "The rule engine provides Timers which allow measurement of elapsed time between triggerings. Timers can " +
                    "be created, read, reset and deleted via the following API\n" + "\n" + "  public boolean createTimer(Object o)\n" +
                    "  public long getElapsedTimeFromTimer(Object o)\n" + "  public long resetTimer(Object o)\n" +
                    "  public boolean deleteTimer(Object o)\n" + "\n" +
                    "As before, Timers are identified by an arbitrary object. All methods are designed to be used in rule " +
                    "conditions or actions.\n" + "\n" +
                    "createTimer can be called to create a new Timer associated with o. createTimer returns true if a new " +
                    "Timer was created and false if a Timer associated with o already exists.\n" + "\n" +
                    "getElapsedTimeFromTimer can be called to obtain the number of elapsed milliseconds since the Timer " +
                    "associated with o was created or since the last call to resetTimer. If no timer associated with o exists" +
                    " a new timer is created before returning the elapsed time.\n" + "\n" +
                    "resetTimer can be called to zero the Timer associated with o. It returns the number of seconds since the" +
                    " Timer was created or since the last previous call to resetTimer If no timer associated with o exists a " +
                    "new timer is created before returning the elapsed time.\n" + "\n" +
                    "deleteTimer can be called to delete the Timer associated with o. deleteTimer returns true if a new Timer" +
                    " was deleted and false if no Timer associated with o exists.\n";
    private static final String LOP_SHARED = "\nByteman provides the English language keywords listed below which can" + " " + "be " +
            "used in place of the related standard Java operators (in brackets):\n" +
            "OR (||), AND (&&), NOT (!), LE (< =), LT (<), EQ (==), NE (!=), GE (>=), GT (>), TIMES (*), DIVIDE (/), " +
            "PLUS (+), MINUS (-), MOD (%), Keywords are recognised in either upper or lower (but not mixed) case.\n" +
            "Keywords may clash with the same names where they they occur as legal Java identifiers in the target " +
            "classes and methods specified in Byteman rules";
    private static final String HELPER_DESC =
            "\n" + "\n" + "A rule can specify it’s own helper class if it wants to extend, override or replace the set of built-in " +
                    "calls available for use in its event, condition or action. For example, in the following rule, class " +
                    "FailureTester is used as the helper class. Its boolean instance method doWrongState(CoordinatorEngine) " +
                    "is called from the condition to decide whether or not to throw a WrongStateException.\n" + "\n" +
                    "  # helper example\n" + "  RULE help yourself\n" + "  CLASS com.arjuna.wst11.messaging.engines.CoordinatorEngine\n" +
                    "  METHOD commit\n" + "  HELPER com.arjuna.wst11.messaging.engines.FailureTester\n" + "  AT EXIT\n" +
                    "  IF doWrongState($0)\n" + "  DO throw new WrongStateException()\n" + "  ENDRULE\n" + "\n" +
                    "A helper class does not need to implement any special interface or inherit from any pre-defined class. " +
                    "It merely needs to provide instance methods to resolve the built-in calls which occur in the rule. The " +
                    "only limitations are\n" + "\n" + "    your helper class must not be final\n" + "\n" +
                    "        Byteman needs to be able to subclass your helper in order to interface it to the rule execution " +
                    "engine\n" + "\n" + "    your helper class must not be abstract\n" + "\n" +
                    "        Byteman needs to be able to instantiate your helper when the rule is triggered\n" + "\n" +
                    "    you must provide a suitable public constructor for your helper class\n" + "\n" +
                    "        by default Byteman will instantiate it using the empty constructor (i.e. the one with signature " + "())\n" +
                    "\n" + "        if you provide a constructor that accepts the rule as argument (i.e. with signature (org.jboss" +
                    ".byteman.agent.rule.Rule)) Byteman will use that for preference\n" + "\n" +
                    "By sub-classing the default helper it is possible to extend or override the default set of methods. For " +
                    "example, the following rule employs a helper which adds emphasis to the debug messages printed by the " + "rule.\n" +
                    "\n" + "  # helper example 2\n" + "  RULE help yourself but rely on others\n" +
                    "  CLASS com.arjuna.wst11.messaging.engines.CoordinatorEngine\n" + "  METHOD commit\n" + "  HELPER HelperSub\n" +
                    "  AT ENTRY\n" + "  IF NOT flagged($this)\n" + "  DO debug(\"throwing wrong state\");\n" + "     flag($this);\n" +
                    "     throw new WrongStateException()\n" + "  ENDRULE\n" + "\n" + "  class HelperSub extends Helper\n" + "  {\n" +
                    "      public HelperSub(Rule rule)\n" + "      {\n" + "        super(rule);\n" + "      }\n" +
                    "      public boolean debug(String message)\n" + "      {\n" +
                    "          super(\"!!! IMPORTANT EVENT !!! \" + message);\n" + "      }\n" + "  }\n" + "\n" +
                    "The rule is still able to employ the built-in methods flag and flagged defined by the default helper " + "class.\n" +
                    "\n" + "The examples above use a HELPER line in the rule body to reset the helper for a specific rule. It is " +
                    "also possible to reset the helper for all subsequent rules in a file by adding a HELPER line outside of " +
                    "the scope of a rule. So, in the following example the first two rules use class HelperSub while the " +
                    "third one uses class YellowSub.\n" + "\n" + "  HELPER HelperSub\n" + "  # helper example 3\n" +
                    "  RULE helping hand\n" + "  . . .\n" + "  RULE I can't help myself\n" + "  . . .\n" +
                    "  RULE help, I need somebody\n" + "  CLASS . . .\n" + "  METHOD . . .\n" + "  HELPER YellowSub\n" + "  . . .\n" + "\n";
    private static final String AS_TRIGGER =
            "\n" + "\n" + "By contrast, if the rule uses AS TRIGGER semantics then when the rule is matched against class " +
                    "LinkedList that dynamic type will be used during typecheck to type expression $0 and type check would " +
                    "succeed. The same applies when the rule is matched against any other class that implements a method with" +
                    " signature append(Object). The rule will still fail to typecheck when the trigger class is ArrayList " +
                    "leading to a type error and disabling of injection for that case.\n" + "\n" +
                    "There are benefits to using the dynamic type scope established at the point of injection as well as the " +
                    "potential for errors as seen above. One common case where using AS TRIGGER semantics is preferable " +
                    "arises when the dynamically determined trigger classes belong to a child classloader of the target class" +
                    ". If AS TRIGGER semantics are used then the body of the rule can reference other classes defined by the " +
                    "child class loader. If AS TARGET semantics were used instead then those types would not be in scope. In " +
                    "many cases this is simply resolved by injecting direct into each specific subclass but cases do arise " +
                    "where it is easier to employ a single rule which relies on injection through an interface and/or down a " +
                    "class hierarchy.\n" + "\n" +
                    "For hysterical reasons, Byteman uses AS TRIGGER (dynamic) type scoping as the default semantics. " +
                    "However, it also supports AS TARGET (lexical) type scoping. In fact, you can mix and match the two " +
                    "approaches for individual rules or rule groups by inserting an AS TRIGGER or AS TARGET clause into your " +
                    "scripts. If the clause appears in the body of a rule, between the location (AT) and condition (IF) " +
                    "clauses, then it defines the type scope for that specific rule. If it appears at the top level, outside " +
                    "of a rule body then it (re-)defines the default type scope to be used for subsequent rules which do not " +
                    "provide their own declaration.\n";
    private static final String AS_TARGET = "The AS TARGET clause tells the type checker to use the target type named" +
            " in the CLASS or INTERFACE clause when type checking the expression $0 or $this. It also tells the type " +
            "checker to resolve class names using the class loader of the target type.";
    private static final CompletionItem[] BYTEMAN_KEYWORDS = {
            new CompletionItem("RULE", "Star of the rule. Is " + "followed by mandatory name (spaces allowed)"),
            new CompletionItem("CLASS", "Follows the RULE, and is followed by FQN of class, it is supposed to touch"),
            new CompletionItem("METHOD", "usually follows CLASS, and is followed by method name, which is going to be" + " affected"),
            new CompletionItem(
                    "BIND",
                    "Allows you to bind your local byteman variables in form " + "name:OptionalType=$egJavaVar." +
                            " You can bind several expressions at once"
            ), new CompletionItem("IF", " followed by expression resulting as boolean, is making part of the rule " + "conditional."),
            new CompletionItem(
                    "DO",
                    "Followed by java code (mixed with byteman's buildins and keywords) is the " +
                            "actual code the rule do or should (if there is IF) do."
            ), new CompletionItem("ENDRULE", "mandatory end of the rule declaration"),
            new CompletionItem("AT", "Followinf METHOD, setting precise place in method where to invoke DO section"),
            new CompletionItem("ENTRY", ENTRY_DESC), new CompletionItem("AT ENTRY", ENTRY_DESC),
            new CompletionItem(
                    "EXIT",
                    "Following AT, setting place to return from the method. An AT EXIT specifier " +
                            "locates a trigger point at each location in the trigger method where a normal return of control " +
                            "occurs (i.e. wherever there is an implicit or explicit return but not where a throw exits the " + "method)."
            ),
            new CompletionItem(
                    "LINE",
                    "Following AT, expecting <number> as parameter, will set DO to exact line in " +
                            "original code. Note, that bytecode had to be compiled with -g to have line numbers accessible. " +
                            "An AT LINE specifier locates the trigger point before the first executable bytecode instruction " +
                            "in the trigger method whose source line number is greater than or equal to the line number " +
                            "supplied as argument to the specifier. If there is no executable code at (or following) the " +
                            "specified line number the agent will not insert a trigger point (note that it does not print an " +
                            "error in such cases because this may merely indicate that the rule does not apply to this " +
                            "particular class or method)."
            ), new CompletionItem("READ", READ_DESC), new CompletionItem("AT READ", READ_DESC),
            new CompletionItem("AFTER READ", AFTERREAD_DESC), new CompletionItem("AT AFTER READ", AFTERREAD_DESC),
            new CompletionItem("WRITE", WRITE_DESC), new CompletionItem("AFTER WRITE", AFTERWRITE_DESC),
            new CompletionItem("INVOKE", INVOKE_DESC), new CompletionItem("AT INVOKE", INVOKE_DESC),
            new CompletionItem("AFTER INVOKE", INVOKE_DESC), new CompletionItem("AT AFTER INVOKE", INVOKE_DESC),
            new CompletionItem("AFTER", "Is not accepted as standalone keyword, is only part as other ones"),
            new CompletionItem("AT AFTER", "Is not accepted as standalone keyword, is only part as other ones"),
            new CompletionItem("NEW", NEW_DESC), new CompletionItem("NEW", NEW_DESC), new CompletionItem("AFTER NEW", NEW_DESC),
            new CompletionItem("AT AFTER NEW", NEW_DESC), new CompletionItem("SYNCHRONIZE", SYNC_DESC),
            new CompletionItem("AT SYNCHRONIZE", SYNC_DESC), new CompletionItem("AFTER SYNCHRONIZE", SYNC_DESC),
            new CompletionItem("AT AFTER SYNCHRONIZE", SYNC_DESC), new CompletionItem("THROW", THORW_DESC),
            new CompletionItem("AT THROW", THORW_DESC), new CompletionItem("EXCEPTION EXIT", EXC_DESC),
            new CompletionItem("AT EXCEPTION EXIT", EXC_DESC), new CompletionItem("traceOpen", TRACE_OPEN_DESC),
            new CompletionItem("traceClose", TRACE_CLOSE_DESC), new CompletionItem("trace", TRACE_DESC),
            new CompletionItem("traceln", TRACELN_DESC),
            new CompletionItem(
                    "traceStack",
                    "(\"found the caller!\\n\", 10) - as expected print the stack trace. " +
                            "String is helper string and number is optional limitation"
            ),
            new CompletionItem(
                    "traceStackMatching",
                    "(\"regex\", 10) - \nSelective Stack Tracing Using a Regular " + "Expression Filter\n" + "\n" +
                            "It is useful to be able to selectively filter a stack trace, limiting it, say, to include only " +
                            "frames from a given package or set of packages. The rule engine provides an alternative set of " +
                            "built-in methods which can be used to obtain or print a string representation of some subset of " +
                            "the stack filtered using a regular expression match. The API defined by the helper class is\n" +
                            "String is helper string and number is optional limitation"
            ),
            new CompletionItem(
                    "traceStackBetween",
                    "(String from, String to) \n Another option for selective stack " + "tracing is to " +
                            "specify a matching expression to select the start and end frame for the trace. The rule engine " +
                            "provides another set of built-in methods which can be used to obtain or print a string " +
                            "representation of a segment of the stack in this manner. The API defined by the helper class is"
            ), new CompletionItem("traceStackBetween", "(String from, String to) \n Another option for selective stack "),
            new CompletionItem("formatStack"), new CompletionItem("formatStackRange(String from, String to) "),
            new CompletionItem("callerEquals(String name...) "), new CompletionItem("callerMatches(String regex...) "),
            new CompletionItem("traceThreadStack(String threadName...) "), new CompletionItem("formatThreadStack(String threadName...) "),
            new CompletionItem("traceAllStacks(String threadName...) "), new CompletionItem("flag", FLAG_DESC),
            new CompletionItem("flagged", FLAG_DESC), new CompletionItem("clear", FLAG_DESC),
            new CompletionItem("killJVM", "Will terminate jvm, as it have crashed"), new CompletionItem("createCounter", COUNTER_DESC),
            new CompletionItem("deleteCounter", COUNTER_DESC), new CompletionItem("incrementCounter", COUNTER_DESC),
            new CompletionItem("decrementCounter", COUNTER_DESC), new CompletionItem("readCounter", COUNTER_DESC),
            new CompletionItem("createTimer", TIMER_DESC), new CompletionItem("getElapsedTimeFromTimer", TIMER_DESC),
            new CompletionItem("resetTimer", TIMER_DESC), new CompletionItem("deleteTimer", TIMER_DESC),
            new CompletionItem(
                    "setTriggering",
                    "(boolean) - enables/disables recursive execution of rules. If " +
                            "enabled is false then triggering is disabled during execution of subsequent expressions in the " +
                            "rule body. If it is true then triggering is re-enabled."
            ), new CompletionItem("#", "is singe line comment for for byteman interpreter"),
            new CompletionItem(
                    "$",
                    "dollar sign allows you to access variabels as they were written in java " + "sources" +
                            ". Eg myVar will be accessed as $myVar. Parameters of method can be accessed bash-like - " + "$0, $1. " +
                            "Value assigned to return is $!" + ".."
            ),
            new CompletionItem(
                    "$!",
                    "\n" + "\n" + "$! is valid in at AT EXIT rule and is bound to the return value on the stack at the point where " +
                            "the rule is triggered. Its type is the same as the trigger method return type. The rule will " +
                            "fail to inject if the trigger method return type is void.\n" + "\n" +
                            "$! is also valid in an AFTER INVOKE rule and is bound to the return value on the stack at the " +
                            "point where the rule is triggered. Its type is the same as the invoked method return type. The " +
                            "rule will fail to inject if the invoked method return type is void.\n" + "\n" +
                            "$! is also valid in an AFTER NEW rule and is bound the instance or array created by the new " +
                            "operation which triggered the rule. Its type is that of the corresponding new expression in the " +
                            "trigger method.\n"
            ),
            new CompletionItem(
                    "$^",
                    "\n" + "\n" + "$^ is valid in an AT THROW rule and is bound to the throwable on the stack at the point where " +
                            "the rule is triggered. Its type is Throwable.\n" + "\n" +
                            "$^ is also valid in an AT EXCEPTION EXIT rule and is bound to the throwable being returned from " +
                            "the method via exceptional control flow. Its type is Throwable.\n"
            ), new CompletionItem("$#", "$# has type int and identifies the number of parameters supplied to the trigger" + " method."),
            new CompletionItem(
                    "$*",
                    "$* is bound to an Object[] array containing the trigger method recipient, " +
                            "$this, in slot 0 and the trigger method parameter values, $1, $2 etc in slots 1, 2 etc (for a " +
                            "static trigger method the value in slot 0 is null)."
            ),
            new CompletionItem(
                    "$@",
                    "$@ is only valid in an AT INVOKE rule and is bound to an Object[] array " +
                            "containing the AT INVOKE target method recipient in slot 0 and the call arguments for the target" +
                            " method installed in slots 1 upwards in call order (if the target method is static the value in " +
                            "slot 0 is null). Note that this variable is not valid in AFTER INVOKE rules. The array contains " +
                            "the call arguments located on the stack just before the trigger method calls the AT INVOKE " +
                            "target method. These values are no longer available after the call has completed."
            ),
            new CompletionItem(
                    "$CLASS",
                    "$CLASS is valid in all rules and is bound to a String whose value is the " +
                            "full package qualified name of the trigger class for the rule. The trigger class is the class " +
                            "whose method the rule has been injected into. Note that this is normally the same as the target " +
                            "class mentioned in the CLASS clause of the rule. However, when injecting into interfaces or " +
                            "using overriding injection the trigger class may be an implementation or subclass, respectively," +
                            " of the target class. So there may be more than one trigger class for any given target class."
            ),
            new CompletionItem(
                    "$METHOD",
                    "$METHOD is valid in all rules and is bound to a String whose value is the " +
                            "full name of the trigger method into which the rule has been injected, qualified with signature " +
                            "and return type. Note that this is normally the same as the target method mentioned in the " +
                            "METHOD clause of the rule. However, the target method may omit the signature and return type. So" +
                            " there may be more than one trigger method for any given target method."
            ),
            new CompletionItem(
                    "$NEWCLASS",
                    "$NEWCLASS is only valid in AT NEW and AFER NEW rules. It is bound to a " +
                            "String which is the canonical name of the object or array created by the new operation e.g org" +
                            ".my.Foo, int[], org.my.Bar[][]."
            ), new CompletionItem("OR", " (||)" + LOP_SHARED), new CompletionItem("AND", " (&&)" + LOP_SHARED),
            new CompletionItem("NOT", " (!)" + LOP_SHARED), new CompletionItem("LE", " (<=)" + LOP_SHARED),
            new CompletionItem("LT", " (<)" + LOP_SHARED), new CompletionItem("EQ", " (==)" + LOP_SHARED),
            new CompletionItem("NE", " (!=)" + LOP_SHARED), new CompletionItem("GE", " (>=)" + LOP_SHARED),
            new CompletionItem("GT", " (>)" + LOP_SHARED), new CompletionItem("TIMES", " (*)" + LOP_SHARED),
            new CompletionItem("DIVIDE", "(/)" + LOP_SHARED), new CompletionItem("PLUS", " (+)" + LOP_SHARED),
            new CompletionItem("MINUS", "(-)" + LOP_SHARED), new CompletionItem("MOD", "(%)" + LOP_SHARED),
            new CompletionItem("HELPER", "f.q.n\n" + HELPER_DESC), new CompletionItem("AS"), new CompletionItem("TRIGGER", AS_TRIGGER),
            new CompletionItem("AS TRIGGER", AS_TRIGGER), new CompletionItem("TARGET", AS_TARGET),
            new CompletionItem("AS TARGET", AS_TARGET)};

    @Override
    public CompletionItem[] getItemsArray() {
        CompletionItem[] r = Arrays.copyOf(BYTEMAN_KEYWORDS, BYTEMAN_KEYWORDS.length);
        Arrays.sort(r);
        return r;
    }

    @Override
    public List<CompletionItem> getItemsList() {
        List<CompletionItem> l = Arrays.asList(BYTEMAN_KEYWORDS);
        Collections.sort(l);
        return l;
    }

    @Override
    public Pattern getRecommendedDelimiterSet() {
        return CompletionItem.CompletionItemSet.delimiterWordSet();
    }

    @Override
    public String toString() {
        return "Byteman keywords - good to concat with java";
    }
}
