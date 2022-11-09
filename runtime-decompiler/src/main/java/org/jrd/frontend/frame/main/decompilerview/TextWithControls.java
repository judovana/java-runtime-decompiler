package org.jrd.frontend.frame.main.decompilerview;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class TextWithControls extends JPanel implements LinesProvider {

    private final RSyntaxTextArea bytecodeSyntaxTextArea;
    private final SearchControlsPanel bytecodeSearchControls;
    private DecompilationController.AgentApiGenerator popup;
    private File f;

    public TextWithControls(String title) {
        HexWithControls.initTabLayers(this, title);
        bytecodeSyntaxTextArea = createSrcTextArea(true);
        bytecodeSearchControls = SearchControlsPanel.createBytecodeControls(this);
        RTextScrollPane bytecodeScrollPane = new RTextScrollPane(bytecodeSyntaxTextArea);
        this.add(bytecodeScrollPane);
        this.add(bytecodeSearchControls, BorderLayout.SOUTH);
    }

    public String getText() {
        return bytecodeSyntaxTextArea.getText();
    }

    public byte[] getTextAsBytes() {
        return bytecodeSyntaxTextArea.getText().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void undo() {
        bytecodeSyntaxTextArea.undoLastAction();
    }


    @Override
    public void redo() {
        bytecodeSyntaxTextArea.redoLastAction();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "to lazy to invert control here")
    public RSyntaxTextArea getSyntaxTextArea() {
        return bytecodeSyntaxTextArea;
    }

    void resetSrcArea(String data) {
        bytecodeSyntaxTextArea.setText(data);
        resetUndoRedo();
    }

    private RSyntaxTextArea createSrcTextArea(boolean api) {
        RSyntaxTextArea rst = new RSyntaxTextArea();
        rst.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    bytecodeSearchControls.clickNextButton();
                }
                if ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                        showApiMenu(null);
                    }
                }
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        showApiMenu(null);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        bytecodeSearchControls.focus(); //!!global :(
                    }
                }
            }
        });
        rst.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        rst.setCodeFoldingEnabled(true);
        return rst;
    }

    public void setPopup(DecompilationController.AgentApiGenerator ap) {
        this.popup = ap;
    }

    void showApiMenu(Point forcedLocation) {
        Point caretPosition = bytecodeSyntaxTextArea.getCaret().getMagicCaretPosition();
        if (caretPosition == null || forcedLocation != null) {
            caretPosition = forcedLocation;
        }
        if (caretPosition == null) {
            return;
        }

        // y is offset to the next row
        popup.getFor(bytecodeSyntaxTextArea, forcedLocation == null).show(
                bytecodeSyntaxTextArea, caretPosition.x,
                caretPosition.y + bytecodeSyntaxTextArea.getFontMetrics(bytecodeSyntaxTextArea.getFont()).getHeight()
        );
    }

    @Override
    public List<String> getLines(LinesFormat type) {
        return Arrays.asList(bytecodeSyntaxTextArea.getText().split("\n"));
    }

    @Override
    public void setLines(LinesFormat type, List<String> lines) {
        bytecodeSyntaxTextArea.setText(lines.stream().collect(Collectors.joining("\n")));
    }

    @Override
    public boolean isBin() {
        return false;
    }

    @Override
    public File getFile() {
        return f;
    }

    @Override
    public void setFile(File f) {
        this.f = f;
    }

    @Override
    public void open(File f) throws IOException {
        String s = Files.readString(f.toPath());
        resetSrcArea(s);
    }

    @Override
    public void save(File f) throws IOException {
        Files.writeString(f.toPath(), bytecodeSyntaxTextArea.getText());
    }

    @Override
    public JComponent asComponent() {
        return this;
    }


    @Override
    public void resetUndoRedo() {
        bytecodeSyntaxTextArea.discardAllEdits(); // makes the bytecode upload not undoable
        bytecodeSyntaxTextArea.setCaretPosition(0);
    }

    @Override
    public String getName() {
        if (getFile() != null) {
            return getFile().getName();
        } else {
            return super.getName();
        }
    }
}
