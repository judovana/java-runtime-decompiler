package org.jrd.frontend;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import org.jrd.backend.core.AgentRequestAction;
import org.jrd.backend.core.DecompilerRequestReceiver;
import org.jrd.backend.data.Cli;
import org.jrd.backend.data.VmInfo;
import org.jrd.backend.data.VmManager;
import org.jrd.frontend.frame.main.VmDecompilerInformationController;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static final int FULLY_QUALIFIED_NAME = 0;
    public static final int SRC_SUBDIRS_NAME = 1;
    public static final int CUSTOM_NAME = 2;

    public static interface StatusKeeper {
        public void setText(String s);

        public void onException(Exception ex);
    }

    public static boolean saveByGui(String fileNameBase, int naming, String suffix, StatusKeeper status, String clazz, byte[] content) {
        String name = "???";
        String ss = "Error to save: ";
        boolean r = true;
        try {
            name = cheatName(fileNameBase, naming, suffix, clazz);
            File f = new File(name);
            if (naming == SRC_SUBDIRS_NAME) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), content);
            ss = "Saved: ";
        } catch (Exception ex) {
            status.onException(ex);
            r = false;
        }
        status.setText(ss + name);
        return r;
    }

    public static boolean uploadByGui(VmInfo vmInfo, VmManager vmManager, StatusKeeper status, String clazz, byte[] content) {
        String ss = "Error to upload: ";
        boolean r = true;
        try {
            String response = uploadBytecode(clazz, vmManager, vmInfo, content);
            if (response.equals(DecompilerRequestReceiver.ERROR_RESPONSE)) {
                throw new Exception("Agent returned error");
            }
            ss = "uploaded: ";
        } catch (Exception ex) {
            status.onException(ex);
            r = false;
        }
        status.setText(ss + clazz);
        return r;
    }

    public static String cheatName(String base, int selectedIndex, String suffix, String fullyClassifiedName) {
        if (selectedIndex == CUSTOM_NAME) {
            return base;
        }
        if (selectedIndex == FULLY_QUALIFIED_NAME) {
            return base + "/" + fullyClassifiedName + suffix;
        }
        if (selectedIndex == SRC_SUBDIRS_NAME) {
            return base + "/" + fullyClassifiedName.replaceAll("\\.", "/") + suffix;
        }
        throw new RuntimeException("Unknown name target " + selectedIndex);
    }


    public static String uploadBytecode(String clazz, VmManager vmManager, VmInfo vmInfo, byte[] bytes) {
        final String body = VmDecompilerInformationController.bytesToBase64(bytes);
        AgentRequestAction request = VmDecompilerInformationController.createRequest(vmInfo, AgentRequestAction.RequestAction.OVERWRITE, clazz, body);
        return VmDecompilerInformationController.submitRequest(vmManager, request);
    }

    public static String guessClass(String src) throws IOException {
        return Cli.guessName(Files.readAllBytes(new File(src).toPath()));
    }
    public static IdentifiedSource[] sourcesToIdentifiedSources(boolean recursive, List<File> sources) throws IOException {
        return sourcesToIdentifiedSources(recursive, sources.stream().map(x -> x.getAbsolutePath()).toArray(String[]::new));
    }

    public static IdentifiedSource[] sourcesToIdentifiedSources(boolean recursive, String... sources) throws IOException {
        List<IdentifiedSource> loaded = new ArrayList<>(sources.length);
        for (int i = 0; i < sources.length; i++) {
            File f = new File(sources[i]);
            if (f.isDirectory()) {
                if (recursive) {
                    Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                            File clazz = path.toFile();
                            if (clazz.getName().endsWith(".java")) {
                                loaded.add(new IdentifiedSource(new ClassIdentifier(guessClass(clazz.getAbsolutePath())), Files.readAllBytes(clazz.toPath())));
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
            } else {
                loaded.add(new IdentifiedSource(new ClassIdentifier(guessClass(f.getAbsolutePath())), Files.readAllBytes(f.toPath())));
            }
        }
        return loaded.toArray(new IdentifiedSource[0]);
    }

    ;

}
