package org.jrd.frontend.frame.main;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.MessagesListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jrd.backend.core.Logger;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.ScreenFinder;

public class GlobalConsole implements MessagesListener, OverwriteClassDialog.TextLog {

    public static final String CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT = "io.github.mkoncek.cplc.log-to-provider";
    public static final String CPLC_IL = "IGNORE_LAMBDAS";
    public static final String CPLC_IA = "IGNORE_ARRAYS";
    public static final String CPLC_SO = "SKIPPING_OVER";
    public static final String CPLC_AC = "ADDING_CLASS";
    public static final String CPLC_R = "INTERESTING";
    static final String[] CPLC_ITEMS =
            Arrays.stream(new String[]{CPLC_IL, CPLC_IA, CPLC_SO, CPLC_AC, CPLC_R}).sorted().toArray(String[]::new);
    private static GlobalConsole console = new GlobalConsole();
    private final RSyntaxTextArea log;
    private final JButton clean; //assigned by inherited listner
    private JList<String> verboseCplc;
    private final JFrame frame;
    private boolean first = true;

    public GlobalConsole() {
        JButton tmpClean;
        RSyntaxTextArea tmpLog;
        JFrame tmpFrame;
        Logger.getLogger().disableGuiLogging();
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                verboseCplc = new JList(CPLC_ITEMS);
                tmpLog = new RSyntaxTextArea();
                tmpLog.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS);
                tmpClean = new JButton("Clean log");
                tmpFrame = new JFrame("Log console");
                tmpFrame.setLayout(new BorderLayout());
                tmpFrame.add(new JScrollPane(tmpLog));
                verboseCplc.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                verboseCplc.addListSelectionListener(actionEvent -> {
                    if (!actionEvent.getValueIsAdjusting()) {
                        onListEverything();
                    }
                });

                tmpFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowActivated(WindowEvent windowEvent) {
                        if (System.getProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT) == null ||
                                System.getProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT).trim().isEmpty()) {
                            verboseCplc.clearSelection();
                            System.clearProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT);
                            setCplcVerbosityTooltip();
                        } else {
                            String[] futureSelection = System.getProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT).split("\\s*,\\s*");
                            List<Integer> indexes = new ArrayList<>(futureSelection.length);
                            for (String s : futureSelection) {
                                int index = Arrays.binarySearch(CPLC_ITEMS, s);
                                if (index >= 0) {
                                    indexes.add(index);
                                }
                            }
                            if (indexes.isEmpty()) {
                                verboseCplc.clearSelection();
                                System.clearProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT);
                            } else {
                                verboseCplc.setSelectedIndices(indexes.stream().mapToInt(Integer::intValue).toArray());
                            }
                            setCplcVerbosityTooltip();
                        }
                    }
                });
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
                JPanel verbosity = new JPanel(new BorderLayout());
                verbosity.add(verbose, BorderLayout.NORTH);
                JScrollPane verbosityScroller = new JScrollPane(verboseCplc);
                verbosityScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                verbosityScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                verbosityScroller.setMaximumSize(new Dimension(verbosityScroller.getMaximumSize().width, 20));
                verbosityScroller.setPreferredSize(new Dimension(verbosityScroller.getPreferredSize().width, 20));
                verbosityScroller.setMinimumSize(new Dimension(verbosityScroller.getMaximumSize().width, 20));
                verbosity.add(verbosityScroller, BorderLayout.SOUTH);
                p.add(verbosity, BorderLayout.EAST);
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

    private void onListEverything() {
        if (verboseCplc.getSelectedValuesList() == null || verboseCplc.getSelectedValuesList().isEmpty()) {
            System.clearProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT);
            setCplcVerbosityTooltip();
        } else {
            System.setProperty(
                    CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT, verboseCplc.getSelectedValuesList().stream().collect(Collectors.joining(","))
            );
            setCplcVerbosityTooltip();
        }
    }

    private void setCplcVerbosityTooltip() {
        verboseCplc.setToolTipText("CPLC verbosity: " + System.getProperty(CPLC_DUPLICATED_CODE_VERBOSITY_CONSTANT, "none"));
    }

    private String[] getAllLexers() throws IllegalAccessException {
        List<String> r = new ArrayList<>();
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
