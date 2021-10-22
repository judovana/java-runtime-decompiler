package org.jrd.backend.data;

import org.jrd.frontend.frame.main.DecompilationController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jrd.backend.data.Cli.BASE64;
import static org.jrd.backend.data.Cli.BYTES;
import static org.jrd.backend.data.Cli.COMPILE;
import static org.jrd.backend.data.Cli.CP;
import static org.jrd.backend.data.Cli.DECOMPILE;
import static org.jrd.backend.data.Cli.H;
import static org.jrd.backend.data.Cli.HELP;
import static org.jrd.backend.data.Cli.INIT;
import static org.jrd.backend.data.Cli.LIST_CLASSES;
import static org.jrd.backend.data.Cli.LIST_JVMS;
import static org.jrd.backend.data.Cli.LIST_PLUGINS;
import static org.jrd.backend.data.Cli.OVERWRITE;
import static org.jrd.backend.data.Cli.R;
import static org.jrd.backend.data.Cli.VERBOSE;
import static org.jrd.backend.data.Cli.VERSION;
import static org.jrd.backend.data.Help.BASE64_FORMAT;
import static org.jrd.backend.data.Help.BYTES_FORMAT;
import static org.jrd.backend.data.Help.DECOMPILE_FORMAT;
import static org.jrd.backend.data.Help.INIT_FORMAT;
import static org.jrd.backend.data.Help.LIST_CLASSES_FORMAT;
import static org.jrd.backend.data.Help.OVERWRITE_FORMAT;
import static org.jrd.backend.data.Help.printHelpText;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CliTest extends AbstractAgentNeedingTest {
    private String[] args;
    private Cli cli;
    private static final String NEW_GREETINGS = "Greetings";


    private static final String UNKNOWN_FLAG = "--zyxwvutsrqponmlqjihgfedcba";

    @Override
    AbstractSourceTestClass dummyProvider() throws AbstractSourceTestClass.SourceTestClassWrapperException {
        return new TestingDummyHelper()
                .writeDefault()
                .compile()
                .execute();
    }

    private String prependSlashes(String original, int count) {
        return "-".repeat(count) + original;
    }

    private String cleanSlashes(String original) {
        return original.replace("-", "");
    }

    private String oneSlash(String flag) {
        return prependSlashes(cleanSlashes(flag), 1);
    }

    private String twoSlashes(String flag) {
        return prependSlashes(cleanSlashes(flag), 2);
    }

    @Test
    void testShouldBeVerbose() {
        // gui verbose
        args = new String[]{VERBOSE};
        cli = new Cli(args, model);
        assertTrue(cli.shouldBeVerbose());

        // gui not verbose
        args = new String[]{};
        cli = new Cli(args, model);
        assertFalse(cli.shouldBeVerbose());

        // cli verbose
        args = new String[]{VERBOSE, UNKNOWN_FLAG};
        cli = new Cli(args, model);
        assertTrue(cli.shouldBeVerbose());

        // cli not verbose
        args = new String[]{UNKNOWN_FLAG};
        cli = new Cli(args, model);
        assertFalse(cli.shouldBeVerbose());
    }

    @Test
    void testIsGui() {
        // is gui
        args = new String[]{};
        cli = new Cli(args, model);
        assertTrue(cli.isGui());

        args = new String[]{VERBOSE};
        cli = new Cli(args, model);
        assertTrue(cli.isGui());

        // is cli
        args = new String[]{UNKNOWN_FLAG};
        cli = new Cli(args, model);
        assertFalse(cli.isGui());

        args = new String[]{VERBOSE, UNKNOWN_FLAG};
        cli = new Cli(args, model);
        assertFalse(cli.isGui());
    }

    @Test
    void testHelp() throws Exception {
        args = new String[]{HELP};
        cli = new Cli(args, model);

        cli.consumeCli();
        String cliResult = streams.getOut();

        printHelpText();
        String methodResult = streams.getOut();

        assertEquals(cliResult, methodResult);
    }

    @Test
    void testVersion() throws Exception {
        args = new String[]{VERSION};
        cli = new Cli(args, model);

        cli.consumeCli();
        String cliResult = streams.getOut();

        assertEquals(cliResult.trim(), MetadataProperties.getInstance().toString());
    }

    private String processFormatDefault(String original) {
        return processFormat(
                original,
                dummy.getPid(),
                new String[]{dummy.getClassRegex()},
                dummy.getFqn(),
                dummy.getDotClassPath(),
                "javap"
        );
    }

    private static String processFormat(
            String original, String puc, String[] classRegex, String className, String classFile, String plugin
    ) {
        String result = original
                .replace("...", "")
                .replace("<PUC>", puc)
                .replace("<PLUGIN>", plugin);

        if (classRegex != null) {
            result = result.replace("<CLASS REGEX>", String.join(" ", classRegex));
        }
        if (className != null) {
            result = result.replace("<FQN>", original.startsWith(INIT) ? "java.lang.Override" : className);
        }
        if (classFile != null) {
            result = result.replace("<CLASS FILE>", classFile);
        }

        return result
                .replaceAll("\\[.*?<.*?>.*?]", "") //remove unused optional arguments
                .replaceAll("\\[(.*?)]", "$1"); //remove brackets around used optional arguments
    }

    private String[] processArgs(String operation) {
        if (operation.contains(" ")) {
            return processFormatDefault(operation).split(" ");
        }

        return new String[]{operation};
    }

    // temporarily missing COMPILE
    private static Stream<String> validOperationSource() {
        return Stream.of(
                H, HELP, VERBOSE, VERSION, LIST_JVMS, LIST_PLUGINS, INIT_FORMAT,
                LIST_CLASSES_FORMAT, BYTES_FORMAT, BASE64_FORMAT, DECOMPILE_FORMAT, OVERWRITE_FORMAT
        );
    }

    @ParameterizedTest
    @MethodSource("validOperationSource")
    void testValidOperation(String operation) {
        args = processArgs(operation);
        cli = new Cli(args, model);

        assertDoesNotThrow(cli::consumeCli);
    }

    @ParameterizedTest
    @MethodSource("validOperationSource")
    void testValidOperationTwoSlashes(String operation) {
        args = processArgs(operation);
        args[0] = twoSlashes(args[0]);
        cli = new Cli(args, model);

        assertDoesNotThrow(cli::consumeCli);
    }

    @ParameterizedTest
    @ValueSource(strings = {VERBOSE, LIST_JVMS, LIST_PLUGINS, OVERWRITE_FORMAT, INIT_FORMAT})
    void testTooManyArguments(String operation) {
        if (operation.contains(" ")) {
            args = processFormatDefault(operation + " " + UNKNOWN_FLAG).split(" ");
        } else {
            args = new String[]{operation, UNKNOWN_FLAG};
        }
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli());
    }

    @Test
    void testInvalidOperation() {
        args = new String[]{UNKNOWN_FLAG};
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli());
    }

    private List<String> queryJvmList() throws Exception {
        args = new String[]{LIST_JVMS};
        cli = new Cli(args, model);

        cli.consumeCli();
        String jvms = streams.getOut();

        List<String> pids = new ArrayList<>(); // test dummy termination can take time to propagate => List.contains
        Matcher m = dummy.getJvmListRegex().matcher(jvms);
        while (m.find()) {
            pids.add(m.group(1));
        }

        return pids;
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    @Timeout(10)
    void testListJvms() throws Exception {
        while (!queryJvmList().contains(dummy.getPid())) {
            Thread.sleep(5000); // vmManager only refreshes vmList every 5 seconds
        }
    }

    void testListClasses(String pucComponent) throws Exception {
        // no regex
        args = new String[]{VERBOSE, LIST_CLASSES, pucComponent};
        cli = new Cli(args, model);

        cli.consumeCli();
        String allClassesDefault = streams.getOut();

        Matcher m = dummy.getExactClassRegex().matcher(allClassesDefault);
        if (!m.find()) {
            fail("Class " + dummy.getClassName() + " not found when listing all classes.");
        }

        // all regex
        args = new String[]{LIST_CLASSES, pucComponent, ".*"};
        cli = new Cli(args, model);

        cli.consumeCli();
        String allClassesRegex = streams.getOut();

        m = dummy.getExactClassRegex().matcher(allClassesRegex);
        if (!m.find()) {
            fail("Class " + dummy.getClassName() + " not found when listing all classes via .* regex.");
        }

        // exact class list differs between dummy process executions
        assertEqualsWithTolerance(Arrays.asList(allClassesDefault.split("\n")), Arrays.asList(allClassesRegex.split("\n")), 0.9);

        // specific regex
        classListMatchesExactly(pucComponent, dummy.getExactClassRegex());
    }

    @Test
    void testListClasses() throws Exception {
        testListClasses(dummy.getPid());
        testListClasses(dummy.getClasspath());
    }

    private void classListMatchesExactly(String pucComponent, Pattern exactClassRegex) throws Exception {
        Matcher m;
        args = new String[]{LIST_CLASSES, pucComponent, exactClassRegex.toString()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String ourClass = streams.getOut();

        m = exactClassRegex.matcher(ourClass);
        if (!m.find()) {
            fail("Class " + exactClassRegex + " not found when listing classes via exact regex.");
        }

        assertEquals(ourClass.lines().count(), 1);
    }

    // until CLI plugin importing is added, this is the only certain output
    @Test
    void testListPlugins() throws Exception {
        args = new String[]{LIST_PLUGINS};
        cli = new Cli(args, model);

        cli.consumeCli();
        String plugins = streams.getOut();

        assertTrue(plugins.contains("javap Internal/valid - null"));
        assertTrue(plugins.contains("javap -v Internal/valid - null"));
    }

    void testBytes(String pucComponent) throws Exception {
        args = new String[]{BYTES, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();

        byte[] bytes = streams.getOutBytes();
        byte[] fileContents = Files.readAllBytes(Path.of(dummy.getDotClassPath()));

        assertArrayEquals(fileContents, bytes);
    }

    @Test
    void testBytes() throws Exception {
        testBytes(dummy.getPid());
        testBytes(dummy.getClasspath());
    }

    void testBase64Bytes(String pucComponent) throws Exception {
        args = new String[]{BASE64, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();

        byte[] base64Bytes = streams.getOut().trim().getBytes(StandardCharsets.UTF_8);
        byte[] fileContents = Files.readAllBytes(Path.of(dummy.getDotClassPath()));
        byte[] encoded = Base64.getEncoder().encode(fileContents);

        assertArrayEquals(encoded, base64Bytes);
    }

    @Test
    void testBase64Bytes() throws Exception {
        testBase64Bytes(dummy.getPid());
        testBase64Bytes(dummy.getClasspath());
    }

    void testBytesAndBase64BytesEqual(String pucComponent) throws Exception {
        args = new String[]{BYTES, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        byte[] bytes = streams.getOutBytes();

        args = new String[]{BASE64, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String base64 = streams.getOut().trim();
        byte[] decoded = Base64.getDecoder().decode(base64);

        assertArrayEquals(bytes, decoded);
    }

    @Test
    void testBytesAndBase64BytesEqual() throws Exception {
        testBytesAndBase64BytesEqual(dummy.getPid());
        testBytesAndBase64BytesEqual(dummy.getClasspath());
    }

    private Stream<Arguments> tooFewArgumentsSource() {
        String unimportantPid = "123456";
        return Stream.of(
                new String[]{LIST_CLASSES},
                new String[]{BYTES},
                new String[]{BYTES, unimportantPid},
                new String[]{BASE64},
                new String[]{BASE64, unimportantPid},
                new String[]{INIT},
                new String[]{INIT, unimportantPid},
                new String[]{OVERWRITE},
                new String[]{OVERWRITE, unimportantPid},
                new String[]{DECOMPILE},
                new String[]{DECOMPILE, unimportantPid},
                new String[]{DECOMPILE, unimportantPid, "javap"},
                new String[]{COMPILE},
                new String[]{COMPILE, R},
                new String[]{COMPILE, R, CP, unimportantPid},
                new String[]{COMPILE, R, CP, unimportantPid, Cli.P, "unimportantPluginName"}
        ).map(a -> (Object) a).map(Arguments::of); // cast needed because of varargs factory method .of()
    }

    @ParameterizedTest
    @MethodSource("tooFewArgumentsSource")
    void testTooFewArguments(String[] wrongArgs) {
        args = wrongArgs;
        cli = new Cli(args, model);

        assertThrows(IllegalArgumentException.class, () -> cli.consumeCli());
    }

    void testDecompileJavap(String pucComponent, String option) throws Exception {
        args = new String[]{DECOMPILE, pucComponent, "javap" + option, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String jrdDisassembled = streams.getOut().trim();
        String javapDisassembled = dummy.executeJavaP(option).trim();

        // JRD javap has additional debug comment lines + header is different
        assertEqualsWithTolerance(jrdDisassembled, javapDisassembled, 0.9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-v"})
    void testDecompileJavap(String option) throws Exception {
        testDecompileJavap(dummy.getPid(), option);
        testDecompileJavap(dummy.getClasspath(), option);
    }

    @Test
    void testDecompileUnknownPlugin() {
        args = new String[]{DECOMPILE, dummy.getPid(), UNKNOWN_FLAG, dummy.getClassRegex()};
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli());
    }

    @Test
    void testDecompileFilePlugin() {
        String emptyFilePlugin = "";
        try {
            emptyFilePlugin = Files.createTempFile("wrapper", ".json").toAbsolutePath().toString();
        } catch (IOException e) {
            fail(e);
        }

        args = new String[]{DECOMPILE, dummy.getPid(), emptyFilePlugin, dummy.getClassRegex()};
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli());
    }

    @SuppressWarnings("unchecked") // field reflection
    @Test
    void testArgumentCleaning() throws Exception {
        // changing access modifier of filteredArgs for this test is not justifiable
        Field field = Cli.class.getDeclaredField("filteredArgs");
        field.setAccessible(true);

        // nothing gets cleaned
        args = new String[]{LIST_CLASSES, dummy.getPid(), "--"}; // weird regex, but should not get cleaned
        cli = new Cli(args, model);
        cli.consumeCli();

        String[] filteredArgs = ((List<String>) field.get(cli)).toArray(new String[0]);
        assertArrayEquals(args, filteredArgs);

        // only operation gets cleaned, but is still valid
        args = new String[]{twoSlashes(LIST_CLASSES), dummy.getPid(), "--"};
        cli = new Cli(args, model);
        cli.consumeCli();

        filteredArgs = ((List<String>) field.get(cli)).toArray(new String[0]);
        assertArrayEquals(args, filteredArgs);
    }

    void testOverwrite(String pucComponent) throws Exception {
        createReplacement(NEW_GREETINGS);

        args = new String[]{
                OVERWRITE,
                pucComponent,
                dummy.getFqn(),
                dummy.getDotClassPath()
        };
        cli = new Cli(args, model);

        assertDoesNotThrow(() -> cli.consumeCli());
        assertTrue(streams.getOut().contains("success"));

        // assert that change propagated, unfortunately we have to rely on another operation here
        bytecodeContainsNewString(pucComponent, NEW_GREETINGS);
    }


    /**
     * Why this is passing:
     * The TestingDummy never request new class definition.
     * So yes, we change the bytecode, and that get changed.
     * Then we can see the modified definition
     *
     * @throws Exception
     */
    @Test
    void testOverwriteRunning() throws Exception {
        testOverwrite(dummy.getPid());
        dummyOutputWasNotChanged(NEW_GREETINGS);
    }

    @Test
    void testOverwriteCP() throws Exception {
        testOverwrite(dummy.getClasspath());
    }

    void testOverwriteStdIn(String pucComponent) throws Exception {
        createReplacement(NEW_GREETINGS);

        args = new String[]{
                OVERWRITE,
                pucComponent,
                dummy.getFqn()
        };
        cli = new Cli(args, model);

        // setup input stream
        ByteArrayInputStream fakeIn = new ByteArrayInputStream(Files.readAllBytes(Path.of(dummy.getDotClassPath())));
        final InputStream originalIn = System.in;
        System.setIn(fakeIn);

        assertDoesNotThrow(() -> cli.consumeCli());
        assertTrue(streams.getOut().contains("success"));
        bytecodeContainsNewString(pucComponent, NEW_GREETINGS);

        System.setIn(originalIn); // revert input stream
    }

    /**
     * Why this is passing:
     *
     * @throws Exception
     */
    @Test
    void testOverwriteStdInRunning() throws Exception {
        testOverwriteStdIn(dummy.getPid());
        dummyOutputWasNotChanged(NEW_GREETINGS);
    }

    @Test
    void testOverwriteStdInCp() throws Exception {
        testOverwriteStdIn(dummy.getClasspath());
    }

    private static void createReplacement(String newGreeting) {
        try {
            new TestingDummyHelper()
                    .write(newGreeting)
                    .compile();
        } catch (AbstractSourceTestClass.SourceTestClassWrapperException e) {
            fail("Failed to create data to be uploaded.", e);
        }
    }

    /**
     * Although the class definition was transforemd, in OpenJDK HotSpot
     * this implementation of dummy do not request the new definition,
     * adn continues to spit out its jitted output
     *
     * @param newString
     * @throws Exception
     */
    private void dummyOutputWasNotChanged(String newString) throws Exception {
        Thread.sleep(1000); //the test must wait while reader do something
        Assertions.assertTrue(dummy.getOutString().contains(dummy.getGreetings()));
        Assertions.assertFalse(dummy.getOutString().contains(newString));
    }

    private void bytecodeContainsNewString(String pucComponent, String newString) throws Exception {
        args = new String[]{DECOMPILE, pucComponent, "javap-v", dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String overwrittenClassInVm = streams.getOut();

        assertFalse(overwrittenClassInVm.contains(dummy.getGreetings()));
        assertTrue(overwrittenClassInVm.contains(newString));
    }

    @Test
    void testOverwriteWarning() {
        String nonClassFile = dummy.getDotClassPath().replace(".class", "");
        try {
            Files.copy(Path.of(dummy.getDotClassPath()), Path.of(nonClassFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            fail("Failed to copy file.", e);
        }

        args = new String[]{
                OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                nonClassFile
        };
        cli = new Cli(args, model);

        assertDoesNotThrow(() -> cli.consumeCli());
        String output = streams.getErr();
        assertTrue(output.contains("WARNING:"));
    }

    @Test
    void testOverwriteError() {
        args = new String[]{
                OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                dummy.getTargetDir()
        };
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli()); // wrapped IOException
        String output = streams.getErr();
        assertTrue(output.contains("ERROR:"));
    }

    @Test
    void testOverwriteAgentError() {
        args = new String[]{
                OVERWRITE,
                dummy.getPid(),
                UNKNOWN_FLAG, // non-FQN makes agent not find the class
                dummy.getDotClassPath()
        };
        cli = new Cli(args, model);

        RuntimeException e = assertThrows(RuntimeException.class, () -> cli.consumeCli());
        assertEquals(DecompilationController.CLASSES_NOPE, e.getMessage());
    }

    @Test
    void testInit() throws Exception {
        String targetClass = "java.lang.Override";
        args = new String[]{
                INIT,
                dummy.getPid(),
                targetClass
        };
        cli = new Cli(args, model);

        assertDoesNotThrow(() -> cli.consumeCli());

        streams.getOut(); // clean stdout
        classListMatchesExactly(dummy.getPid(), Pattern.compile(targetClass, Pattern.MULTILINE));
    }

    @Test
    void testInitAgentError() {
        args = new String[]{
                INIT,
                dummy.getPid(),
                UNKNOWN_FLAG
        };
        cli = new Cli(args, model);

        assertThrows(RuntimeException.class, () -> cli.consumeCli());
    }

    @Test
    public void testGuessNameIncorrectNoPkg() {
        byte[] contents = dummy.getDefaultContentWithoutPackage().getBytes(StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> Cli.guessName(contents));
    }

    @Test
    public void testGuessNameIncorrectNoClass() {
        byte[] contents = ("package " + dummy.getPackageName() + ";").getBytes(StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> Cli.guessName(contents));
    }

    @Test
    public void testGuessNameIncorrectNoCompile() {
        byte[] contents = "uncompilable text?".getBytes(StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> Cli.guessName(contents));
    }


    private Stream<byte[]> correctClassContents() {
        return Stream.of(
                new TestingDummyHelper().getDefaultContentWithPackage(),
                new TestingDummyHelper().getEmptyClassWithPackage()
        ).map(s -> s.getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("correctClassContents")
    void testGuessNameCorrect(byte[] contents) {
        try {
            assertEquals(
                    dummy.getPackageName() + "." + dummy.getClassName(),
                    Cli.guessName(contents)
            );
        } catch (IOException e) {
            fail(e);
        }
    }

}
