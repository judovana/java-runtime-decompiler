package org.jrd.frontend.frame.main;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.logging.Level;


public class GlobalConsole implements MessagesListener, OverwriteClassDialog.TextLog {

    private static GlobalConsole console = new GlobalConsole();
    private final JTextArea log;
    private final JButton clean;
    private final JFrame frame;

    public GlobalConsole() {
        JButton tmpClean;
        JTextArea tmpLog;
        JFrame tmpFrame;
        Logger.getLogger().disableGuiLogging();
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                tmpLog = new JTextArea();
                tmpClean = new JButton("Clean log");
                tmpFrame = new JFrame();
                tmpFrame.setLayout(new BorderLayout());
                tmpFrame.add(new JScrollPane(tmpLog));
                JPanel p = new JPanel(new BorderLayout());
                JCheckBox verbose = new JCheckBox("verbose mode", Logger.getLogger().isVerbose());
                verbose.addActionListener(actionEvent -> Logger.getLogger().setVerbose(verbose.isSelected()));
                p.add(tmpClean, BorderLayout.CENTER);
                p.add(verbose, BorderLayout.EAST);
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
            } catch (Error eerr) {
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

    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Public encapsualted singleton."
    )
    public static GlobalConsole getConsole() {
        return console;
    }

    public void show() {
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
                log.setText(log.getText() + stamp() + tail(s));
                log.setCaretPosition(log.getDocument().getLength());
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
        log.setText(s);
        log.setCaretPosition(log.getDocument().getLength());
    }
}
