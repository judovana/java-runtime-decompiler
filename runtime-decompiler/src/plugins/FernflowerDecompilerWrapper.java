import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class FernflowerDecompilerWrapper {

    public String classToStub(String s) {
        return s.replace(".", "/") + ".class";
    }

    public File classToFile(File f, String clazz) {
        return new File(f.getAbsolutePath() + "/" + classToStub(clazz));
    }

    /*
    * At the end of this fun was found, that fernflower, correctly, ignores naming, so all the fun with saving in file tree may be hapily abandoned
    * and just bunch of tmp files can be used. Final file is always given_dir/Cazz.java , where class is not fully qalified nor placed in pkg dirs
    */
    public String decompile(
            String name, byte[] bytecode, Map<String, byte[]> innerClasses, String[] options
    ) throws IOException {
        File base = File.createTempFile("fernflower-" + name, ".class");
        base.delete();
        base.mkdir();
        base.deleteOnExit();
        File mainFile = bytesToFile(classToFile(base, name), bytecode);
        String[] args = new String[innerClasses.entrySet().size() + 2];
        int i = 0;
        args[i] = mainFile.getAbsolutePath();
        for (Map.Entry<String, byte[]> item: innerClasses.entrySet()) {
            File ff = bytesToFile(classToFile(base, item.getKey()), item.getValue());
            i++;
            args[i] = ff.getAbsolutePath();
        }
        i++;
        args[i] = base.getAbsolutePath();
        File decompiledFile = null;
        String decompiledString;
        try {
            Object[] o = fernflower(base.getAbsolutePath() + "/" + mainFile.getName().replace(".class", ".java"), args);
            decompiledFile = (File) o[0];
            decompiledString = (String) o[1];
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            if (mainFile != null && mainFile.exists()) {
                mainFile.delete();
            }
            if (decompiledFile != null && decompiledFile.exists()) {
                decompiledFile.delete();
            }
        }

        return decompiledString;
    }

    public String decompile(byte[] bytecode, String[] options) {
        File tempByteFile = null;
        File decompiledFile = null;
        String decompiledString;
        try {
            tempByteFile = bytesToFile(bytecode);
            String[] args = new String[]{tempByteFile.getAbsolutePath(), System.getProperty("java.io.tmpdir")};
            Object[] o = fernflower(
                    tempByteFile.getPath().substring(0, tempByteFile.getPath().length() - 6) + ".java", args
            );
            decompiledFile = (File) o[0];
            decompiledString = (String) o[1];
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            if (tempByteFile != null && tempByteFile.exists()) {
                tempByteFile.delete();
            }
            if (decompiledFile != null && decompiledFile.exists()) {
                decompiledFile.delete();
            }
        }

        return decompiledString;
    }

    private Object[] fernflower(String decompiledFilePath, String... args) throws IOException {
        PrintStream old = System.out;
        System.setOut(System.err);
        try {
            ConsoleDecompiler.main(args);
            File decompiledFile = new File(decompiledFilePath);
            String decompiledString = readStringFromFile(decompiledFilePath);
            return new Object[]{decompiledFile, decompiledString};
        } finally {
            System.setOut(old);
        }
    }

    private String readStringFromFile(String filePath) throws IOException {
        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
        return fileContent;
    }

    private File bytesToFile(byte[] bytes) throws IOException {
        File tempFile = File.createTempFile("temporary-byte-file", ".class");
        tempFile.deleteOnExit();
        return bytesToFile(tempFile, bytes);
    }

    private File bytesToFile(File tempFile, byte[] bytes) throws IOException {
        tempFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(tempFile.getCanonicalPath());
        fos.write(bytes);
        fos.flush();
        fos.close();
        return tempFile;
    }
}
