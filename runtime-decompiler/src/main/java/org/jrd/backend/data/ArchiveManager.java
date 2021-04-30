package org.jrd.backend.data;

import org.jrd.backend.communication.FsAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveManager {
    final File c;
    final String tmpdir = System.getProperty("java.io.tmpdir");
    ArrayList<String> currentPathInJars = new ArrayList<>();
    ArchivePathManager pathManager = ArchivePathManager.getInstance();
    int currentD = 0;

    public ArchiveManager(File c) {
        this.c = c;
    }

    /**
     * Finds out whether desired class is contained in <code>c</code>
     *
     * @param clazz Class to search
     * @return Whether class is in this file
     * @throws IOException Error while reading streams
     */
    public boolean isClassInFile(String clazz) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(c));
        if (pathManager.wasFound() && pathManager.getCurrentClazz().equals(clazz)) {
            return true;
        } else {
            delete();
            pathManager.clear();
            pathManager.setClazz(clazz);
            pathManager.addPathPart(c.getName());
            return findClazz(zis, clazz);
        }
    }

    /**
     * Recursive search through nested jars
     *
     * @param zis   ZipInputStream of current jar
     * @param clazz Class to search
     * @return Whether class is in this file
     * @throws IOException Error while reading streams
     */
    private boolean findClazz(ZipInputStream zis, String clazz) throws IOException {
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                if (shouldOpen(entry.getName())) {
                    pathManager.addPathPart(entry.getName());
                    if(findClazz(new ZipInputStream(zis), clazz)) {
                        return true;
                    }
                    pathManager.removePathPart(entry.getName());
                } else {
                    String clazzInJar = FsAgent.toClass(entry.getName());
                    if (clazzInJar.equals(clazz)) {
                        pathManager.setFound();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines whether this file can be opened with ZipInputStream
     * @param n Name of the file
     * @return Whether file can opened with ZipInputStream
     */
    public static boolean shouldOpen(String n) throws IOException {
        /* This way has been selected as there's no other "easier" way of determining if it is an archive.
         * If you wish to add a format, feel free to PR */
        // return name.endsWith(".zip") || name.endsWith(".zipx") || name.endsWith(".zz") || name.endsWith(".jar") || name.endsWith(".a") || name.endsWith(".ar") || name.endsWith(".cpio") || name.endsWith(".shar") || name.endsWith(".lbr") || name.endsWith(".iso") || name.endsWith(".mar") || name.endsWith(".sbx") || name.endsWith(".tar");
        String name = n.toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".jar");
    }

    /**
     * Returns whether extraction is necessary
     *
     * @return If extraction is necessary
     */
    public boolean needExtract() {
        return !(pathManager.getPathSize() == 1 || pathManager.isExtracted());
    }

    /**
     * Unpacks files necessary to access desired class
     *
     * @return .jar containing desired class
     * @throws IOException Error while reading streams
     */
    public File unpack() throws IOException {
        // Create my dir in tmpdir
        File f = new File(tmpdir + "/jrd/");
        if (f.mkdir()) {
            File ret = recursiveUnpack(c);
            if (ret != null) {
                pathManager.setExtracted();
            }
            return ret;
        } else {
            throw new IOException("Couldn't create temp file");
        }
    }

    /**
     * Recursively unpacks all required archives
     * @param toUnpack Archive to be unpacked
     * @return File pointer to last archive
     * @throws IOException
     */
    private File recursiveUnpack(File toUnpack) throws IOException {
        File destDir = new File(tmpdir + "/jrd/" + currentD);
        if (destDir.mkdir()) {
            byte[] buffer = new byte[1024];
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(toUnpack))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null ) {
                    File newFile = newFile(destDir, zipEntry);
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        FileOutputStream fout = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fout.write(buffer, 0, len);
                        }
                        fout.close();
                    }
                }
            }
        } else {
            throw new IOException();
        }
        currentD++;
        if (currentD == pathManager.getPathSize() - 1) {
            return new File(destDir.getAbsolutePath() + "/" + pathManager.get(currentD));
        } else if (currentD == pathManager.getPathSize()) {
            throw new IOException("Somehow got past");
        }
        return recursiveUnpack(new File(destDir.getAbsolutePath() + "/" + pathManager.get(currentD)));
    }

    /**
     * ZipSlip guard
     * @param destinationDir Destination directory
     * @param zipEntry Zip entry
     * @return
     * @throws IOException
     */
    public File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Deletes jrd temporary folder
     *
     * @return whether folder was successfully deleted
     */
    public boolean delete() {
        pathManager.clear();
        return deleteRecursive(new File(tmpdir + "/jrd/"));
    }

    /**
     * Deletes all nested files and directories, so the root can be deleted
     * @param f Directory to be deleted
     * @return Whether directory was delted
     */
    private boolean deleteRecursive(File f) {
        File[] content = f.listFiles();
        if (content != null) {
            for (File file: content) {
                deleteRecursive(file);
            }
        }
        return f.delete();
    }
}
