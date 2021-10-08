package org.jrd.backend.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompileUploadCliTest extends AbstractAgentNeedingTest {
    private String[] args;
    private Cli cli;
    private static final String NEW_GREETING = "Greetings";


    @Override
    AbstractSourceTestClass dummyProvider() throws AbstractSourceTestClass.SourceTestClassWrapperException {
        return new ModifiableDummyTestingHelper()
                .writeDefault()
                .compile()
                .execute();
    }

    void testBytes(String pucComponent) throws Exception {
        args = new String[]{Cli.BYTES, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();

        byte[] bytes = streams.getOutBytes();
        byte[] fileContents = Files.readAllBytes(Path.of(dummy.getDotClassPath()));

        Assertions.assertArrayEquals(fileContents, bytes);
    }


    @Test
    void testBytes() throws Exception {
        testBytes(dummy.getPid());
        testBytes(dummy.getClasspath());
    }

    void testBase64Bytes(String pucComponent) throws Exception {
        args = new String[]{Cli.BASE64, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();

        byte[] base64Bytes = streams.getOut().trim().getBytes(StandardCharsets.UTF_8);
        byte[] fileContents = Files.readAllBytes(Path.of(dummy.getDotClassPath()));
        byte[] encoded = Base64.getEncoder().encode(fileContents);

        Assertions.assertArrayEquals(encoded, base64Bytes);
    }

    @Test
    void testBase64Bytes() throws Exception {
        testBase64Bytes(dummy.getPid());
        testBase64Bytes(dummy.getClasspath());
    }

    void testBytesAndBase64BytesEqual(String pucComponent) throws Exception {
        args = new String[]{Cli.BYTES, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        byte[] bytes = streams.getOutBytes();

        args = new String[]{Cli.BASE64, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String base64 = streams.getOut().trim();
        byte[] decoded = Base64.getDecoder().decode(base64);

        Assertions.assertArrayEquals(bytes, decoded);
    }

    @Test
    void testBytesAndBase64BytesEqual() throws Exception {
        testBytesAndBase64BytesEqual(dummy.getPid());
        testBytesAndBase64BytesEqual(dummy.getClasspath());
    }


    void testDecompileJavap(String pucComponent, String option) throws Exception {
        args = new String[]{Cli.DECOMPILE, pucComponent, "javap" + option, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String jrdDisassembled = streams.getOut();
        String javapDisassembled = dummy.executeJavaP(option);

        // JRD javap has additional debug comment lines + header is different
        assertEqualsWithTolerance(jrdDisassembled, javapDisassembled, 0.8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-v"})
    void testDecompileJavap(String option) throws Exception {
        testDecompileJavap(dummy.getPid(), option);
        testDecompileJavap(dummy.getClasspath(), option);
    }


    void testOverwrite(String pucComponent) throws Exception {
        createReplacement(NEW_GREETING);

        args = new String[]{
                Cli.OVERWRITE,
                pucComponent,
                dummy.getFqn(),
                dummy.getDotClassPath() // contains newGreeting because of try-catch above
        };
        cli = new Cli(args, model);

        Assertions.assertDoesNotThrow(() -> cli.consumeCli());
        Assertions.assertTrue(streams.getOut().contains("success"));

        // assert that change propagated, unfortunately we have to rely on another operation here
        bytecodeContainsNewString(pucComponent, NEW_GREETING);
    }

    @Test
    void testOverwriteRunning() throws Exception {
        testOverwrite(dummy.getPid());
        dummyOutputWasChanged(NEW_GREETING);
    }

    @Test
    void testOverwriteCp() throws Exception {
        testOverwrite(dummy.getClasspath());
    }

    void testOverwriteStdIn(String pucComponent) throws Exception {
        createReplacement(NEW_GREETING);

        args = new String[]{
                Cli.OVERWRITE,
                pucComponent,
                dummy.getFqn()
        };
        cli = new Cli(args, model);

        // setup input stream
        ByteArrayInputStream fakeIn = new ByteArrayInputStream(Files.readAllBytes(Path.of(dummy.getDotClassPath())));
        final InputStream originalIn = System.in;
        System.setIn(fakeIn);

        Assertions.assertDoesNotThrow(() -> cli.consumeCli());
        Assertions.assertTrue(streams.getOut().contains("success"));
        bytecodeContainsNewString(pucComponent, NEW_GREETING);
        System.setIn(originalIn); // revert input stream
    }

    /**
     * Why this have oposite definition then its soulmate in CliTest:
     * <p>
     * Because TestingModifiableDummy keeps asking for new class definition
     * So immediately once we disconnect agent, the class get restored for original receipt
     */
    @Test
    void testOverwriteStdInRunning() throws Exception {
        testOverwriteStdIn(dummy.getPid());
        dummyOutputWasChanged(NEW_GREETING);
    }

    @Test
    void testOverwriteStdInCp() throws Exception {
        testOverwriteStdIn(dummy.getClasspath());
    }

    private static void createReplacement(String newGreeting) {
        try {
            new ModifiableDummyTestingHelper()
                    .write(newGreeting)
                    .compile();
        } catch (AbstractSourceTestClass.SourceTestClassWrapperException e) {
            Assertions.fail("Failed to create data to be uploaded.", e);
        }
    }

    /**
     * As the class definition was transforemd, in OpenJDK HotSpot
     * this implementation of dummy did requested (by the call of new ..().print() )
     * this new deffinition and so we can see the change
     *
     * @param newString
     * @throws Exception
     */
    private void dummyOutputWasChanged(String newString) throws Exception {
        Thread.sleep(1000); //the test must wait while reader do something
        Assertions.assertTrue(dummy.getOutString().contains(dummy.getGreetings()));
        Assertions.assertTrue(dummy.getOutString().contains(newString));
    }

    private void bytecodeContainsNewString(String pucComponent, String newString) throws Exception {
        args = new String[]{Cli.DECOMPILE, pucComponent, "javap-v", dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();
        String overwrittenClassInVm = streams.getOut();

        Assertions.assertFalse(overwrittenClassInVm.contains(dummy.getGreetings()));
        Assertions.assertTrue(overwrittenClassInVm.contains(newString));
    }

    @Test
    void testOverwriteWarning() {
        String nonClassFile = dummy.getDotClassPath().replace(".class", "");
        try {
            Files.copy(Path.of(dummy.getDotClassPath()), Path.of(nonClassFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Assertions.fail("Failed to copy file.", e);
        }

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                nonClassFile
        };
        cli = new Cli(args, model);

        Assertions.assertDoesNotThrow(() -> cli.consumeCli());
        String output = streams.getErr();
        Assertions.assertTrue(output.contains("WARNING:"));
    }


    @Test
    void testDecompileCompileCfr() throws Exception {
        final String plugin = "Cfr";
        File decompiledFile = File.createTempFile("jrd", "test.java");
        args = new String[]{
                Cli.DECOMPILE,
                dummy.getPid(),
                plugin,
                dummy.getFqn(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, decompiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String sOrig = Files.readAllLines(
                decompiledFile.toPath(), StandardCharsets.UTF_8).stream()
                .collect(Collectors.joining("\n"));
        String sNoCommnets = Files.readAllLines(
                decompiledFile.toPath(), StandardCharsets.UTF_8).stream()
                .filter(a -> !(a.trim().startsWith("/") || a.trim().startsWith("*")))
                .collect(Collectors.joining("\n"));
        assertEqualsWithTolerance(sOrig, sNoCommnets, 0.9);
        assertEqualsWithTolerance(sNoCommnets, dummy.getDefaultContentWithPackage(), 0.85);

        File compiledFile = File.createTempFile("jrd", "test.class");
        args = new String[]{
                Cli.COMPILE,
                Cli.CP, dummy.getPid(),
                decompiledFile.getAbsolutePath(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String compiled = readBinaryAsString(compiledFile);
        String original = readBinaryAsString(new File(dummy.getDotClassPath()));
        assertEqualsWithTolerance(compiled, original, 0.9);

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                decompiledFile.getAbsolutePath()//nonsense, will not be accepted
        };
        cli = new Cli(args, model);
        Exception ex = null;
        try {
            cli.consumeCli();
        } catch (Exception eex) {
            ex = eex;
        }
        Assertions.assertNotNull(ex);
    }

    @Test
    void testDecompileCompileJasm() throws Exception {
        final String plugin = "jasm";
        File decompiledFile = File.createTempFile("jrd", "test.java");
        args = new String[]{
                Cli.DECOMPILE,
                dummy.getPid(),
                plugin,
                dummy.getFqn(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, decompiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String sOrig = Files.readAllLines(decompiledFile.toPath(), StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
        String sLine = Files.readAllLines(decompiledFile.toPath(), StandardCharsets.UTF_8).stream().collect(Collectors.joining(" "));
        //unluckily there is nothing to compare to, unless we wish to call jasm from here "again"
        //so at least some verifiers
        Assertions.assertTrue(sOrig.contains("{"));
        Assertions.assertTrue(sOrig.contains("}"));
        Assertions.assertTrue(sOrig.contains("version"));
        Assertions.assertTrue(sOrig.contains("invokevirtual"));
        Assertions.assertTrue(sOrig.contains("invokestatic"));
        Assertions.assertTrue(sOrig.contains("goto"));
        Assertions.assertTrue(sLine.matches(".*package\\s+testing/modifiabledummy;.*"));
        Assertions.assertTrue(sLine.matches(".*class\\s+TestingModifiableDummy.*"));
        Assertions.assertTrue(sLine.matches(".*public\\s+Method\\s+\"<init>\".*"));
        Assertions.assertTrue(sLine.matches(".*new\\s+class\\s+TestingModifiableDummy.*"));
        Assertions.assertTrue(sLine.matches(".*private\\s+Method\\s+print.*"));
        Assertions.assertTrue(sLine.matches(".*getstatic\\s+Field\\s+java/lang/System.out:\"Ljava/io/PrintStream;\";.*"));
        Assertions.assertTrue(sLine.matches(".*ldc\\s+String\\s+\"Hello\";.*"));

        File compiledFile = File.createTempFile("jrd", "test.class");
        args = new String[]{
                Cli.COMPILE,
                Cli.CP, dummy.getPid(),
                Cli.P, plugin,
                decompiledFile.getAbsolutePath(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String compiled = readBinaryAsString(compiledFile);
        String original = readBinaryAsString(new File(dummy.getDotClassPath()));
        assertEqualsWithTolerance(compiled, original, 0.4); //yah, jasm performance is not greate

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                decompiledFile.getAbsolutePath() //some nonsense, should fail
        };
        cli = new Cli(args, model);
        Exception ex = null;
        try {
            cli.consumeCli();
        } catch (Exception eex) {
            ex = eex;
        }
        Assertions.assertNotNull(ex);
    }

    @Test
    void testDecompileCompileJcoder() throws Exception {
        final String plugin = "jcoder";
        File decompiledFile = File.createTempFile("jrd", "test.java");
        args = new String[]{
                Cli.DECOMPILE,
                dummy.getPid(),
                plugin,
                dummy.getFqn(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, decompiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String sOrig = Files.readAllLines(decompiledFile.toPath(), StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
        //unluckily there is nothing to compare to, unless we wish to call jasm from here "again"

        File compiledFile = File.createTempFile("jrd", "test.class");
        args = new String[]{
                Cli.COMPILE,
                Cli.CP, dummy.getPid(),
                Cli.P, plugin,
                decompiledFile.getAbsolutePath(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String compiled = readBinaryAsString(compiledFile);
        String original = readBinaryAsString(new File(dummy.getDotClassPath()));
        assertEqualsWithTolerance(compiled, original, 0.4); //yah, jasm performance is not greate

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                decompiledFile.getAbsolutePath() //some nonsense, should fail
        };
        cli = new Cli(args, model);
        Exception ex = null;
        try {
            cli.consumeCli();
        } catch (Exception eex) {
            ex = eex;
        }
        Assertions.assertNotNull(ex);
    }


    @Test
    void testGlobalApi() throws Exception {
        args = new String[]{
                Cli.API,
                dummy.getPid()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        String apiHelp = streams.getOut();
        Assertions.assertTrue(apiHelp.contains("org.jrd.agent.api.Variables.Global.get"));
        Assertions.assertTrue(apiHelp.contains("org.jrd.agent.api.Variables.Global.set"));

        File decompiledFile = File.createTempFile("jrd", "test.java");

        String withNonsense = dummy.getDefaultContentWithPackage().replace("/*API_PLACEHOLDER*/", "some nonsese\n");
        Files.write(decompiledFile.toPath(), withNonsense.getBytes(StandardCharsets.UTF_8));
        File compiledFile = File.createTempFile("jrd", "test.class");
        args = new String[]{
                Cli.COMPILE,
                Cli.CP, dummy.getPid(),
                decompiledFile.getAbsolutePath(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        Exception expectedEx = null;
        try {
            cli.consumeCli();
        } catch (Exception ex) {
            String afterCompilationsOut = streams.getOut();
            String afterCompilationsErr = streams.getErr();
            expectedEx = ex;
        }
        Assertions.assertNotNull(expectedEx);

        String withApi = dummy.getDefaultContentWithPackage().replace("/*API_PLACEHOLDER*/", "" +
                "Integer i = (Integer)(org.jrd.agent.api.Variables.Global.getOrCreate(\"counter\", new Integer(0)));\n" +
                "i=i+1;\n" +
                "org.jrd.agent.api.Variables.Global.set(\"counter\", i);\n" +
                "System.out.println(\"API: \"+i+\" had spoken\");\n");
        Files.write(decompiledFile.toPath(), withApi.getBytes(StandardCharsets.UTF_8));
        args = new String[]{
                Cli.COMPILE,
                Cli.CP, dummy.getPid(),
                decompiledFile.getAbsolutePath(),
                Cli.SAVE_LIKE, Cli.Saving.EXACT,
                Cli.SAVE_AS, compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        try {
            cli.consumeCli();
        } catch (Exception ex) {
            String afterCompilationsOut = streams.getOut();
            String afterCompilationsErr = streams.getErr();
            throw ex;
        }
        String compiled = readBinaryAsString(compiledFile);
        String original = readBinaryAsString(new File(dummy.getDotClassPath()));
        assertEqualsWithTolerance(compiled, original, 0.4);

        args = new String[]{
                Cli.OVERWRITE,
                dummy.getPid(),
                dummy.getFqn(),
                compiledFile.getAbsolutePath()
        };
        cli = new Cli(args, model);
        cli.consumeCli();
        Thread.sleep(1000);
        String mainOutput = dummy.getOutString();
        Assertions.assertTrue(mainOutput.contains("API: 1 had spoken"));
        Assertions.assertTrue(mainOutput.contains("API: 2 had spoken"));
        Assertions.assertTrue(mainOutput.contains("API: 3 had spoken"));
        Assertions.assertTrue(mainOutput.contains("API: 4 had spoken"));
    }
}
