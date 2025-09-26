package org.jrd.backend.data.cli;

import org.jrd.backend.core.agentstore.AgentLiveliness;
import org.jrd.backend.core.agentstore.AgentLoneliness;
import org.jrd.backend.core.agentstore.KnownAgents;
import org.jrd.backend.data.Directories;
import org.jrd.backend.data.MetadataProperties;
import org.jrd.backend.data.cli.utils.Saving;
import org.jrd.frontend.frame.main.GlobalConsole;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jrd.backend.data.cli.CliSwitches.*;

/**
 * Class for relaying help texts to the user.
 */
@SuppressWarnings("ExecutableStatementCount") //big setters
public final class Help {

    public static final String HELP_FORMAT = HELP + ", " + H;
    public static final String VERBOSE_FORMAT = VERBOSE;
    public static final String VERSION_FORMAT = VERSION;
    public static final String HEX_FORMAT = HEX;
    public static final String FS_FORMAT = FS + " <class:path>";
    public static final String CONFIG_FORMAT = CONFIG;
    public static final String BASE_SHARED_FORMAT = " <PUC> <CLASS REGEX>...";
    public static final String BASE_SHARED_OPTIONAL_FORMAT = " <PUC> [<CLASS REGEX>...]";
    public static final String BASE64_FORMAT = BASE64 + BASE_SHARED_FORMAT;
    public static final String BYTES_FORMAT = BYTES + BASE_SHARED_FORMAT;
    public static final String DEPS_FORMAT = DEPS + BASE_SHARED_FORMAT;
    public static final String CLASSLOADER_SET_FORMAT = CLASSLOADER_SET + " <classloader identification>";
    public static final String LIST_JVMS_FORMAT = LIST_JVMS;
    public static final String LIST_AGENTS_FORMAT = LIST_AGENTS;
    public static final String LIST_OVERRIDES_FORMAT = LIST_OVERRIDES + " <PUC>";
    public static final String LIST_CLASSLOADERS_FORMAT = LIST_CLASSLOADERS + " <PUC> [classRegex]";
    public static final String REMOVE_OVERRIDES_FORMAT = REMOVE_OVERRIDES + " <PUC> removalRegex";
    public static final String LIST_PLUGINS_FORMAT = LIST_PLUGINS;
    public static final String LIST_CLASSES_FORMAT = LIST_CLASSES + BASE_SHARED_OPTIONAL_FORMAT;
    public static final String SEARCH_FORMAT = SEARCH + BASE_SHARED_FORMAT + " searchedSubstring true/false (with/without details)";
    public static final String LIST_CLASSESDETAILS_FORMAT = LIST_CLASSESDETAILS + BASE_SHARED_OPTIONAL_FORMAT;
    public static final String LIST_CLASSESBYTECODEVERSIONS_FORMAT = LIST_CLASSESBYTECODEVERSIONS + BASE_SHARED_OPTIONAL_FORMAT;
    public static final String LIST_CLASSESDETAILSVERSIONS_FORMAT = LIST_CLASSESDETAILSBYTECODEVERSIONS + BASE_SHARED_OPTIONAL_FORMAT;
    public static final String COMPILE_FORMAT = COMPILE + " [" + P + " <PLUGIN>] [" + CP + " <PUC>] [" + R + "] <PATH>...";
    public static final String DECOMPILE_FORMAT = DECOMPILE + " <PUC> <PLUGIN> <CLASS REGEX>...";
    public static final String OVERWRITE_FORMAT = OVERWRITE + " <PUC> <FQN> [<CLASS FILE>]";
    public static final String ADD_CLASS_FORMAT = ADD_CLASS + " <PUC> <FQN> <CLASS FILE>";
    public static final String ADD_CLASSES_FORMAT1 = ADD_CLASSES + " <PUC> (<CLASS FILE1>)^n [" + BOOT_CLASS_LOADER + "]";
    public static final String ADD_CLASSES_FORMAT2 = ADD_CLASSES + " <PUC> (<FQN1> <CLASS FILE1>)^n [" + BOOT_CLASS_LOADER + "]";
    public static final String ADD_JAR_FORMAT = ADD_JAR + " <PUC> <JAR FILE> [" + BOOT_CLASS_LOADER + "]";
    public static final String PATCH_FORMAT = PATCH + " <PUC>  <PLUGIN>xor<ADDITIONAL-SOURCE/CLASS-PATH (" + HEX + ") (" + REVERT + ")";
    public static final String INIT_FORMAT = INIT + " <PUC> <FQN>";
    public static final String AGENT_FORMAT =
            AGENT + " <" + AgentLiveliness.class.getSimpleName() + "> " + "<" + AgentLoneliness.class.getSimpleName() + "> " + "<port>";
    public static final String ATTACH_FORMAT = ATTACH + " <PID>";
    public static final String DETACH_FORMAT = DETACH + " URL xor PORT xor PID";
    public static final String API_FORMAT = API + " <PUC>";
    public static final String COMPLETION_FORMAT = COMPLETION + " [<completion1, completion2,...>]";
    public static final String SAVE_AS_FORMAT = SAVE_AS + " <PATH>";
    public static final String SAVE_LIKE_FORMAT = SAVE_LIKE + " <SAVE METHOD>";

