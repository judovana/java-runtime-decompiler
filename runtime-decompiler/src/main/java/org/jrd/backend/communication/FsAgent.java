package org.jrd.backend.communication;


import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.ArchiveManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * This class is doing agent-like based operations on filesystem
 */
public class FsAgent implements JrdAgent {

    private final List<File> cp;

    public FsAgent(List<File> cp) {
        this.cp = cp;
    }

    /**
     * Opens a socket and sends the request to the agent via socket.
     *
     * @param request either "CLASSES" or "BYTES \n className", other formats
     *                are refused
     * @return agents response or null
     */
    @Override
    public String submitRequest(final String request) {
        String[] q = request.split("\\s+");
        try {
            switch (q[0]) {
                case "CLASSES":
                    return readClasses();
                case "BYTES":
                    return sendByteCode(request);
                case "OVERWRITE":
                    uploadByteCode(request);
                    return "OK";
                case "HALT":
                    return "OK";
                default:
                    throw new RuntimeException("Unknown command: " + q[0]);
            }
        } catch (Exception ex) {
            OutputController.getLogger().log(ex);
            return "ERROR";
        }
    }

    private Void uploadByteCode(String request) {
        String[] clazz = request.split("\\s+");
        try {
            return new OperateOnCp<Void>(cp).operatetOnCp(clazz[1], new WriteingCpOperator(clazz[2]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendByteCode(String request) {
        String[] clazz = request.split("\\s+");
        try {
            String s = new OperateOnCp<String>(cp).operatetOnCp(clazz[1], new ReadingCpOperator());
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readClasses() throws IOException {
        List<String> classes = new ArrayList<>();
        new OperateOnCp<Void>(cp).operatetOnCp(null, new ListingCpOperator(classes));
        return classes.stream().collect(Collectors.joining(";"));
    }

    private interface CpOperator<T> {
        T onDirEntry(File dir, File clazz, String fqn) throws IOException;

        T onJarEntry(File file, ZipFile zipFile, ZipEntry ze, String fqn) throws IOException;

        T finalizirung();
    }

    private static class OperateOnCp<T> {
        private final List<File> cp;

        public OperateOnCp(List<File> cp) {
            this.cp = cp;
        }

        private T operatetOnCp(String clazz, CpOperator<T> op) throws IOException {
            for (File c : cp) {
                if (c.isDirectory()) {
                    String root = sanitize(c.getAbsolutePath());
                    if (clazz == null) {
                        //no return, search
                        op.onDirEntry(c, null, null);
                    } else {
                        String classOnFs = clazz.replace(".", File.separator) + ".class";
                        File f = new File(root + File.separator + classOnFs);
                        if (f.exists()) {
                            return op.onDirEntry(c, f, clazz);
                        }
                    }
                } else {
                    if (op instanceof ListingCpOperator){
                        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(c))) {
                            T ret = onEntryList(zipInputStream, clazz, op);
                            if (ret != null) {
                                return ret;
                            }
                        }
                    } else {
                        if (clazz == null) {
                            throw new IOException("Trying to find class but no class is specified");
                        }
                        ArchiveManager am = ArchiveManager.getInstance();
                        am.setFile(c);
                        if (am.isClassInFile(clazz)) {
                            if (am.needExtract()) {
                                File f = am.unpack();
                                T ret = onEntryOther(f, clazz, op);
                                if (op instanceof WriteingCpOperator) {
                                    am.pack();
                                }
                                return ret;
                            } else {
                                return onEntryOther(c, clazz, op);
                            }
                        }
                    }
                }
            }
            if (clazz == null) {
                return op.finalizirung();
            }
            throw new IOException(clazz + " not found on CP");
        }

        // Only Listing - no need to unpack
        private T onEntryList(ZipInputStream zipInputStream, String clazz, CpOperator<T> op) throws IOException {
            ZipEntry entry = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (ArchiveManager.shouldOpen(entry.getName())) {
                    onEntryList(new ZipInputStream(zipInputStream), clazz, op);
                } else {
                    if (clazz == null) {
                        op.onJarEntry(null, null, entry, null);
                    } else {
                        String clazzInJar = toClass(entry.getName());
                        if (clazzInJar.equals(clazz)) {
                            return op.onJarEntry(null, null, entry, clazz);
                        }
                    }
                }
            }
            return null;
        }

        private T onEntryOther(File f, String clazz, CpOperator<T> op) throws IOException {
            ZipFile zipFile = new ZipFile(f);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (!ze.isDirectory()) {
                    String clazzInJar = toClass((ze.getName()));
                    if (clazzInJar.equals(clazz)) {
                        return op.onJarEntry(f, zipFile, ze, clazz);
                    }
                }
            }
            throw new IOException(clazz + " not found on CP");
        }
    }

    private static void addJustClass(String s, List<String> classes, String root) {
        if (s.endsWith(".class")) {
            classes.add(toClass(s.substring(root.length() + 1)));
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "ignored non .class element on cp: " + s);
        }
    }

    public static String toClass(String s) {
        return s.replace("/", ".").replaceAll("\\\\", ".").replaceAll(".class$", "");
    }

    private static String sanitize(String s) {
        while (s.contains("//") || s.contains("\\\\")) {
            s = s.replaceAll("//", "/");
            s = s.replaceAll("\\\\\\\\", "\\");
        }
        while (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }

    private static class WriteingCpOperator implements CpOperator<Void> {
        private final String body;

        public WriteingCpOperator(String body) {
            this.body = body;
        }

        @Override
        public Void onDirEntry(File dir, File clazz, String fqn) throws IOException {
            Files.write(clazz.toPath(), Base64.getDecoder().decode(body));
            OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "written " + clazz.getAbsolutePath());
            return null;
        }

        @Override
        public Void onJarEntry(File file, ZipFile zipFile, ZipEntry ze, String fqn) throws IOException {
            try (FileSystem fs = FileSystems.newFileSystem(file.toPath(), null)) {
                Path fileInsideZipPath = fs.getPath(ze.getName());
                Files.write(fileInsideZipPath, Base64.getDecoder().decode(body));
                OutputController.getLogger().log(OutputController.Level.MESSAGE_DEBUG, "written " + file.getAbsolutePath() + "!" + fileInsideZipPath);
            }
            return null;
        }

        @Override
        public Void finalizirung() {
            return null;
        }
    }

    private static class ReadingCpOperator implements CpOperator<String> {
        @Override
        public String onDirEntry(File dir, File clazz, String fqn) throws IOException {
            byte[] bytes = Files.readAllBytes(clazz.toPath());
            return Base64.getEncoder().encodeToString(bytes);
        }

        @Override
        public String onJarEntry(File file, ZipFile zipFile, ZipEntry ze, String fqn) throws IOException {
            byte[] data = new byte[(int) ze.getSize()];
            try (DataInputStream dis = new DataInputStream(zipFile.getInputStream(ze))) {
                dis.readFully(data);
            }
            return Base64.getEncoder().encodeToString(data);
        }

        @Override
        public String finalizirung() {
            return null;
        }
    }

    private static class ListingCpOperator implements CpOperator<Void> {
        private final List<String> classes;

        public ListingCpOperator(List<String> classes) {
            this.classes = classes;
        }

        @Override
        public Void onDirEntry(File c, File clazz, String fqn) throws IOException {
            Files.walkFileTree(c.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    String s = sanitize(path.toFile().getAbsolutePath());
                    String root = sanitize(c.getAbsolutePath());
                    addJustClass(s, classes, root);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
            return null;
        }

        @Override
        public Void onJarEntry(File file, ZipFile zipFile, ZipEntry ze, String fqn) throws IOException {
            addJustClass("/" + ze.getName(), classes, "");
            return null;
        }

        @Override
        public Void finalizirung() {
            return null;
        }
    }
}
