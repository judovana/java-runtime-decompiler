import decompiler.core;

import java.util.Arrays;
import java.util.Map;
import java.io.File;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.PrintStream;
import java.util.stream.Stream;


public class ClojureDecompilerWrapper {

    public String classToStub(String s) {
        return s.replace(".", "/") + ".class";
    }

    public File classToFile(File f, String clazz) {
        return new File(f.getAbsolutePath() + "/" + classToStub(clazz));
    }

    /**
     * At the end of this fun was found, that fernflower, correctly, ignores naming, so all the fun with saving in file tree may be hapily abandoned
     * and just bunch of tmp files can be used. Final file is always given_dir/Cazz.java , where class is not fully qalified nor placed in pkg dirs
     **/
    public String decompile(String name, byte[] bytecode, Map<String, byte[]> innerClasses, String[] options) throws IOException {
        File base = File.createTempFile("jrd-clojure-" + name, "");
        base.delete();
        base.mkdir();
        base.deleteOnExit();
        File output = File.createTempFile("jrd-clojure-" + name + "-output", "");
        output.delete();
        output.mkdir();
        output.deleteOnExit();
        File mainFile = bytesToFile(classToFile(base, name), bytecode);
        String[] args = new String[innerClasses.entrySet().size() + 3];
        int i = 0;
        args[i] = "-o";
        i++;
        args[i] = output.getAbsolutePath();
        i++;
        args[i] = mainFile.getAbsolutePath();
        for (Map.Entry<String, byte[]> item : innerClasses.entrySet()) {
            File ff = bytesToFile(classToFile(base, item.getKey()), item.getValue());
            i++;
            args[i] = ff.getAbsolutePath();
        }
        File decompiledFile = null;
        final StringBuilder decompiledString = new StringBuilder(Arrays.toString(args));
        try {
            clojure(args);
            decompiledFile = output;
            decompiledString.setLength(0);
            try (Stream<Path> paths = Files.walk(output.toPath())) {
                paths.filter(Files::isRegularFile).forEach(info -> {
                    decompiledString.append(readStringFromFile(info.toFile().getAbsolutePath())).append("\n");
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            return e.toString()+"\n"+decompiledString.toString();
        } finally {
            if (mainFile != null && mainFile.exists()) {
                mainFile.delete();
            }
            if (decompiledFile != null && decompiledFile.exists()) {
                decompiledFile.delete();
            }

        }
        return decompiledString.toString();

    }

    public String decompile(byte[] bytecode, String[] options) {
        throw new RuntimeException("Clojure decompiler can not work witout nested classes support");
    }

    private void clojure(String... args) throws IOException {
        PrintStream old = System.out;
        System.setOut(System.err);
        try {
            System.err.println("decompiler.core.main " + Arrays.toString(args));
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            decompiler.core.main(args);
        } finally {
            System.setOut(old);
        }
    }

    private String readStringFromFile(String filePath) {
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
            return fileContent;
        } catch(IOException ex){
            throw new RuntimeException(ex);
        }
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

    public Map<String, byte[]> compile(Map<String, String> src, String[] options, Object maybeLogger) throws Exception {
        throw new RuntimeException("Clojure can not be compiled right now and support is not planned");
    }
}
