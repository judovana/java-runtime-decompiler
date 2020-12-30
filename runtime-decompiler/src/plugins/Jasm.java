import org.openjdk.asmtools.jdis.Main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Jasm {

    public String decompile(byte[] bytecode, String[] options) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        try {
            File file = File.createTempFile("jrd-jasm", "tmp.class");
            file.deleteOnExit();
            Files.write(file.toPath(), bytecode);
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                Main jdis = new Main(ps, "jdis");
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
}
