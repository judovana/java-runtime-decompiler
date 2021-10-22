package org.jrd.frontend.frame.main;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.ScreenFinder;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class GlobalConsole implements MessagesListener, OverwriteClassDialog.TextLog {

    private static GlobalConsole console = new GlobalConsole();
    private final RSyntaxTextArea log;
    private final JButton clean;
    private final JFrame frame;
    private boolean first = true;

    public GlobalConsole() {
        JButton tmpClean;
        RSyntaxTextArea tmpLog;
        JFrame tmpFrame;
        Logger.getLogger().disableGuiLogging();
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                tmpLog = new RSyntaxTextArea();
                tmpLog.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS);
                tmpClean = new JButton("Clean log");
                tmpFrame = new JFrame("Log console");
                tmpFrame.setLayout(new BorderLayout());
                tmpFrame.add(new JScrollPane(tmpLog));
                JPanel p = new JPanel(new BorderLayout());
                JCheckBox verbose = new JCheckBox("Verbose mode", Logger.getLogger().isVerbose());
                verbose.addActionListener(actionEvent -> Logger.getLogger().setVerbose(verbose.isSelected()));
                JComboBox<String> hgltr = new JComboBox<String>(getAllLexers());
                hgltr.setSelectedItem(SyntaxConstants.SYNTAX_STYLE_SAS);
                hgltr.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (log != null) {
                            log.setSyntaxEditingStyle(hgltr.getSelectedItem().toString());
                        }
                    }
                });
                p.add(tmpClean, BorderLayout.CENTER);
                p.add(verbose, BorderLayout.EAST);
                p.add(hgltr, BorderLayout.WEST);
                tmpFrame.add(p, BorderLayout.SOUTH);
                tmpFrame.setSize(800, 600);
                tmpClean.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (log != null) {
                            log.setText("");
                        }
                    }
                });
            } catch (Error | Exception eerr) {
                Logger.getLogger().log(eerr);
                tmpFrame = null;
                tmpLog = null;
                tmpClean = null;
            }
        } else {
            tmpFrame = null;
            tmpLog = null;
            tmpClean = null;
        }
        clean = tmpClean;
        log = tmpLog;
        frame = tmpFrame;
        Logger.getLogger().enableGuiLogging();
    }

    private String[] getAllLexers() throws IllegalAccessException {
        List<String> r = new ArrayList();
        Field[] fields = SyntaxConstants.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(String.class)) {
                r.add(field.get(null).toString());
            }
        }
        return r.toArray(new String[0]);
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Public encapsulated singleton.")
    public static GlobalConsole getConsole() {
        return console;
    }

    public void show() {
        if (first) {
            ScreenFinder.centerWindowToCurrentScreen(frame);
            first = false;
        }
        frame.setVisible(true);
    }

    private String stamp() {
        return "[" + new Date().toInstant().toString() + "] ";
    }

    private String tail(String s) {
        if (s.endsWith("\n")) {
            return s;
        } else {
            return s + "\n";
        }
    }

    private Logger.Level levelToLevel(Level level) {
        if (level.intValue() < Level.WARNING.intValue()) {
            return Logger.Level.DEBUG;
        } else {
            return Logger.Level.ALL;
        }
    }

    @Override
    public void addMessage(java.util.logging.Level level, String s) {
        Logger.Level ourLevel = levelToLevel(level);
        if (log != null) {
            if (ourLevel == Logger.Level.ALL || Logger.getLogger().isVerbose()) {
                try {
                    log.setText(log.getText() + stamp() + tail(s));
                    log.setCaretPosition(log.getDocument().getLength());
                } catch (Exception ex) {
                    //belive or not, rsyntax are can throw exception form here, and asnothing expects that, it may be fatal
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void addMessage(Level level, String format, Object... args) {
        MessagesListener.super.addMessage(level, format, args);
    }

    @Override
    public String getText() {
        return log.getText();
    }

    @Override
    public void setText(String s) {
        try {
            log.setText(s);
            log.setCaretPosition(log.getDocument().getLength());
        } catch (Exception ex) {
            //belive or not, rsyntax are can throw exception form here, and asnothing expects that, it may be fatal
            ex.printStackTrace();
        }
    }
}
