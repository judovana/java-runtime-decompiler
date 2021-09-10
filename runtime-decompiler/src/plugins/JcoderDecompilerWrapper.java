import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JcoderDecompilerWrapper {

    public String decompile(byte[] bytecode, String[] options) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        try {
            File file = File.createTempFile("jrd-jcoder", "tmp.java");
            file.deleteOnExit();
            Files.write(file.toPath(), bytecode);
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(ps, "jdec");
                jdec.decode(new String[]{"-g", file.getAbsolutePath()});
            }
            String data = baos.toString(utf8);
            if (data.isEmpty()) {
                return "No output, unpatched asmtools? See " +
                        "https://github.com/openjdk/asmtools/pull/13/commits/6f8e5b532aa0cdb032ede0854de30da16cf2bb5c";
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    private void log(Object logger, String message) {
        System.err.println(message);
    }

    public Map<String, byte[]> compile(Map<String, String> src, String[] options, Object maybeLogger) throws Exception {
        log(maybeLogger, "jcoder compiler caled with input of: " + src.size());
        File parentDir = File.createTempFile("jrd-jcoder", "tmp.dir");
        parentDir.delete();
        parentDir.mkdir();
        parentDir.deleteOnExit();
        File srcs = new File(parentDir, "src");
        srcs.mkdir();
        File target = new File(parentDir, "bin");
        target.mkdir();
        log(maybeLogger, "entering into " + parentDir.getAbsolutePath());
        List<String> tmpSources = new ArrayList<>(src.size());
        for (Map.Entry<String, String> fileToCompile : src.entrySet()) {
            File nw = new File(srcs, fileToCompile.getKey() + ".java");
            log(maybeLogger, "writing tmp file into " + nw.getAbsolutePath());
            Files.write(nw.toPath(), fileToCompile.getValue().getBytes());
            tmpSources.add(nw.getAbsolutePath());
        }
        tmpSources.add(0, target.getAbsolutePath());
        tmpSources.add(0, "-d");
        String[] opts = tmpSources.toArray(new String[0]);
        log(maybeLogger, "jcoder " + Arrays.toString(opts));

        org.openjdk.asmtools.jcoder.Main jcoder = new org.openjdk.asmtools.jcoder.Main(System.err, "jcoder");
        jcoder.compile(opts);
        Map<String, byte[]> r = new HashMap();
        Files.walk(target.toPath()).filter(Files::isRegularFile).forEach(k -> {
            try {
                String futureFullyQualifiedNiceName = k
                        .toString()
                        .replace(target + File.separator, "")
                        .replace(File.separator, ".")
                        .replaceAll("\\.class$", "");
                r.put(futureFullyQualifiedNiceName, Files.readAllBytes(k));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        return r;
    }
}