    private static final String HELP_TEXT = "Print this help text.";
    private static final String VERBOSE_TEXT = "All exceptions and some debugging strings will be printed to standard error.";
    private static final String VERSION_TEXT = "Print version project name, version and build timestamp.";
    private static final String CONFIG_TEXT = "Print path to main config file. In verbose mode prints file itself..";
    private static final String HEX_TEXT = "Switch all binary operations to work in hex-readbale format (including patching...)";
    private static final String FS_TEXT = "Open jrd in local filesystem  with the set classpath";
    private static final String BASE64_TEXT = "Print Base64 encoded binary form of requested classes of a process.";
    private static final String BYTES_TEXT = "Print binary form of requested classes of a process";
    private static final String DEPS_TEXT = "Print all deps of the selected class(es).";
    private static final String CLASSLOADER_SET_TEXT = "In jvm, there may be multiple classes of each FQN. Each " +
            "of them loaded by different classlaoder. This regex allows you to specify classloader(s) you operate on";
    private static final String LIST_JVMS_TEXT = "List all local Java processes and their PIDs.";
    private static final String LIST_AGENTS_TEXT =
            "JRD keeps record off all local agents, dropping them once inaccessible." + "Use this to list known agents.\nYou can append " +
                    VERSIONS + " to get also version of agent" + " (may be incompatible or not ours at all)";
    private static final String LIST_PLUGINS_TEXT = "List all currently configured decompiler plugins and their statuses.";
    private static final String LIST_OVERRIDES_TEXT = "List all currently overwritten classes";
    private static final String REMOVE_OVERRIDES_TEXT = "remove all matching overwrittes of classes";
    private static final String LIST_CLASSESBYTECODEVERSIONS_TEXT = "list all classes with bytecode version (slow!)";
    private static final String LIST_CLASSESDETAILSVERSIONS_TEXT = "list all classes with details and bytecode version (slow!)";
    private static final String LIST_CLASSLOADERS_TEXT = "list and count all classloaders visible by agent";
    private static final String LIST_CLASSES_TEXT = "List all loaded classes of a process, optionally filtering them.\n" + "Only '" +
            SAVE_LIKE + " " + Saving.EXACT + "' or '" + SAVE_LIKE + " " + Saving.DEFAULT + "' are allowed as saving modifiers.";
    private static final String SEARCH_TEXT = "Will search ascii/utf8 substring in regex-subset binaries in remote vm.\n" +
            "To search in decompiled classes use grep.You can misuses " + HEX + " to include bytecode level";
    private static final String LIST_CLASSESDETAILS_TEXT = "Similar to " + LIST_CLASSES + ", only more details are printed about classes.";
    private static final String COMPILE_TEXT = "Compile local files against runtime classpath, specified by " + CP + ".\n" + "Use " + P +
            " to utilize some plugins' (like jasm or jcoder) bundled compilers.\n" + "Use " + R +
            " for recursive search if <PATH> is a directory.\n" + "If the argument of '" + SAVE_AS + "' is a valid PID or URL, " +
            "the compiled code will be attempted to be injected into that process.\n" + "If multiple PATHs were specified, but no '" +
            SAVE_AS + "', the process fails.\n" + "use: -D of " + GlobalConsole.CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT +
            "= any subset of " + Arrays.stream(GlobalConsole.CPLC_ITEMS).collect(Collectors.joining(",")) + "\n" +
            "to see what is CPLC reolver doing. Warning, CPLC have issues with default (none).";
    private static final String DECOMPILE_TEXT = "Decompile and print classes of a process with the specified decompiler plugin.\n" +
            "Javap can be passed options by appending them without spaces: " + "'javap-v-public ...' executes as 'javap -v -public ...'";
    private static final String OVERWRITE_TEXT =
            "Overwrite class of a process with new bytecode. If <CLASS FILE> is not set, standard input is used.";
    private static final String ADD_CLASS_TEXT =
            "Add class is currently unable to add class, unless all its dependencies are already in running vm. Stdin used if no file.";
    private static final String ADD_JAR_TEXT = "Will add all classes from jar into selected VM." +
            " If you are adding system classes, yo have to specify " + BOOT_CLASS_LOADER;
    private static final String ADD_CLASSES_TEXT1 = "Will add all classes into jar, guess theirs FQN, and sent them into selected VM." +
            " If you are adding system classes, yo have to specify " + BOOT_CLASS_LOADER;
    private static final String ADD_CLASSES_TEXT2 = "Will add all classes into jar, set theirs FQN, and sent them into selected VM." +
            " If you are adding system classes, yo have to specify " + BOOT_CLASS_LOADER;
    private static final String PATCH_TEXT = "You may ignore plugin/path param in " + HEX + " mode." +
            "You can apply patch from STD-IN to classes in <PUC>. The patch can be on source, or on binary if  " + HEX + " is provided\n" +
            "The header (+++/---) must contain dot-delimited FQN of class. All before / (or \\) is stripped." +
            " .class$/.java$  is omitted.\n" + "See gui for the examples.\n" +
            "If plugin is specified, runtime classpath is decompiled, patched," + " compiled (is not (de)compiled with " + HEX +
            ") and uploaded.\n " +
            "If plugin is not specified, then source from additional-source-path is patched, compiled and uploaded.\n" + "If " + HEX +
            " is set, then binary from additional-class-path is patched and uploaded. In both cases, class is INIT before all.\n" +
            "This is a bit different from gui, where patch is patching just one file.\n" +
            "In cli can contain several files, and is moreover direct shortcut to init, bytes, (decompile,) patch,( detect bytecode" +
            " level, compile,) upload.\n" + "As patch tool, " + REVERT + " will invert the patch\n" +
            "For debugging -savelike dir can be used to save patched and compiled output." +
            " -savelike fqn can be used to save patched sources";
    private static final String INIT_TEXT = "Try to initialize a class in a running JVM (has no effect in FS VMs). " +
            "Because class loading is lazy, the class you need might be missing, eg. java.lang.Override.";
    static final String ATTACH_TEXT = "Will only attach the agent to selected pid. Prints out the port for future usage.";
    public static final String AGENT_TEXT =
            "Control how agent is attached. Have sense only in operations attaching to PID. Possible values of " +
                    AgentLiveliness.class.getSimpleName() + ":\n" +
                    Arrays.stream(AgentLiveliness.values()).map(i -> "  " + i.toString() + " - " + i.toHelp())
                            .collect(Collectors.joining("\n")) +
                    "\n" + "optional, defaults to " + AgentLiveliness.SESSION + " for " + OVERWRITE + ", " + PATCH + ", " + ATTACH + "" +
                    " and " + COMPILE + " with upload to VM.\n" + "to " + AgentLiveliness.ONE_SHOT + " for read. Followed one of " +
                    AgentLoneliness.class.getSimpleName() + ":\n" +
                    Arrays.stream(AgentLoneliness.values()).map(i -> "  " + i.toString() + " - " + i.toHelp())
                            .collect(Collectors.joining("\n")) +
                    "\n" + "optional, defaults to " + AgentLoneliness.SINGLE_INSTANCE + "\n" +
                    "You can also specify *port* where the agent will listen, otherwise default port is calculated.\n" +
                    "JRD keep record of all permanent and session agents, so they can be listed/reused/removed.\n" +
                    "This list is still checked for consistency.\n" + "File is " + KnownAgents.JRD_TMP_FILE.toFile().getAbsolutePath() +
                    "\nThe " + ADD_CLASS + ", " + ADD_CLASSES + ", " + ADD_JAR + " is not " + AgentLiveliness.SESSION + " or " +
                    AgentLiveliness.PERMANENT + " because there is no way to unload them";
    private static final String DETACH_TEXT = "Will close and detach " + AgentLiveliness.PERMANENT +
            " agent from given localhost:port or url. To detach from PID, a valid mapping in " +
            KnownAgents.JRD_TMP_FILE.toFile().getAbsolutePath() + " file is needed";
    private static final String API_TEXT =
            "Will print out dynamic, real-to-time api which can be used to insert fields/methods to " + "running vm";
    private static final String COMPLETION_TEXT = "Without parameter Will print available code completions. With parameter of" +
            "coded completion names(delimited by ',' or ' '), will print details of that exact set of completion.";
    private static final String SAVE_AS_TEXT = "All outputs will be written to PATH instead of to standard output.";
    private static final String SAVE_LIKE_TEXT = "Specify how saving will behave.";

