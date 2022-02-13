import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.DecompilerDriver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
        System.setOut(System.err);
        File f = File.createTempFile("procyon","jrd.out");
        f.delete();
        f.mkdir();
        try {
            String[] files = new String[innerClasses.size() + 3];
            files[0] = bytesToFile(bytecode).getAbsolutePath();
            files[1] = "-o";
            files[2] = f.getAbsolutePath();
            int i = 3;
            for (byte[] inner : innerClasses.values()) {
                files[i] = bytesToFile(inner).getAbsolutePath();
                i++;
            }
            DecompilerDriver.main(files);
            return readStringFromFile(getFileFrom(f,name));
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            System.setOut(so);
        }
    }

    private File getFileFrom(File dir, String fqn) {
        return new File(dir.getAbsolutePath() + File.separator + fqn.replace('.', File.separatorChar)+".java");
    }

    private String readStringFromFile(File filePath) throws IOException {
        String fileContent = new String(Files.readAllBytes(filePath.toPath()));
        return fileContent;
    }

    private File bytesToFile(byte[] bytes) throws IOException {
        File tempFile = File.createTempFile("temporary-byte-file", ".class");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile.getCanonicalPath());
        fos.write(bytes);
        fos.close();
        return tempFile;
    }

    public String decompilerHelp() throws Exception {
        PrintStream oldo = System.out;
        PrintStream olde = System.err;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Object consoleWriterBackup = null;
        Field consoleWriter = null;
        if (System.console() != null) {
            //unluckily, procyon is using jcommander's console, whcich is USUALLY System.console...
            try {
                consoleWriter = System.console().getClass().getDeclaredField("pw");
                consoleWriter.setAccessible(true);
                consoleWriterBackup = consoleWriter.get(System.console());
                consoleWriter.set(System.console(), new PrintWriter(bos));
            } catch (Throwable ex) {
                if (false) {
                    ex.printStackTrace();
                }
            }
        }
        System.setOut(new PrintStream(bos));
        System.setErr(new PrintStream(bos));
        try {
            DecompilerDriver.main(new String[]{"--help"});
            if (System.console() != null) {
                System.console().writer().flush();
            }
            return bos.toString("utf-8");
        } finally {
            System.setOut(oldo);
            System.setOut(olde);
            if (consoleWriterBackup != null && consoleWriter != null) {
                consoleWriter.set(System.console(), consoleWriterBackup);
            }
        }
    }
}
