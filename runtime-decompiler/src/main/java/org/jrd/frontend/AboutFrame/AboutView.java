package org.jrd.frontend.AboutFrame;

import org.jrd.backend.core.OutputController;
import org.jrd.frontend.MainFrame.MainFrameView;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class AboutView extends JDialog {

    public AboutView(MainFrameView mainFrameView) {
        JLabel label = new JLabel();
        Font font = label.getFont();

        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
        style.append("font-size:" + font.getSize() + "pt;");

        JEditorPane editorPane = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" +
            "<h2>Java-Runtime-Decompiler</h2>" +
            "Version " + getVersion() + "<br />" +
            "Licenced under the GNU General Public License v3.0<br />" +
            "Visit <a href=\"https://github.com/pmikova/java-runtime-decompiler\">the GitHub repository</a> for more information.<br />" +
            "</body></html>");

        editorPane.addHyperlinkListener(event -> {
            if(event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(event.getURL().toURI());
                } catch (IOException | URISyntaxException e0) {
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, e0);
                } catch (UnsupportedOperationException e1){
                    OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new RuntimeException("Unable to open link."));
                }

            }
        });
        editorPane.setEditable(false);
        editorPane.setBackground(new Color(label.getBackground().getRGB()));

        JOptionPane.showMessageDialog(mainFrameView.getMainFrame(), editorPane, "About Java-Runtime-Decompiler", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getVersion(){
        String path = "/version.prop";
        InputStream stream = getClass().getResourceAsStream(path);
        if(stream == null) {
            return "UNKNOWN";
        }
        Properties props = new Properties();
        try {
            props.load(stream);
            stream.close();
            return (String) props.get("version");
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}