    private static final String NOTES_SLASH = "All options can be with either one or two leading slashes ('-').";
    private static final String NOTES_REGEX =
            "When using <CLASS REGEX>, escape dollar signs '$' of inner classes to '\\$'; otherwise they mean the end-of-line.";
    private static final String NOTES_FQN = "<FQN> is the fully qualified name of a class as per the Java Language Specification §6.7.";
    private static final String NOTES_PUC = "<PUC>, short for PidUrlClasspath, can be one of:";
    private static final String NOTES_SAVE = "<SAVE METHOD> can be one of:";
    private static final String[] NOTES_PUC_ITEMS =
            new String[]{"local process PID", "remote process URL, in the format of 'hostname:port'",
                    "classpath of JAR on the filesystem (classpath separator is '" + File.pathSeparator + "')"

            };
    private static final String[] NOTES_SAVE_ITEMS =
            new String[]{"'" + Saving.DIR + "' - Result will be saved as '<PATH>/fully/qualified/name.class'. Default for .class binaries.",
                    "'" + Saving.FQN + "' - Result will be saved as '<PATH>/fully.qualified.name.java'. Default for .java sources.",
                    "'" + Saving.EXACT + "' - Result will be saved exactly to '<PATH>'. Default for everything else.",
                    "'" + Saving.DEFAULT + "' - Saving uses the defaults mentioned above."};

