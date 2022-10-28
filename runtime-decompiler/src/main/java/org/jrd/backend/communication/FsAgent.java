package org.jrd.backend.communication;

import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.ClassInfo;
import org.jrd.backend.core.Logger;
import org.jrd.backend.data.ArchiveManager;
import org.jrd.backend.data.VmInfo;

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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * This class is doing agent-like based operations on filesystem
 */
public final class FsAgent implements DelegatingJrdAgent {

    private static final Map<VmInfo, FsAgent> AGENTS = new HashMap<>();

    private final List<File> cp;
    private final String suffix;
    private final DelegatingHelper delegationCandidates = new DelegatingHelper();
    /**
     * This is exact oposite of how remote agent does this.
     * Remote agent keeps all overrides, because when new class defintion is laoded original is plled, and is modifed (overvritten) by new deffnitio.
     * <p>
     * On contrary, in FS, the class is after writing immediately overriden in file FileSystem. So we keep original, saved during first override.
     * If we keep original, we know class was overwritten.
     * The removal of of override  ==  restore of original (and remvoal of original from map
     * <p>
     * In addition, insted of byte[] we store base64 encoded String
     */
    private final Map<String, String> originals = new HashMap<>();

    private FsAgent(List<File> cp, String suffix) {
        this.cp = cp;
        this.suffix = suffix;
    }

    public static FsAgent get(VmInfo vmInfo) {
        FsAgent agent = AGENTS.get(vmInfo);
        if (agent == null) {
            agent = new FsAgent(vmInfo.getCp(), "class");
            AGENTS.put(vmInfo, agent);
        }
        return agent;
    }

    public static FsAgent createAdditionalClassPathFsAgent(String cp) {
        return createAdditionalClassPathFsAgent(
                Arrays.stream(cp.split(File.pathSeparator)).map(a -> new File(a)).collect(Collectors.toList())
        );
    }

    public static FsAgent createAdditionalClassPathFsAgent(List<File> cp) {
        return new FsAgent(cp, "class");
    }

    public static FsAgent createAdditionalSourcePathFsAgent(String cp) {
        return createAdditionalSourcePathFsAgent(
                Arrays.stream(cp.split(File.pathSeparator)).map(a -> new File(a)).collect(Collectors.toList())
        );
    }

    public static FsAgent createAdditionalSourcePathFsAgent(List<File> cp) {
        return new FsAgent(cp, "java");
    }

    public List<String> getOverrides() {
        return Collections.unmodifiableList(new ArrayList<>(originals.keySet()));
    }

    private synchronized int cleanOverrides(Pattern pattern) {
        int removed = 0;
        List<String> keys = getOverrides();
        for (String key : keys) {
            if (pattern.matcher(key).matches()) {
                String toRestore = originals.remove(key);
                uploadByteCode(key, toRestore);
                removed++;
                Logger.getLogger().log("Restored " + key + " original bytes");
            }
        }
        return removed;
    }

