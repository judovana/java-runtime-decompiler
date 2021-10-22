package org.jrd.backend.data;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

abstract class AbstractSourceTestClass {

    private Process process;
    private final String srcDir;
    private final String targetDir;
    private ProcessStdStreamReade sout;
    private ProcessStdStreamReade serr;

    protected AbstractSourceTestClass() {
        String tmpDir;
        try {
            tmpDir = Files.createDirectories(
                    Path.of(System.getProperty("java.io.tmpdir"), getClassName(), "src", "main", "java", getPackageDirs())
            ).toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            tmpDir = System.getProperty("java.io.tmpdir");
        }
        srcDir = tmpDir;

        try {
            tmpDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), getClassName(), "target")).toAbsolutePath()
                    .toString();
        } catch (IOException e) {
            e.printStackTrace();
            tmpDir = System.getProperty("java.io.tmpdir");
        }
        targetDir = tmpDir;
    }

    abstract String getClassName();

    abstract String getPackageName();

    abstract String getGreetings();

    abstract String getContentWithoutPackage(String nwHello);

    public String getSrcDir() {
        return srcDir;
    }

    public String getTargetDir() {
        return targetDir;
    }

    protected String getPackageDirs() {
        return getPackageName().replace('.', File.separatorChar);
    }

    protected String getFqn() {
        return getPackageName() + "." + getClassName();
    }

    String getDotJavaPath() {
        return getSrcDir() + File.separator + getClassName() + ".java";
    }

    String getDotClassPath() {
        return getTargetDir() + File.separator + getPackageDirs() + File.separator + getClassName() + ".class";
    }

    String getClassRegex() {
        return ".*" + getClassName() + ".*";
    }

    Pattern getJvmListRegex() {
        return Pattern.compile(String.format("^(\\d+).*%s.*$", getClassName()), Pattern.MULTILINE);
    }

    Pattern getExactClassRegex() {
        return Pattern.compile(String.format("^%s$", getFqn()), Pattern.MULTILINE);
    }

    AbstractSourceTestClass writeDefault() throws SourceTestClassWrapperException {
        return write(getGreetings());
    }

    AbstractSourceTestClass write(String argument) throws SourceTestClassWrapperException {
        try (PrintWriter pw = new PrintWriter(getDotJavaPath(), StandardCharsets.UTF_8)) {
            pw.print(getContentWithPackage(argument));
        } catch (IOException e) {
            throw new SourceTestClassWrapperException("Failed to write file '" + getDotJavaPath() + "'.", e);
        }

        return this;
    }

    AbstractSourceTestClass compile() throws SourceTestClassWrapperException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        int errLevel = compiler.run(null, null, errStream, "-d", getTargetDir(), getDotJavaPath());
        String errMessage = errStream.toString(StandardCharsets.UTF_8);

        if (errLevel != 0 || !errMessage.isEmpty()) {
            throw new SourceTestClassWrapperException("Failed to compile file '" + getDotJavaPath() + ". Cause:\n" + errMessage);
        }

        return this;
    }

    AbstractSourceTestClass execute() throws SourceTestClassWrapperException {
        ProcessBuilder pb = new ProcessBuilder("java", "-Djdk.attach.allowAttachSelf=true", getFqn());
        pb.directory(new File(getTargetDir()));
        try {
            process = pb.start();
            sout = new ProcessStdStreamReade(process.getInputStream());
            serr = new ProcessStdStreamReade(process.getErrorStream());
            new Thread(sout).start();
            new Thread(serr).start();
        } catch (IOException e) {
            throw new SourceTestClassWrapperException("Failed to execute '" + getFqn() + "' class.", e);
        }

        return this;
    }

    String executeJavaP(String... options) throws SourceTestClassWrapperException, InterruptedException, IOException {
        List<String> commands = new ArrayList<>();
        commands.add("javap");
        commands.addAll(Arrays.asList(options));
        commands.add(getFqn());

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true); // merge process stderr with stdout
        pb.directory(new File(getTargetDir()));

        Process javap;
        try {
            javap = pb.start();
        } catch (IOException e) {
            throw new SourceTestClassWrapperException("Failed to execute javap on '" + getFqn() + "' class.", e);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(javap.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.getProperty("line.separator", "\n"));
            }
        }

        javap.waitFor();
        return sb.toString();
    }

    void terminate() {
        process.destroy();
    }

    boolean isAlive() {
        return process.isAlive();
    }

    public String getPid() {
        return Long.toString(process.pid());
    }

    public String getClasspath() {
        return getTargetDir();
    }

    String getDefaultContentWithoutPackage() {
        return getContentWithoutPackage(getGreetings());
    }

    String getDefaultContentWithPackage() {
        return getContentWithPackage(getGreetings());
    }

    String getContentWithPackage(String nwHello) {
        return "package " + getPackageName() + ";\n\n" + getContentWithoutPackage(nwHello);
    }

    public String getEmptyClassWithPackage() {
        return getEmptyClassWithPackage(getPackageName(), getClassName());
    }

    public static String getEmptyClassWithPackage(String pkg, String name) {
        return "package " + pkg + ";\n" + "public class " + name + " {}";
    }

    public byte[] getErrBytes() {
        return serr.getBuffer();
    }

    public byte[] getOutBytes() {
        return sout.getBuffer();
    }

    public String getErrString() {
        return new String(serr.getBuffer(), StandardCharsets.UTF_8);
    }

    public String getOutString() {
        return new String(sout.getBuffer(), StandardCharsets.UTF_8);
    }

    static class SourceTestClassWrapperException extends Exception {
        SourceTestClassWrapperException(String message) {
            super(message);
        }

        SourceTestClassWrapperException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ProcessStdStreamReade implements Runnable {
        private final InputStream is;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean read = false;

        ProcessStdStreamReade(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try {
                int nRead;
                byte[] data = new byte[8]; //slow, but we need quick updates
                while ((nRead = is.readNBytes(data, 0, data.length)) != 0) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
            } catch (Exception ex) {
                //ex.printStackTrace(); //yah, the process is being killed
            }
            read = true;
        }

        public byte[] getBuffer() {
            return buffer.toByteArray();
        }

        public boolean isRead() {
            return read;
        }
    }
}
