package org.jrd.backend.data;

import static org.jrd.backend.data.Cli.BASE64;
import static org.jrd.backend.data.Cli.BYTES;
import static org.jrd.backend.data.Cli.DECOMPILE;
import static org.jrd.backend.data.Cli.OVERWRITE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    void testBytes(String pucComponent) throws Exception {
        args = new String[]{BYTES, pucComponent, dummy.getClassRegex()};
        cli = new Cli(args, model);

        cli.consumeCli();

        byte[] bytes = streams.getOutBytes();
        byte[] fileContents = Files.readAllBytes(Path.of(dummy.getDotClassPath()));

        assertArrayEquals(fileContents, bytes);
    }

    @Override
    AbstractSourceTestClass dummyProvider() throws AbstractSourceTestClass.SourceTestClassWrapperException {
        return new ModifiableDummyTestingHelper()
                .writeDefault()
                .compile()
                .execute();
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


    void testDecompileJavap(String pucComponent, String option) throws Exception {
        args = new String[]{DECOMPILE, pucComponent, "javap" + option, dummy.getClassRegex()};
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
                OVERWRITE,
                pucComponent,
                dummy.getFqn(),
                dummy.getDotClassPath() // contains newGreeting because of try-catch above
        };
        cli = new Cli(args, model);

        assertDoesNotThrow(() -> cli.consumeCli());
        assertTrue(streams.getOut().contains("success"));

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
        bytecodeContainsNewString(pucComponent, newGreeting);

        System.setIn(originalIn); // revert input stream
    }

    /**
     * Why this have oposite definition then its soulmate in CliTest:
     *
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
            fail("Failed to create data to be uploaded.", e);
        }
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
        try {k
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

}