    /**
     * Opens a socket and sends the request to the agent via socket.
     *
     * @param request either "CLASSES" or "BYTES \n className", other formats
     *                are refused
     * @return agents response or null
     */
    @SuppressWarnings("ReturnCount") // pretty returns
    @Override
    public String submitRequest(final String request) {
        String[] q = request.split("\\s+");
        try {
            switch (AgentRequestAction.RequestAction.fromString(q[0])) {
                case OVERRIDES:
                    return String.join(";", getOverrides());
                case REMOVE_OVERRIDES:
                    int removed = cleanOverrides(Pattern.compile(q[1]));
                    if (removed == 0) {
                        throw new RuntimeException("Nothing removed by " + q[1] + " in " + originals.size() + " items");
                    }
                    return Communicate.NO_VALLUE_DONE_RESULT;
                case CLASSES:
                    return readClasses(false);
                case CLASSES_WITH_INFO:
                    return readClasses(true);
                case BYTES:
                    String classNameForBytes = q[1];
                    return sendByteCode(classNameForBytes);
                case OVERWRITE:
                    String classNameForOverwrite = q[1];
                    if (!originals.containsKey(classNameForOverwrite)) {
                        Logger.getLogger().log("backuping original bytecode of " + classNameForOverwrite);
                        originals.put(classNameForOverwrite, sendByteCode(classNameForOverwrite));
                    }
                    String futureBody = q[2];
                    uploadByteCode(classNameForOverwrite, futureBody);
                    return Communicate.NO_VALUE_OK_RESULT;
                case ADD_CLASS:
                    throw new RuntimeException("Not sure if adding class will be ever implemented for FS, but should be");
                case ADD_JAR:
                    throw new RuntimeException("Add jar is not implemented in FS vm, and never will");
                case INIT_CLASS:
                    Logger.getLogger().log(Logger.Level.DEBUG, "Init class have no meaning in FS 'vm'");
                    return Communicate.NO_VALLUE_DONE_RESULT;
                case HALT:
                    return Communicate.NO_VALUE_OK_RESULT;
                default:
                    throw new RuntimeException("Unknown command: " + q[0]);
            }
        } catch (Exception ex) {
            Logger.getLogger().log(ex);
            return ErrorCandidate.toError(ex);
        }
    }

