package org.jrd.frontend.PluginMangerFrame.embedded;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Zip implements Listable{
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
