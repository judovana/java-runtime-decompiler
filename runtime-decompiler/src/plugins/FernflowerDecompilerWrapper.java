import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
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

    public String decompilerHelp() throws IOException {
        PrintStream oldo = System.out;
        PrintStream olde = System.err;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));
        System.setErr(new PrintStream(bos));
        try {
            ConsoleDecompiler.main((new String[]{"--help"}));
            System.out.println("");
            System.out.println("See detailed options from remote content: ");
            try {
                URL u = new URL("https://raw.githubusercontent.com/JetBrains/intellij-community/master/plugins/java-decompiler/engine/README.md");
                bos.writeBytes(u.openStream().readAllBytes());
            }catch(Exception ex){
                System.out.println("fetching remote content failed:");
                System.out.println(ex.toString());
            }
            System.out.println("");
            System.out.println("Hardcoded copy:");
            System.out.println("-rbr (1): hide bridge methods\n" +
                    "-rsy (0): hide synthetic class members\n" +
                    "-din (1): decompile inner classes\n" +
                    "-dc4 (1): collapse 1.4 class references\n" +
                    "-das (1): decompile assertions\n" +
                    "-hes (1): hide empty super invocation\n" +
                    "-hdc (1): hide empty default constructor\n" +
                    "-dgs (0): decompile generic signatures\n" +
                    "-ner (1): assume return not throwing exceptions\n" +
                    "-den (1): decompile enumerations\n" +
                    "-rgn (1): remove getClass() invocation, when it is part of a qualified new statement\n" +
                    "-lit (0): output numeric literals \"as-is\"\n" +
                    "-asc (0): encode non-ASCII characters in string and character literals as Unicode escapes\n" +
                    "-bto (1): interpret int 1 as boolean true (workaround to a compiler bug)\n" +
                    "-nns (0): allow for not set synthetic attribute (workaround to a compiler bug)\n" +
                    "-uto (1): consider nameless types as java.lang.Object (workaround to a compiler architecture flaw)\n" +
                    "-udv (1): reconstruct variable names from debug information, if present\n" +
                    "-ump (1): reconstruct parameter names from corresponding attributes, if present\n" +
                    "-rer (1): remove empty exception ranges\n" +
                    "-fdi (1): de-inline finally structures\n" +
                    "-mpm (0): maximum allowed processing time per decompiled method, in seconds. 0 means no upper limit\n" +
                    "-ren (0): rename ambiguous (resp. obfuscated) classes and class elements\n" +
                    "-urc (-): full name of a user-supplied class implementing IIdentifierRenamer interface. It is used to determine which class identifiers\n" +
                    "          should be renamed and provides new identifier names (see \"Renaming identifiers\")\n" +
                    "-inn (1): check for IntelliJ IDEA-specific @NotNull annotation and remove inserted code if found\n" +
                    "-lac (0): decompile lambda expressions to anonymous classes\n" +
                    "-nls (0): define new line character to be used for output. 0 - '\\r\\n' (Windows), 1 - '\\n' (Unix), default is OS-dependent\n" +
                    "-ind: indentation string (default is 3 spaces)\n" +
                    "-log (INFO): a logging level, possible values are TRACE, INFO, WARN, ERROR");
            return bos.toString("utf-8");
        } finally {
            System.setOut(oldo);
            System.setOut(olde);
        }
    }
}
