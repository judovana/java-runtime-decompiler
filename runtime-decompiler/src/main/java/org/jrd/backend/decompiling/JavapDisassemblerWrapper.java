package org.jrd.backend.decompiling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JavapDisassemblerWrapper {

    private final String otherArgs;

    public JavapDisassemblerWrapper(String otherArgs) {
        this.otherArgs = otherArgs;
    }

    public String decompile(byte[] bytecode, String[] options) {
        try {
            File tempByteFile = bytesToFile(bytecode);
            File tempOutputFile = File.createTempFile("decompile-output", ".java");
            PrintWriter printWriter = new PrintWriter(tempOutputFile, StandardCharsets.UTF_8);
            StringBuilder optionsString = new StringBuilder();
            if (options != null) {
                for (String option: options) {
                    optionsString.append(option);
                }
            }

            com.sun.tools.javap.Main.run(
                    new String[]{otherArgs + optionsString, tempByteFile.getAbsolutePath()}, printWriter
            );

            return readStringFromFile(tempOutputFile.getAbsolutePath());
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            return "Exception while decompiling" + errors;
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
