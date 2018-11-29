package org.jrd.backend.decompiling;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
public class JavapDisassemblerWrapper {

    public String decompile(byte[] bytecode, String[] options){
        try {
            File tempByteFile = bytesToFile(bytecode);
            File tempOutputFile = File.createTempFile("decompile-output", ".java");
            PrintWriter printWriter = new PrintWriter(tempOutputFile);
            StringBuilder OptionsString = new StringBuilder();
            if (options != null){
                for (String option: options){
                    OptionsString.append(option);
                }
            }
            com.sun.tools.javap.Main.run(new String[]{OptionsString.toString(), tempByteFile.getAbsolutePath()}, printWriter);
            return readStringFromFile(tempOutputFile.getAbsolutePath());
        } catch (Exception e){
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            return "Exception while decompiling" + errors.toString();
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

    private String readStringFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}
