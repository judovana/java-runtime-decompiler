import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;

/**
 * Fully based on: https://github.com/java-decompiler/jd-core/blob/master/README.md
 **/
public class JdDecompilerWrapper {

    private static boolean debug = false;
    private static boolean allow = true; //this is interesting feature of JD, to  read (or not) the JDK's classes

    private static void log(String s) {
        if (debug) {
            System.err.println(s);
        }
    }

    //JD calls internally itself with / instead of .
    private static String jditize(String s) {
        return s.replace(".", "/");
    }

    public static String run(String name, byte[] bytecode, Map<String, byte[]> innerClasses) throws Exception {
        Loader loader = new Loader() {
            @Override
            public byte[] load(String internalName) throws LoaderException {
                internalName = jditize(internalName);
                log("Trying: " + internalName);
                if (jditize(name).equals(internalName)) {
                    log(" found as main");
                    return bytecode;
                }
                for (String clazz : innerClasses.keySet()) {
                    if (jditize(clazz).equals(internalName)) {
                        log(" found as secondary");
                        return innerClasses.get(clazz);
                    }
                }
                if (!allow) {
                    return null;
                }
                InputStream is = this.getClass().getResourceAsStream("/" + internalName + ".class");
                if (is == null) {
                    log(" not found");
                    return null;
                } else {
                    try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int read = in.read(buffer);

                        while (read > 0) {
                            out.write(buffer, 0, read);
                            read = in.read(buffer);
                        }
                        log(" found in garbage");
                        return out.toByteArray();
                    } catch (IOException e) {
                        throw new LoaderException(e);
                    }
                }
            }

            @Override
            public boolean canLoad(String internalName) {
                return this.getClass().getResource("/" + internalName + ".class") != null;
            }
        };
        int r = 0;
        Printer printer = new Printer() {
            protected static final String TAB = "  ";
            protected static final String NEWLINE = "\n";

            protected int indentationCount = 0;
            protected StringBuilder sb = new StringBuilder();

            @Override
            public String toString() {
                return sb.toString();
            }

            @Override
            public void start(int maxLineNumber, int majorVersion, int minorVersion) {
            }

            @Override
            public void end() {
            }

            @Override
            public void printText(String text) {
                sb.append(text);
            }

            @Override
            public void printNumericConstant(String constant) {
                sb.append(constant);
            }

            @Override
            public void printStringConstant(String constant, String ownerInternalName) {
                sb.append(constant);
            }

            @Override
            public void printKeyword(String keyword) {
                sb.append(keyword);
            }

            @Override
            public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
                sb.append(name);
            }

            @Override
            public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
                sb.append(name);
            }

            @Override
            public void indent() {
                this.indentationCount++;
            }

            @Override
            public void unindent() {
                this.indentationCount--;
            }

            @Override
            public void startLine(int lineNumber) {
                for (int i = 0; i < indentationCount; i++) sb.append(TAB);
            }

            @Override
            public void endLine() {
                sb.append(NEWLINE);
            }

            @Override
            public void extraLine(int count) {
                while (count-- > 0) sb.append(NEWLINE);
            }

            @Override
            public void startMarker(int type) {
            }

            @Override
            public void endMarker(int type) {
            }
        };
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        decompiler.decompile(loader, printer, name);
        String source = printer.toString();
        return (source);
    }

    public String decompile(
            String name, byte[] bytecode, Map<String, byte[]> innerClasses, String[] options
    ) throws Exception {
        return run(name, bytecode, innerClasses);
    }

    public String decompile(byte[] bytecode, String[] options) throws Exception {
        return decompile("unknow.cfr.class" + bytecode.length, bytecode, new HashMap<>(), options);
    }


}
