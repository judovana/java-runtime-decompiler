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
                default:
                    throw new RuntimeException("Unknown command: " + q[0]);
            }
        } catch (Exception ex) {
            OutputController.getLogger().log(ex);
            return "ERROR";
        }
    }

    private String readClasses() throws IOException {
        List<String> classes = new ArrayList<>();
        for (File c : cp) {
            String root = sanitize(c.getAbsolutePath());
            Files.walkFileTree(c.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    String s = sanitize(path.toFile().getAbsolutePath());
                    if (s.endsWith(".class")) {
                        classes.add(toClass(s.substring(root.length()+1)));
                    } else {
                        //todo!
                        //is it zip? look into it!
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
