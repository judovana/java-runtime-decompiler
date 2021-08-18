package org.jrd.frontend.frame.plugins.embedded;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface Listable {
    public List<URL> listChildren() throws IOException;
}
