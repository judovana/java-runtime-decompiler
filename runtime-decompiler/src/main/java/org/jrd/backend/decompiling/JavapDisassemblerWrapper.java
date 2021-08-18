package org.jrd.backend.decompiling;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
public class JavapDisassemblerWrapper {

    private final String otherArgs;

    public JavapDisassemblerWrapper(String otherArgs){
        this.otherArgs=otherArgs;
    }

    public String decompile(byte[] bytecode, String[] options){
        try {
            File tempByteFile = bytesToFile(bytecode);
            File tempOutputFile = File.createTempFile("decompile-output", ".java");
            PrintWriter printWriter = new PrintWriter(tempOutputFile, StandardCharsets.UTF_8);
            StringBuilder OptionsString = new StringBuilder();
            if (options != null){
                for (String option: options){
                    OptionsString.append(option);
                }
            }
            com.sun.tools.javap.Main.run(new String[]{otherArgs+OptionsString.toString(), tempByteFile.getAbsolutePath()}, printWriter);
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

        try (FileOutputStream fos = new FileOutputStream(tempFile.getCanonicalPath())) {
            fos.write(bytes);
        }

        return tempFile;
    }

    private String readStringFromFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
}
