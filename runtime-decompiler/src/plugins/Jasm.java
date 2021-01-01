import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class Jasm {

    public String decompile(byte[] bytecode, String[] options) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        try {
            File file = File.createTempFile("jrd-jasm", "tmp.java");
            file.deleteOnExit();
            Files.write(file.toPath(), bytecode);
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(ps, "jdis");
                jdis.disasm(new String[]{file.getAbsolutePath()});
            }
            String data = baos.toString(utf8);
            if (data.isEmpty()) {
                return "No output, unpatched asmtools? See https://github.com/openjdk/asmtools/pull/13/commits/9104af81fef8c220be919a3e34912386a7b99a60";
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    private void log(Object logger, String message) {
        if (logger == null || message == null) {
            //return;
        }
        try {
            //Currently plugins do not ahve access to parent classloader
            //Method logingMethod = logger.getClass().getMethod("addMessage", java.util.logging.Level.class, String.class);
            //logingMethod.invoke(logger, message);
            System.err.println(message);
        } catch (Exception ex) {
            System.err.println("Jasm have logger, but that logger do not have correct addMessage java.util.logging.Level String method or call failed");
            ex.printStackTrace();
        }
    }

    public Map<String, byte[]> compile(Map<String, String> src, String[] options, Object maybeLogger) throws Exception {
        log(maybeLogger, "jasm compiler caled with input of: " + src.size());
        File parentDir = File.createTempFile("jrd-jasm", "tmp.dir");
        parentDir.delete();
        parentDir.mkdir();
        parentDir.deleteOnExit();
        File srcs = new File(parentDir, "src");
        srcs.mkdir();
        File target = new File(parentDir, "bin");
        target.mkdir();
        log(maybeLogger, "entering into " + parentDir.getAbsolutePath());
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(System.err, "jdis");
        List<String> tmpSources = new ArrayList<>(src.size());
        for (Map.Entry<String, String> fileToCompile : src.entrySet()) {
            File nw = new File(srcs, fileToCompile.getKey() + ".java");
            log(maybeLogger, "writing tmp file into " + nw.getAbsolutePath());
            Files.write(nw.toPath(), fileToCompile.getValue().getBytes());
            tmpSources.add(nw.getAbsolutePath());
        }
        tmpSources.add(0, target.getAbsolutePath());
        tmpSources.add(0, "-d");
        tmpSources.add(0, "-g"); //shoud add debug info
        String[] opts = tmpSources.toArray(new String[0]);
        log(maybeLogger, "jasm " + Arrays.toString(opts));
        jasm.compile(opts);
        Map<String, byte[]> r = new HashMap();
        Files.walk(target.toPath()).filter(Files::isRegularFile).forEach((k) -> {
            try {
                String futureFullyQualifiedNiceName = k.toString();
                futureFullyQualifiedNiceName = futureFullyQualifiedNiceName.replace(target + File.separator, "");
                futureFullyQualifiedNiceName = futureFullyQualifiedNiceName.replace(File.separator, ".");
                futureFullyQualifiedNiceName = futureFullyQualifiedNiceName.replaceAll("\\.class$", "");
                r.put(futureFullyQualifiedNiceName, Files.readAllBytes(k));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        return r;
    }
}
