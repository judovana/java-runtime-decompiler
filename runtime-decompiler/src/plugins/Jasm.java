import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

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
                return "No output, maybe inner class?";
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public Map<String,byte[]> compile(Map<String,String> src, String[] options) throws Exception {
        File file = File.createTempFile("jrd-jasm", "tmp.dir");
        file.delete();
        file.mkdir();
        file.deleteOnExit();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(System.out, "jdis");
        jasm.compile(new String[]{"-g", "-d",file.getAbsolutePath()});
        Map<String,byte[]> r = new HashMap();
        Files.walk(file.toPath()).filter(Files::isRegularFile).forEach((k) -> {
                try {
                    String futureFullyQualifiedNiceName = k.toString();
                    r.put(futureFullyQualifiedNiceName, Files.readAllBytes(k));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        return r;
    }
}