    private static final String LAUNCHER_LINUX = "./start.sh";
    private static final String LAUNCHER_WINDOWS = "start.bat";

    static final Map<String, String> ALL_OPTIONS = new LinkedHashMap<>();
    static final Map<String, String> SAVING_OPTIONS = new LinkedHashMap<>();
    private static final Map<String, String[]> NOTES = new LinkedHashMap<>();

    static {
        ALL_OPTIONS.put(HELP_FORMAT, HELP_TEXT);
        ALL_OPTIONS.put(VERBOSE_FORMAT, VERBOSE_TEXT);
        ALL_OPTIONS.put(VERSION_FORMAT, VERSION_TEXT);
        ALL_OPTIONS.put(CONFIG_FORMAT, CONFIG_TEXT);
        ALL_OPTIONS.put(HEX_FORMAT, HEX_TEXT);
        ALL_OPTIONS.put(FS_FORMAT, FS_TEXT);
        ALL_OPTIONS.put(CLASSLOADER_SET_FORMAT, CLASSLOADER_SET_TEXT);
        ALL_OPTIONS.put(LIST_JVMS_FORMAT, LIST_JVMS_TEXT);
        ALL_OPTIONS.put(LIST_PLUGINS_FORMAT, LIST_PLUGINS_TEXT);
        ALL_OPTIONS.put(LIST_AGENTS_FORMAT, LIST_AGENTS_TEXT);
        ALL_OPTIONS.put(LIST_OVERRIDES_FORMAT, LIST_OVERRIDES_TEXT);
        ALL_OPTIONS.put(REMOVE_OVERRIDES_FORMAT, REMOVE_OVERRIDES_TEXT);
        ALL_OPTIONS.put(LIST_CLASSES_FORMAT, LIST_CLASSES_TEXT);
        ALL_OPTIONS.put(LIST_CLASSLOADERS_FORMAT, LIST_CLASSLOADERS_TEXT);
        ALL_OPTIONS.put(LIST_CLASSESDETAILS_FORMAT, LIST_CLASSESDETAILS_TEXT);
        ALL_OPTIONS.put(LIST_CLASSESBYTECODEVERSIONS_FORMAT, LIST_CLASSESBYTECODEVERSIONS_TEXT);
        ALL_OPTIONS.put(LIST_CLASSESDETAILSVERSIONS_FORMAT, LIST_CLASSESDETAILSVERSIONS_TEXT);
        ALL_OPTIONS.put(SEARCH_FORMAT, SEARCH_TEXT);
        ALL_OPTIONS.put(BASE64_FORMAT, BASE64_TEXT);
        ALL_OPTIONS.put(BYTES_FORMAT, BYTES_TEXT);
        ALL_OPTIONS.put(DEPS_FORMAT, DEPS_TEXT);
        ALL_OPTIONS.put(PATCH_FORMAT, PATCH_TEXT);
        ALL_OPTIONS.put(COMPILE_FORMAT, COMPILE_TEXT);
        ALL_OPTIONS.put(DECOMPILE_FORMAT, DECOMPILE_TEXT);
        ALL_OPTIONS.put(OVERWRITE_FORMAT, OVERWRITE_TEXT);
        ALL_OPTIONS.put(ADD_CLASS_FORMAT, ADD_CLASS_TEXT);
        ALL_OPTIONS.put(ADD_JAR_FORMAT, ADD_JAR_TEXT);
        ALL_OPTIONS.put(ADD_CLASSES_FORMAT1, ADD_CLASSES_TEXT1);
        ALL_OPTIONS.put(ADD_CLASSES_FORMAT2, ADD_CLASSES_TEXT2);
        ALL_OPTIONS.put(INIT_FORMAT, INIT_TEXT);
        ALL_OPTIONS.put(ATTACH_FORMAT, ATTACH_TEXT);
        ALL_OPTIONS.put(AGENT_FORMAT, AGENT_TEXT);
        ALL_OPTIONS.put(DETACH_FORMAT, DETACH_TEXT);
        ALL_OPTIONS.put(API_FORMAT, API_TEXT);
        ALL_OPTIONS.put(COMPLETION_FORMAT, COMPLETION_TEXT);

        SAVING_OPTIONS.put(SAVE_AS_FORMAT, SAVE_AS_TEXT);
        SAVING_OPTIONS.put(SAVE_LIKE_FORMAT, SAVE_LIKE_TEXT);

        NOTES.put(NOTES_SLASH, new String[0]);
        NOTES.put(NOTES_REGEX, new String[0]);
        NOTES.put(NOTES_FQN, new String[0]);
        NOTES.put(NOTES_PUC, NOTES_PUC_ITEMS);
        NOTES.put(NOTES_SAVE, NOTES_SAVE_ITEMS);
        NOTES.put(
                "WARNING: ",
                new String[]{"JRD wa designed to overwrite existing classed, with all limits hot swap have.",
                        "And to add new, not yet present, classes.",
                        "You can try to add add already existing class," +
                                " to workaround hotswap limitations, but there is no guarantee, such class will be ever picked up."}
        );
    }