    private Void uploadByteCode(String clazz, String body) {
        try {
            return new OperateOnCp<Void>(cp, suffix).operateOnCp(clazz, new WritingCpOperator(body));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendByteCode(String clazz) {
        try {
            String s = new OperateOnCp<String>(cp, suffix).operateOnCp(clazz, new ReadingCpOperator());
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readClasses(final boolean details) throws IOException {
        List<String> classes = new ArrayList<>();
        new OperateOnCp<Void>(cp, suffix).operateOnCp(null, new ListingCpOperator(classes, details));
        return String.join(";", classes);
    }

    private interface CpOperator<T> {
        T onDirEntry(File dir, File clazz) throws IOException;

        T onJarEntry(File file, ZipFile zipFile, ZipEntry ze) throws IOException;
    }

    private static class OperateOnCp<T> {
        private final List<File> cp;
        private final String suffix;

        OperateOnCp(List<File> cp, String suffix) {
            this.cp = cp;
            this.suffix = suffix;
        }

        @SuppressWarnings({"NestedIfDepth", "ReturnCount"}) // no way around this
        private T operateOnCp(String clazz, CpOperator<T> op) throws IOException {
            for (File c : cp) {
                if (c.isDirectory()) {
                    String root = sanitize(c.getAbsolutePath());

                    if (clazz == null) {
                        //no return, search
                        op.onDirEntry(c, null);
                    } else {
                        String classOnFs = clazz.replace(".", File.separator) + "." + suffix;
                        File f = new File(root + File.separator + classOnFs);
                        if (f.exists()) {
                            return op.onDirEntry(c, f);
                        }
                    }
                } else {
                    if (op instanceof ListingCpOperator) {
                        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(c))) {
                            T ret = onEntryList(zipInputStream, clazz, op, c.getAbsolutePath() + "!");
                            if (ret != null) {
                                return ret;
                            }
                        }
                    } else {
                        if (clazz == null) {
                            throw new IOException("Trying to find class but no class is specified");
                        }

                        ArchiveManager am = ArchiveManager.getInstance();
                        if (am.isClassInFile(clazz, c)) {
                            if (am.needExtract()) {
                                File f = am.unpack(c);
                                T ret = onEntryOther(f, clazz, op);
                                if (op instanceof WritingCpOperator) {
                                    am.pack(c);
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
                return null;
            }
            throw new IOException(clazz + " not found on CP");
        }

        // Only Listing - no need to unpack
        private T onEntryList(ZipInputStream zipInputStream, String clazz, CpOperator<T> op, String c) throws IOException {
            ZipEntry entry = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (ArchiveManager.shouldOpen(entry.getName())) {
                    onEntryList(new ZipInputStream(zipInputStream), clazz, op, c + "/" + entry.getName() + "!");
                } else {
                    if (clazz == null) {
                        op.onJarEntry(new File(c), null, entry);
                    } else {
                        String clazzInJar = toClass(entry.getName());
                        if (clazzInJar.equals(clazz)) {
                            return op.onJarEntry(new File(c), null, entry);
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
                    String clazzInJar = toClass(ze.getName());
                    if (clazzInJar.equals(clazz)) {
                        return op.onJarEntry(f, zipFile, ze);
                    }
                }
            }
            throw new IOException(clazz + " not found on CP");
        }
    }

    private static void addJustClass(String s, List<String> classes, String root, boolean details, String detailsPath) {
        if (s.endsWith(".class")) {
            if (details) {
                classes.add(
                        new ClassInfo(toClass(s.substring(root.length() + 1)), detailsPath, "class order in realvm may differ")
                                .toAgentLikeAnswer()
                );
            } else {
                classes.add(toClass(s.substring(root.length() + 1)));
            }
        } else {
            Logger.getLogger().log(Logger.Level.DEBUG, "ignored non .class element on cp: " + s);
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

    private static class WritingCpOperator implements CpOperator<Void> {
        private final String body;

        WritingCpOperator(String body) {
            this.body = body;
        }

        @Override
        public Void onDirEntry(File dir, File clazz) throws IOException {
            Files.write(clazz.toPath(), Base64.getDecoder().decode(body));
            Logger.getLogger().log(Logger.Level.DEBUG, "written " + clazz.getAbsolutePath());
            return null;
        }

        @Override
        public Void onJarEntry(File file, ZipFile zipFile, ZipEntry ze) throws IOException {
            zipFile.close(); // caused java.nio.file.FileSystemException when closing fs after try-with-resources

            try (FileSystem fs = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null)) {
                Path fileInsideZipPath = fs.getPath(ze.getName());
                Files.write(fileInsideZipPath, Base64.getDecoder().decode(body));
                Logger.getLogger().log(Logger.Level.DEBUG, "written " + file.getAbsolutePath() + "!" + fileInsideZipPath);
            }
            return null;
        }
    }

    private static class ReadingCpOperator implements CpOperator<String> {
        @Override
        public String onDirEntry(File dir, File clazz) throws IOException {
            byte[] bytes = Files.readAllBytes(clazz.toPath());
            return Base64.getEncoder().encodeToString(bytes);
        }

        @Override
        public String onJarEntry(File file, ZipFile zipFile, ZipEntry ze) throws IOException {
            byte[] data = new byte[(int) ze.getSize()];
            try (DataInputStream dis = new DataInputStream(zipFile.getInputStream(ze))) {
                dis.readFully(data);
            }
            zipFile.close();
            return Base64.getEncoder().encodeToString(data);
        }
    }

    private static class ListingCpOperator implements CpOperator<Void> {
        private final List<String> classes;
        private final boolean details;

        ListingCpOperator(List<String> classes, boolean details) {
            this.classes = classes;
            this.details = details;
        }

        @SuppressWarnings("AnonInnerLength") // already in an inner class, no need to extract it to another inner class
        @Override
        public Void onDirEntry(File c, File clazz) throws IOException {
            Files.walkFileTree(c.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
                    String s = sanitize(path.toFile().getAbsolutePath());
                    String root = sanitize(c.getAbsolutePath());
                    addJustClass(s, classes, root, details, path.toFile().getAbsolutePath());
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
        public Void onJarEntry(File file, ZipFile zipFile, ZipEntry ze) throws IOException {
            addJustClass("/" + ze.getName(), classes, "", details, file.getAbsolutePath() + "/" + ze.getName());
            return null;
        }
    }

    @Override
    public JrdAgent addDelegatingAgent(JrdAgent agent) {
        return delegationCandidates.addDelegatingAgent(agent);
    }

    @Override
    public JrdAgent removeDelegatingAgent(JrdAgent agent) {
        return delegationCandidates.removeDelegatingAgent(agent);
    }

    @Override
    public int cleanDelegatingAgents() {
        return delegationCandidates.cleanDelegatingAgents();
    }
}
