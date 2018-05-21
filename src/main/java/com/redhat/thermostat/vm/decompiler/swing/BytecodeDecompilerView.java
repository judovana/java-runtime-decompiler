package com.redhat.thermostat.vm.decompiler.swing;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

/**
 * Class that creates the view of tab for thermostat GUI
 */
public class BytecodeDecompilerView {

    private JPanel BytecodeDecompilerPanel;

    JSplitPane splitPane;
    private JPanel leftMainPanel;
    private JPanel rightMainPanel;
    private JScrollPane rightScrollPanel;
    private JScrollPane leftScrollPanel;
    private JList<String> listOfClasses;
    private RTextScrollPane bytecodeScrollPane;
    private RSyntaxTextArea bytecodeSyntaxTextArea;
    private ActionListener bytesActionListener;
    private ActionListener classesActionListener;
    private ActionListener agentActionListener;

    private boolean splitPaneFirstResize = true;

    /**
     * Constructor creates the graphics and adds the action listeners.
     */

    public JPanel getBytecodeDecompilerPanel(){
        return BytecodeDecompilerPanel;
    }


    public BytecodeDecompilerView(){

        BytecodeDecompilerPanel = new JPanel(new BorderLayout());

        listOfClasses = new JList<>();
        listOfClasses.setFixedCellHeight(20);
        listOfClasses.setListData(new String[]{"Click button above marked", "refresh loaded class list", "in order to start."});
        listOfClasses.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final String name = listOfClasses.getSelectedValue();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                           ActionEvent event = new ActionEvent(this, 1, name);

                            bytesActionListener.actionPerformed(event);
                            
                        } catch (Throwable t) {
                            // log exception
                        }
                        return null;
                    }
                }.execute();
            
        }});

        JButton topButton = new JButton("Refresh loaded classes list");
        topButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            ActionEvent event = new ActionEvent(this, 2, null);


                            classesActionListener.actionPerformed(event);
                        } catch (Throwable t) {
                            // log exception
                        }
                        return null;
                    }
                }.execute();
            }
        }
        );

        bytecodeSyntaxTextArea = new RSyntaxTextArea();
        bytecodeSyntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        bytecodeSyntaxTextArea.setCodeFoldingEnabled(true);
        bytecodeScrollPane = new RTextScrollPane(bytecodeSyntaxTextArea);

        leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new BorderLayout());
        leftMainPanel.setBorder(new EtchedBorder());

        JPanel topButtonPanel = new JPanel();

        rightMainPanel = new JPanel();
        rightMainPanel.setLayout(new BorderLayout());
        rightMainPanel.setBorder(new EtchedBorder());

        topButtonPanel.setLayout(new BorderLayout());
        topButtonPanel.add(topButton, BorderLayout.WEST);

        leftScrollPanel = new JScrollPane(leftMainPanel);
        leftScrollPanel.getVerticalScrollBar().setUnitIncrement(20);

        leftMainPanel.add(listOfClasses);
        rightMainPanel.add(bytecodeScrollPane);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftScrollPanel, rightMainPanel);

        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if(splitPaneFirstResize){
                    splitPane.setDividerLocation(0.5);
                    splitPaneFirstResize = false;
                }
            }
        });

        BytecodeDecompilerPanel.add(topButtonPanel, BorderLayout.NORTH);
        BytecodeDecompilerPanel.add(splitPane, BorderLayout.CENTER);

        BytecodeDecompilerPanel.setVisible(true);

    }

    /**
     * Sets the class list into the JList.
     *
     * @param classesToReload array of classes to give into JList
     */
    public void reloadClassList(String[] classesToReload) {
        final String[] data = classesToReload;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listOfClasses.setListData(data);
            }
        });

    }

    /**
     * Sets the decompiled code into JTextArea
     *
     * @param decompiledClass String of source code of decompiler class
     */
    public void reloadTextField(String decompiledClass) {
        final String data = decompiledClass;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                bytecodeSyntaxTextArea.setText(data);
            }
        });
    }

    public void setClassesActionListener(ActionListener listener) {
        classesActionListener = listener;
    }


    public void setBytesActionListener(ActionListener listener) {
        bytesActionListener = listener;
    }

    public void setHaltActionListener(ActionListener listener) {
        agentActionListener = listener;
    }

    public void haltServer(){
        agentActionListener.actionPerformed(new ActionEvent(this, 3, null));
    }

    /**
     * Creates a warning table in case of error.
     * @param msg message
     */
    public void handleError(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                //JOptionPane.showMessageDialog(getUiComponent().getParent(), msg, " ", JOptionPane.WARNING_MESSAGE);
            }

        });
    }

}