    private static final String[] UNSAVABLE_OPTIONS = new String[]{HELP, H, COMPLETION, REVERT, HEX, BOOT_CLASS_LOADER, SYSTEM_CLASS_LOADER,
            OVERWRITE, INIT, REMOVE_OVERRIDES, ADD_CLASS, ADD_CLASSES, ADD_JAR, LIST_OVERRIDES, LIST_CLASSLOADERS};
    private static final String[] SAVABLE_OPTIONS = new String[]{LIST_CLASSES, LIST_CLASSESDETAILS, BYTES, BASE64, DEPS, COMPILE, DECOMPILE,
            PATCH, API, LIST_JVMS, LIST_PLUGINS, LIST_CLASSESBYTECODEVERSIONS, LIST_CLASSESDETAILSBYTECODEVERSIONS, SEARCH};

    private static final int LONGEST_FORMAT_LENGTH = Stream.of(ALL_OPTIONS.keySet(), SAVING_OPTIONS.keySet()).flatMap(Collection::stream)
            .map(String::length).max(Integer::compare).orElse(30) + 1; // at least one space between format and text

    private Help() {
    }

    public static void printHelpText() {
        printHelpText(new CliHelpFormatter());
    }

    private static void printHelpText(HelpFormatter formatter) {
        formatter.printTitle();
        formatter.printName();

        formatter.printUsageHeading();
        formatter.printUsage();

        formatter.printOptionsHeading();
        formatter.printOptions();

        formatter.printNotesHeading();
        formatter.printNotes();
    }

