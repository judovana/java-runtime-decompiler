package org.jrd.backend.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompileUploadCliTest extends AbstractAgentNeedingTest {
    private String[] args;
    private Cli cli;


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
        String newGreeting = "Greetings";
        createReplacement(newGreeting);

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
        bytecodeContainsNewString(pucComponent, newGreeting);
    }

    @Test
    void testOverwriteRunning() throws Exception {
        testOverwrite(dummy.getPid());
    }

    @Test
    void testOverwriteCp() throws Exception {
        testOverwrite(dummy.getClasspath());
    }

    void testOverwriteStdIn(String pucComponent) throws Exception {
        String newGreeting = "Greetings";
        createReplacement(newGreeting);

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
        bytecodeContainsNewString(pucComponent, newGreeting);

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
    }

    @Test
    void testOverwriteStdInCp() throws Exception {
        testOverwriteStdIn(dummy.getClasspath());
    }

    private void createReplacement(String newGreeting) {
        try {
            new ModifiableDummyTestingHelper()
                    .write(newGreeting)
                    .compile();
        } catch (AbstractSourceTestClass.SourceTestClassWrapperException e) {
            Assertions.fail("Failed to create data to be uploaded.", e);
        }
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

}
