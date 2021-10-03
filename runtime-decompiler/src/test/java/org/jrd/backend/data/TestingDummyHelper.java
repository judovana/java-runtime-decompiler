package org.jrd.backend.data;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class TestingDummyHelper {

    private Process process;

    static final String CLASS_NAME = "TestingDummy";
    static final String PACKAGE_NAME = "testing.dummy";
    static final String PACKAGE_DIRS = PACKAGE_NAME.replace('.', File.separatorChar);
    static final String FQN = PACKAGE_NAME + "." + CLASS_NAME;

    static final String SRC_DIR;
    static final String TARGET_DIR;

    static {
        String tmpDir;
        try {
            tmpDir = Files.createDirectories(Path.of(
                            System.getProperty("java.io.tmpdir"), CLASS_NAME,
                            "src", "main", "java", PACKAGE_DIRS
                    )).toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            tmpDir = System.getProperty("java.io.tmpdir");
        }
        SRC_DIR = tmpDir;

        try {
            tmpDir = Files.createDirectories(Path.of(
                    System.getProperty("java.io.tmpdir"), CLASS_NAME, "target"
            )).toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            tmpDir = System.getProperty("java.io.tmpdir");
        }
        TARGET_DIR = tmpDir;
    }

    static final String DOT_JAVA_PATH = SRC_DIR + File.separator + CLASS_NAME + ".java";
    static final String DOT_CLASS_PATH = TARGET_DIR + File.separator + PACKAGE_DIRS + File.separator + CLASS_NAME + ".class";

    static final String CLASS_REGEX = ".*" + CLASS_NAME + ".*";
    static final Pattern JVM_LIST_REGEX = Pattern.compile(
            String.format("^(\\d+).*%s.*$", CLASS_NAME),
            Pattern.MULTILINE
    );
    static final Pattern EXACT_CLASS_REGEX = Pattern.compile(
            String.format("^%s$", FQN),
            Pattern.MULTILINE
    );

    static final String DEFAULT_GREETING = "Hello";


    TestingDummyHelper write(String greeting) throws TestingDummyException {
        try (PrintWriter pw = new PrintWriter(DOT_JAVA_PATH, StandardCharsets.UTF_8)) {
            pw.print(getContentWithPackage(greeting));
        } catch (IOException e) {
            throw new TestingDummyException("Failed to write file '" + DOT_JAVA_PATH + "'.", e);
        }

        return this;
    }

    TestingDummyHelper write() throws TestingDummyException {
        return write("Hello");
    }

    TestingDummyHelper compile() throws TestingDummyException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        int errLevel = compiler.run(null, null, errStream,
                "-d", TARGET_DIR,
                DOT_JAVA_PATH
        );
        String errMessage = errStream.toString(StandardCharsets.UTF_8);

        if (errLevel != 0 || !errMessage.isEmpty()) {
            throw new TestingDummyException("Failed to compile file '" + DOT_JAVA_PATH + ". Cause:\n" + errMessage);
        }

        return this;
    }

    TestingDummyHelper execute() throws TestingDummyException {
        ProcessBuilder pb = new ProcessBuilder("java", "-Djdk.attach.allowAttachSelf=true", FQN);
        pb.directory(new File(TARGET_DIR));

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new TestingDummyException("Failed to execute '" + FQN + "' class.", e);
        }

        return this;
    }

    static String executeJavaP(String... options) throws TestingDummyException, InterruptedException, IOException {
        List<String> commands = new ArrayList<>();
        commands.add("javap");
        commands.addAll(Arrays.asList(options));
        commands.add(FQN);

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(TARGET_DIR));

        Process javap;
        try {
            javap = pb.start();
        } catch (IOException e) {
            throw new TestingDummyException("Failed to execute javap on '" + FQN + "' class.", e);
        }
        javap.waitFor();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                javap.getInputStream(), StandardCharsets.UTF_8
        ))) {
            String output = br.lines().collect(Collectors.joining("\n"));
            javap.destroy();
            return output;
        }
    }

    void terminate() {
        process.destroy();
    }

    boolean isAlive() {
        return process.isAlive();
    }

    String getPid() {
        return Long.toString(process.pid());
    }

    static String getDefaultContent() {
        return getContent(DEFAULT_GREETING);
    }

    static String getContent(String greeting) {
        return "public class " + CLASS_NAME + " {\n" +
                "    public static void main(String[] args) throws InterruptedException {\n" +
                "        while(true) {\n" +
                "            System.out.println(\"" + greeting + "\");\n" +
                "            Thread.sleep(1000);\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    static String getContentWithPackage(String greeting) {
        return "package " + PACKAGE_NAME + ";\n\n" +
                getContent(greeting);
    }

    static String getDefaultContentWithPackage() {
        return "package " + PACKAGE_NAME + ";\n\n" +
                getDefaultContent();
    }

    static String getEmptyClass() {
        return "package " + PACKAGE_NAME + ";\n" +
                "public class " + CLASS_NAME + " {}";
    }

    static class TestingDummyException extends Exception {
        TestingDummyException(String message) {
            super(message);
        }

        TestingDummyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