    public static void main(String[] args) {
        printHelpText(new ManPageFormatter());
    }

    private interface HelpFormatter {
        void printTitle();

        void printName();

        void printUsageHeading();

        /**
         * Prints each {@link #launchOptions() launch option} prepended with the common {@link #launcher()} String.
         * Man page formatting doesn't mind the indentation so this is common for both formatters.
         */
        default void printUsage() {
            for (String launchOption : launchOptions()) {
                System.out.println(indent(1) + launcher(true) + launchOption);
            }
            System.out.println(
                    indent(1) + launcher(false) + "[" + HEX + "]" + " [file, file...]" +
                            " launches standalone hex (or text) editor/diff. Mighty diff. " + HEX + " suggests, how to open `file, file...`"
            );
            System.out.println(
                    indent(1) + launcher(false) + "[" + FS + "]" + " <class:path> will launch temporary filesystem based decompiler"
            );
        }

        void printOptionsHeading();

        void printMainOptionsSubheading();

        void printSavingOptionsSubheading();

        default void printOptions() {
            printMainOptionsSubheading();
            printOptions(ALL_OPTIONS);

            printSavingOptionsSubheading();
            printOptions(SAVING_OPTIONS);
        }

        void printOptions(Map<String, String> map);

        /**
         * Joins options together with a pipe and surrounds them in parentheses.
         *
         * @param options String array containing the individual options
         * @return String in the format of "(opt1|opt2|...)"
         */
        String optionize(String[] options);

        default String indent(int depth) { // default because ManPageFormatter doesn't use it
            return "  ".repeat(depth);
        }

        default String[] launchOptions() {
            return new String[]{"# launches GUI", optionize(UNSAVABLE_OPTIONS), optionize(SAVABLE_OPTIONS) + savingModifiers()};
        }

        String launcher(boolean verbose);

        String savingModifiers();

        void printNotesHeading();

        void printNotes();
    }

    private static final class CliHelpFormatter implements HelpFormatter {

        @Override
        public void printTitle() {
        }

        @Override
        public void printName() {
        }

        @Override
        public void printUsageHeading() {
            System.out.println("Usage:");
        }

        @Override
        public void printOptionsHeading() {
        }

        @Override
        public void printMainOptionsSubheading() {
            System.out.println("Available options:");
        }

        @Override
        public void printSavingOptionsSubheading() {
            System.out.println("Saving modifiers:");
        }

        @Override
        public void printOptions(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String format = entry.getKey();
                String initialSpacing = " ".repeat(LONGEST_FORMAT_LENGTH - format.length());
                String interlineSpacing = "\n" + indent(1) + " ".repeat(LONGEST_FORMAT_LENGTH);

                System.out.println(indent(1) + format + initialSpacing + entry.getValue().replaceAll("\n", interlineSpacing));
            }
        }

