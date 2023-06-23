package org.jrd.frontend.frame.about;

import org.jrd.backend.core.Logger;
import org.jrd.backend.data.MetadataProperties;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;

public class AboutView extends JDialog {

    public AboutView(JFrame mainFrameView, boolean showVersion) {
        JLabel label = new JLabel();
        Font font = label.getFont();

        String style = "font-family:" + font.getFamily() + ";" + "font-weight:" + (font.isBold() ? "bold" : "normal") + ";" + "font-size:" +
                font.getSize() + "pt;";

        JEditorPane editorPane = new JEditorPane(
                "text/html",
                "<html><body style=\"" + style + "\">" + "<h2>Java-Runtime-Decompiler</h2>" + "Version " +
                        (showVersion?MetadataProperties.getInstance().getVersion():"No version") + "<br />" +
                        "Licenced under the GNU General Public License v3.0<br />" +
                        "Visit <a href=\"https://github.com/pmikova/java-runtime-decompiler\">the GitHub repository</a>" +
                        " for more information.<br />" + "</body></html>"
        );

        editorPane.addHyperlinkListener(event -> {
            if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(event.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    Logger.getLogger().log(Logger.Level.ALL, e1);
                } catch (UnsupportedOperationException e2) {
                    Logger.getLogger().log(Logger.Level.ALL, new RuntimeException("Unable to open link."));
                }

            }
        });
        editorPane.setEditable(false);
        editorPane.setBackground(new Color(label.getBackground().getRGB()));

        JOptionPane.showMessageDialog(
                mainFrameView, editorPane, "About Java-Runtime-Decompiler", JOptionPane.INFORMATION_MESSAGE
        );
    }
}
