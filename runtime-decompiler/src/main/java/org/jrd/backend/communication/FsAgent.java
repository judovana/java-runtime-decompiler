package org.jrd.backend.communication;


import org.jrd.backend.core.OutputController;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

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

    private String sendByteCode(String request) {
        String[] clazz = request.split("\\s+");
        try {
            return sendByteCodeImpl(clazz[1]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendByteCodeImpl(String clazz) throws IOException {
        for (File c : cp) {
            String root = sanitize(c.getAbsolutePath());
            String classOnFs = clazz.replace(".", File.separator) + ".class";
            File f = new File(root + File.separator + classOnFs);
            if (f.exists()) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                return Base64.getEncoder().encodeToString(bytes);
            }
        }
        throw new IOException(clazz + " not found on CP");
    }

    private String readClasses() throws IOException {
        List<String> classes = new ArrayList<>();
        for (File c : cp) {
            String root = sanitize(c.getAbsolutePath());
            if (c.isDirectory()) {
                Files.walkFileTree(c.toPath(), new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        String s = sanitize(path.toFile().getAbsolutePath());
                        if (s.endsWith(".class")) {
                            classes.add(toClass(s.substring(root.length() + 1)));
                        } else {
                        }
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
            } else {
                //is it jar/zip? trasnaprently traverse it!
            }
        }
        return classes.stream().collect(Collectors.joining(";"));
    }

    private String toClass(String s) {
        return s.replace("/", ".").replaceAll("\\\\", ".").replaceAll(".class$", "");
    }

    private String sanitize(String s) {
        while (s.contains("//") || s.contains("\\\\")) {
            s = s.replaceAll("//", "/");
            s = s.replaceAll("\\\\\\\\", "\\");
        }
        while (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }
}