        @Override
        public String optionize(String[] options) {
            return "(" + String.join("|", options) + ")";
        }

        @Override
        public String savingModifiers() {
            return " [" + SAVE_AS + " [" + SAVE_LIKE + "]]";
        }

        @Override
        public String launcher(boolean verbose) {
            String tail = " ";
            if (verbose) {
                tail = " [" + VERBOSE + "] ";
            }
            return (Directories.isOsWindows() ? LAUNCHER_WINDOWS : LAUNCHER_LINUX) + tail;
        }

        @Override
        public void printNotesHeading() {
            System.out.println("Additional information");
        }

        @Override
        public void printNotes() {
            for (Map.Entry<String, String[]> entry : NOTES.entrySet()) {
                System.out.println(indent(1) + entry.getKey());

                for (String item : entry.getValue()) {
                    System.out.println(indent(2) + "- " + item);
                }
            }
        }
    }

    private static final class ManPageFormatter implements HelpFormatter {

        String formatWrap(char formatChar, String string) {
            return "\\f" + formatChar + string + "\\fR";
        }

        String manFormat(String line) {
            return line.replace("\\", "\\\\") // escape one more time for man parsing
                    .replaceAll("<(.*?)>", "<\\\\fI$1\\\\fR>"); // underline <PLACEHOLDERS>
        }

        @Override
        public void printTitle() {
            String buildTimestamp;
            String centerTitle;

            try {
                buildTimestamp = MetadataProperties.getInstance().getTimestamp().split(" ")[0];
                centerTitle = MetadataProperties.getInstance().getGroup();
            } catch (IndexOutOfBoundsException e) {
                buildTimestamp = "";
                centerTitle = ""; // empty == defaults to "General Commands Manual"
            }

            System.out.println(".TH JRD 1 \"" + buildTimestamp + "\" \"\" " + centerTitle);
        }

        @Override
        public void printName() {
            System.out.println(".SH NAME");
            System.out.println("JRD - Java Runtime Decompiler");
        }

        @Override
        public void printUsageHeading() {
            System.out.println(".SH SYNOPSIS");
        }

        @Override
        public void printOptionsHeading() {
            System.out.println(".SH OPTIONS");
        }

        @Override
        public void printMainOptionsSubheading() {
            System.out.println(".SS Standard options");
        }

        @Override
        public void printSavingOptionsSubheading() {
            System.out.println(".SS Saving modifiers");
        }

        @Override
        public void printOptions(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String manPageParagraphs = entry.getValue().replaceAll("\n", "\n\n ");

                System.out.println(".HP\n" + manFormat(entry.getKey()) + "\n " + manFormat(manPageParagraphs));
            }
        }

        @Override
        public String optionize(String[] options) {
            return "(" + Stream.of(options).map(s -> formatWrap('B', s)).collect(Collectors.joining("|")) + ")";
        }

        @Override
        public String launcher(boolean verbose) {
            // initial \n is for paragraph separation
            // the trailing space separates from rest of line
            String tail = " ";
            if (verbose) {
                tail = " [" + formatWrap('I', VERBOSE) + "] ";
            }
            return "\n" + optionize(new String[]{LAUNCHER_LINUX, LAUNCHER_WINDOWS}) + tail;
        }

        @Override
        public String savingModifiers() {
            return " [" + formatWrap('I', SAVE_AS) + " [" + formatWrap('I', SAVE_LIKE) + "]]";
        }

        @Override
        public void printNotesHeading() {
            System.out.println(".SH NOTES");
        }

        @Override
        public void printNotes() {
            for (Map.Entry<String, String[]> entry : NOTES.entrySet()) {
                System.out.println(".HP\n" + manFormat(entry.getKey()) + "\n");

                for (String item : entry.getValue()) {
                    System.out.println("\\[bu] " + manFormat(item) + "\n"); // unordered list
                }
            }
        }
    }
}
