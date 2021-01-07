import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.util.Map;
import java.io.File;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FernflowerDecompilerWrapper {

    public String decompile(String name, byte[] bytecode, Map<String,byte[]> innerClasses, String[] options){
        return "";
    }

    public String decompile(byte[] bytecode, String[] options){
        File tempByteFile = null;
        File decompiledFile = null;
        String decompiledString;
        try {
            tempByteFile = bytesToFile(bytecode);
            String[] args = new String[]{tempByteFile.getAbsolutePath(), System.getProperty("java.io.tmpdir")};
            ConsoleDecompiler.main(args);
            String decompiledFilePath = tempByteFile.getPath().substring(0, tempByteFile.getPath().length()-6)+ ".java";
            decompiledFile = new File(decompiledFilePath);
            decompiledString = readStringFromFile(decompiledFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            if (tempByteFile != null && tempByteFile.exists()){
                tempByteFile.delete();
            }
            if (decompiledFile != null && decompiledFile.exists()){
                decompiledFile.delete();
            }
            
        }
        return decompiledString;
    }

    private String readStringFromFile(String filePath) throws IOException {
        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
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
}
