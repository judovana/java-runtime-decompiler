package org.jrd.frontend.utility;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jrd.backend.core.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.io.InputStream;

public final class ImageButtonFactory {

    private static final boolean WOULD_FONT_BE_BLACK;
    private static final short[] INVERT_TABLE;
    private static final short[] KEEP_TABLE;

    private static ImageIcon attachIcon;
    private static ImageIcon detachIcon;

    public static final String DETACH_RESOURCE = "detach_24dp.png";

    static {
        INVERT_TABLE = new short[256];
        for (short i = 0; i < 256; i++) {
            INVERT_TABLE[i] = (short) (255 - i);
        }

        KEEP_TABLE = new short[256];
        for (short i = 0; i < 256; i++) {
            KEEP_TABLE[i] = i;
        }

        Color defaultFontColor = new JButton("text").getForeground();
        double luminance = 0.2126 * defaultFontColor.getRed() + 0.7152 * defaultFontColor.getGreen() + 0.0722 * defaultFontColor.getBlue();
        WOULD_FONT_BE_BLACK = luminance < 128;
    }

    private ImageButtonFactory() {
    }

    private static ImageIcon loadResourceIcon(String resourceName) throws IOException {
        InputStream is = ImageButtonFactory.class.getResourceAsStream("/icons/" + resourceName);
        if (is == null) {
            throw new IOException("No resource with name '" + resourceName + "' found.");
        }

        BufferedImage result = ImageIO.read(is);

        if (!WOULD_FONT_BE_BLACK) {
            result = invertImage(result);
        }

        return new ImageIcon(result);
    }

    private static JButton createButton(ImageIcon icon) {
        JButton result;

        result = new JButton(icon);
        result.setBorder(new EmptyBorder(5, 5, 5, 5));

        return result;
    }

    private static JButton createButton(String iconResourceName, String backupButtonText, String toolTipText) {
        JButton result;

        try {
            result = createButton(loadResourceIcon(iconResourceName));
        } catch (IOException e) {
            Logger.getLogger()
                    .log(Logger.Level.ALL, "Unable to load icon image resource '" + iconResourceName + "' as an image, using backup text.");
            result = new JButton(backupButtonText);
        }

        result.setToolTipText(toolTipText);
        return result;
    }

    private static BufferedImage invertImage(final BufferedImage src) throws IOException {
        short[][] tableData;
        switch (src.getColorModel().getNumComponents()) {
            case 2: // grayscale+alpha
                tableData = new short[][]{INVERT_TABLE, KEEP_TABLE};
                break;
            case 4: // RGBA
                tableData = new short[][]{INVERT_TABLE, INVERT_TABLE, INVERT_TABLE, KEEP_TABLE};
                break;
            default:
                throw new IOException("Image file has unrecognized number of components.");
        }

        return new LookupOp(new ShortLookupTable(0, tableData), null).filter(src, src);
    }

    public static JButton createUndoButton() {
        return createButton("undo_24dp.png", "<-", "Undo");
    }

    public static JButton createRedoButton() {
        return createButton("redo_24dp.png", "->", "Redo");
    }

    public static JButton createCompileButton() {
        return createButton("compile_24dp.png", "C", "Compile");
    }

    public static JButton createCompileUploadButton() {
        return createButton("upload_24dp.png", "C&U", "Compile and upload");
    }

    public static JButton createOverwriteButton() {
        return createButton("overwrite_24dp.png", "Overwrite", "Overwrite dialog");
    }

    public static JButton createInitButton() {
        return createButton("init_24dp.png", "Init", "Initialize class");
    }

    public static JButton createAddButton() {
        return createButton("add_24dp.png", "+", "Add");
    }

    public static JButton createRemoveButton() {
        return createButton("remove_24dp.png", "-", "Remove");
    }

    public static JButton createRefreshButton(String tooltip) {
        return createButton("refresh_24dp.png", "\u21BB", tooltip);
    }

    public static JButton createEditButton(String tooltip) {
        return createButton("insert_24dp.png", "API", tooltip);
    }

    public static JButton createTrashButton() {
        return createButton("trash_24dp.png", "X", "Remove");
    }

    public static JButton createDetachButton() {
        return createDetachButton("Detach", "Detach");
    }

    public static JButton createDetachButton(String backup, String tooltip) {
        return createButton(DETACH_RESOURCE, backup, tooltip);
    }

    // Lazy init prevents icon loading each time window is reattached or if the detach function isn't used at all
    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Lazy initialization with a synchronized accessor")
    public static synchronized ImageIcon getAttachIcon() throws IOException {
        if (attachIcon == null) {
            attachIcon = loadResourceIcon("attach_24dp.png");
        }
        return attachIcon;
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Lazy initialization with a synchronized accessor")
    public static synchronized ImageIcon getDetachIcon() throws IOException {
        if (detachIcon == null) {
            detachIcon = loadResourceIcon(DETACH_RESOURCE);
        }
        return detachIcon;
    }

    public static void flipDetachButton(JButton button, boolean shouldAttach, String backupText) {
        try {
            button.setIcon(shouldAttach ? getAttachIcon() : getDetachIcon());
            button.setText("");
        } catch (IOException e) {
            button.setIcon(null);
            button.setText(backupText);
            Logger.getLogger().log(Logger.Level.ALL, "Unable to load " + backupText + " button icon.");
        }

        button.setToolTipText(backupText);
    }
}
