import org.benf.cfr.reader.Main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CfrDecompilerWrapper {

    public String classToStub(String s) {
        return s.replace(".", "/") + ".class";
    }

    public File classToFile(File f, String clazz) {
        return new File(f.getAbsolutePath() + "/" + classToStub(clazz));
    }

    public String decompile(
            String name, byte[] bytecode, Map<String, byte[]> innerClasses, String[] options
    ) throws IOException {
        File base = File.createTempFile("crf-" + name, ".class");
        base.delete();
        base.mkdir();
        base.deleteOnExit();
        File out = File.createTempFile("crf-" + name, ".java");
        out.delete();
        out.mkdir();
        out.deleteOnExit();
        File mainFile = bytesToFile(classToFile(base, name), bytecode);
        String[] args = new String[innerClasses.entrySet().size() + 5];
        int i = 0;
        args[i] = mainFile.getAbsolutePath();
        for (Map.Entry<String, byte[]> item: innerClasses.entrySet()) {
            i++;
            File ff = bytesToFile(classToFile(base, item.getKey()), item.getValue());
            args[i] = ff.getAbsolutePath();
        }
        i++;
        args[i] = "--outputpath";
        i++;
        args[i] = out.getAbsolutePath();
        i++;
        args[i] = "--skipbatchinnerclasses";
        i++;
        args[i] = "false";

        File decompiledFile = null;
        String decompiledString;
        try {
            Object[] o = cfr(name, out, args);
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

    public String decompile(byte[] bytecode, String[] options) throws IOException {
        return decompile("unknow.cfr.class" + bytecode.length, bytecode, new HashMap<String, byte[]>(), options);
    }

    private Object[] cfr(String mainFile, File decompiledDir, String... args) throws IOException {
        PrintStream old = System.out;
        System.setOut(System.err);
        try {
            Main.main(args);
            File mainDecompiledFile = getFileFrom(decompiledDir, mainFile);
            String decompiledString = readStringFromFile(mainDecompiledFile);
            return new Object[]{mainDecompiledFile, decompiledString};
        } finally {
            System.setOut(old);
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


    public String decompilerHelp() throws IOException {
        PrintStream oldo = System.out;
        PrintStream olde = System.err;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));
        System.setErr(new PrintStream(bos));
        try {
            Main.main(new String[]{"--help"});
            return bos.toString("utf-8");
        } finally {
            System.setOut(oldo);
            System.setOut(olde);
        }
    }

}
