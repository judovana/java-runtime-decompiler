import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

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

    private File bytesToFile(byte[] bytes) throws IOException {
        File tempFile = File.createTempFile("temporary-byte-file", ".class");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile.getCanonicalPath());
        fos.write(bytes);
        fos.close();
        return tempFile;
    }
}
