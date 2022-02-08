import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.DecompilerDriver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;

public class ProcyonDecompilerWrapper {

    private StringWriter writer = new StringWriter();

    public String decompile(byte[] bytecode, String[] options) {
        File tempByteFile = null;
        writer.getBuffer().delete(0, writer.getBuffer().length());
        try {
            tempByteFile = bytesToFile(bytecode);
            Decompiler.decompile(tempByteFile.getAbsolutePath(), new PlainTextOutput(writer));
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            if (tempByteFile != null && tempByteFile.exists()) {
                tempByteFile.delete();
            }
        }
        return writer.toString();
    }

    public String decompile(
            String name, byte[] bytecode, Map<String, byte[]> innerClasses, String[] options
    ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream so = System.out;
        System.setOut(new PrintStream(bos));
        try {
            String[] files = new String[innerClasses.size() + 1];
            files[0] = bytesToFile(bytecode).getAbsolutePath();
            int i = 1;
            for (byte[] inner : innerClasses.values()) {
                files[i] = bytesToFile(inner).getAbsolutePath();
                i++;
            }
            DecompilerDriver.main(files);
            return bos.toString(Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            System.setOut(so);
        }
    }

    private File bytesToFile(byte[] bytes) throws IOException {
        File tempFile = File.createTempFile("temporary-byte-file", ".class");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile.getCanonicalPath());
        fos.write(bytes);
        fos.close();
        return tempFile;
    }

    public String decompilerHelp() throws IOException {
        PrintStream oldo = System.out;
        PrintStream olde = System.err;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));
        System.setErr(new PrintStream(bos));
        try {
            DecompilerDriver.main(new String[]{"--help"});
            return bos.toString("utf-8");
        } finally {
            System.setOut(oldo);
            System.setOut(olde);
        }
    }
}
