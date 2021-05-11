package org.jrd.backend.data;

import org.jrd.backend.communication.FsAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
        return !(pathManager.getPathSize() == 1);
    }

    /**
     * Unpacks files necessary to access desired class
     *
     * @return .jar containing desired class
     * @throws IOException Error while reading streams
     */
    public File unpack() throws IOException {
        if (pathManager.isExtracted()) {
            // If file is already extracted, return the extracted one
           return new File(tmpdir + "/jrd/" + (pathManager.getPathSize() - 2) + "/" + (pathManager.get(pathManager.getPathSize() - 1)));
        }
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
     * Packs unpacked files
     */
    public void pack() throws IOException {
        // Go from end to start
        int i = pathManager.getPathSize();
        i -= 2;
        for (; i >= 0; i--) {
            // Create new zip that will contain edited files
            String[] tmp = pathManager.get(i).split("/");
            String path = tmpdir + "/jrd/" + tmp[tmp.length - 1] + "/";
            FileOutputStream fileStream = new FileOutputStream(path);
            ZipOutputStream zOut = new ZipOutputStream(fileStream);
            File f2zip = new File(tmpdir + "/jrd/" + (i));
            for (File f : f2zip.listFiles()) {
                recursiveZip(f, f.getName(), zOut);
            }
            zOut.finish();
            zOut.close();
            fileStream.close();
            // Move it into the temp file if it's not last, so it can be packaged
            if (i > 0) {
                Files.copy(Path.of(path), Path.of(tmpdir + "/jrd/" + (i - 1) + "/" + pathManager.get(i)), REPLACE_EXISTING);
            } else {
                // It's the last, replace the original
                Files.copy(Path.of(path), c.toPath(), REPLACE_EXISTING);
            }
            // Delete once it was moved
            new File(path).delete();
        }
    }

    /**
     * Recursively adds file or files inside folder to archive
     * @param f2zip File/Folder to be archived
     * @param fName Name of the file
     * @param zOut Zip output stream used to output zipped bytes
     * @throws IOException
     */
    public void recursiveZip(File f2zip, String fName, ZipOutputStream zOut) throws IOException {
        if (f2zip.isDirectory()) {
            if (!fName.endsWith("/")) {
                fName += "/";
            }
            zOut.putNextEntry(new ZipEntry(fName));
            zOut.closeEntry();
            File[] sub = f2zip.listFiles();
            for (File child : sub) {
                recursiveZip(child, fName + child.getName(), zOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(f2zip);
        ZipEntry zEntry = new ZipEntry(fName);
        zOut.putNextEntry(zEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zOut.write(bytes, 0, length);
        }
        zOut.closeEntry();
        fis.close();
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
