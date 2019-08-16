package org.jrd.frontend.PluginMangerFrame.embedded;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public interface Listable {
    public List<URL> listChildren() throws IOException;
}
