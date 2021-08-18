package org.jrd.frontend.frame.plugins.embedded;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Directory implements Listable {
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
