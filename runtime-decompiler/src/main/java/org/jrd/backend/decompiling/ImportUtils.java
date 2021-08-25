package org.jrd.backend.decompiling;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Directories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.jrd.backend.data.Directories.createPluginDirectory;

public final class ImportUtils {

    private ImportUtils() {

    }

    public static List<URL> getWrappersFromClasspath(){
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        List<URL> jsonFiles = new ArrayList<>();

        for(String entry : classpathEntries) {
            File entryFile = new File(entry);
            if (!entryFile.exists()) {
                continue;
            }

            try {
                Listable listable;
                if(entry.endsWith(".jar")){
                    listable = new Zip(entryFile);
                } else {
                    listable = new Directory(entryFile);
                }
                jsonFiles.addAll(listable.listChildren());

            } catch (IOException e) {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
            }
        }
        return jsonFiles;
    }

    public static String filenameFromUrl(URL url) {
        return url.toString().substring(url.toString().lastIndexOf("/") + 1);
    }

    public static void importOnePlugin(URL selectedURL, String selectedFilename) {
        try {
            copyWrappers(selectedURL, selectedFilename);
        } catch (IOException e) {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e);
        }
    }

    private static void copyBetweenStreams(URL wrapperJsonUrl, String wrapperFilename) throws IOException {
        try (
                InputStream is = wrapperJsonUrl.openStream();
                OutputStream os = new FileOutputStream(Directories.getPluginDirectory() + File.separator + wrapperFilename);
        ) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private static void copyWrappers(URL wrapperURL, String wrapperFilename) throws IOException {
        createPluginDirectory();

        copyBetweenStreams(wrapperURL, wrapperFilename);

        URL javaComplementUrl = new URL(flipWrapperExtension(wrapperURL.toString()));
        String javaComplementName = flipWrapperExtension(wrapperFilename);

        copyBetweenStreams(javaComplementUrl, javaComplementName);
    }

    public static String flipWrapperExtension(String filePath) {
        if (filePath.endsWith(".json")) {
            return filePath.replace(".json", ".java");
        } else if (filePath.endsWith(".java")) {
            return filePath.replace(".java", ".json");
        } else {
            OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Incorrect plugin wrapper path: " + filePath));
            return filePath;
        }
    }

    public static class Directory implements Listable {
        private File file;

        public Directory(File file) {
            this.file = file;
        }

        private void crawl(List<URL> pathList, File dir) throws MalformedURLException {
            File[] fileList = dir.listFiles();
            if(fileList != null){
                for (File file : fileList) {
                    if (file.isDirectory()) {
                        crawl(pathList, file);
                    } else {
                        if (file.getName().endsWith(".json")) {
                            pathList.add(file.toURI().toURL());
                        }
                    }
                }
            }
        }

        @Override
        public List<URL> listChildren() throws IOException {
            List<URL> children = new ArrayList<>();
            crawl(children, file);
            return children;
        }
    }

    public interface Listable {
        List<URL> listChildren() throws IOException;
    }

    public static class Zip implements Listable {
        private ZipFile jarFile;

        public Zip(File file) throws IOException {
            this.jarFile = new ZipFile(file.getPath());
        }

        @Override
        public List<URL> listChildren() throws MalformedURLException {
            List<URL> children = new ArrayList<>();

            Enumeration<? extends ZipEntry> entries = jarFile.entries();
            while(entries.hasMoreElements()){
                ZipEntry zipEntry = entries.nextElement();
                if(zipEntry.getName().endsWith(".json")){
                    URL url = new URL("jar:file:" + jarFile.getName() + "!/" + zipEntry.getName());
                    children.add(url);
                }
            }

            return children;
        }
    }
}
