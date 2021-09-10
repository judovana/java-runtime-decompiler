package org.jrd.frontend.frame.overwrite;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.io.File;

/**
 *
 * @author jvanek
 */
public class FileToClassValidator implements DocumentListener {

    private final JLabel output;
    private final JTextField file;
    private final JTextField clazz;

    public FileToClassValidator(JLabel validation, JTextField filePath, JTextField className) {
        this.output = validation;
        this.file = filePath;
        this.clazz = className;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        work();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        work();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        work();
    }

    private void work() {
        StringAndScore r = validate(clazz.getText(), file.getText());
        output.setForeground(Color.green);
        if (r.score > 0) {
            output.setForeground(Color.orange);
        }
        if (r.score > 9) {
            output.setForeground(Color.red);
        }
        output.setText(r.message);
    }

    public static StringAndScore validate(String clazz, String file) {
        int score = 0;
        StringBuilder message = new StringBuilder();
        File f = new File(file);
        if (!f.isFile()) {
            score += 10;
            message.append("Not a file! ");
        }
        if (!f.getName().endsWith(".class")) {
            score++;
            message.append("Not a ending with class! ");
        }
        String[] pathHunks = f.getAbsolutePath().replace(".class", "").split("[\\.\\\\/]");
        String[] classHunks = clazz.split("[\\.\\\\/]");
        if (!pathHunks[pathHunks.length - 1].equals(classHunks[classHunks.length - 1])) {
            score += 10;
            message.append("File name and class name do not match! ");
        }
        //skipping name
        int i1 = pathHunks.length - 1;
        int i2 = classHunks.length - 1;
        while (true) {
            i1--;
            i2--;
            if (i1 < 0 || i2 < 0) {
                break;
            }
            if (!pathHunks[i1].equals(classHunks[i2])) {
                score++;
                message.append("Class package do not match directories. ");
                break;
            }
        }
        if (message.length() == 0) {
            message.append("file x class looks OK!");
        }
        return new StringAndScore(score, message.toString());
    }

    public static class StringAndScore {

        private final int score;
        private final String message;

        public StringAndScore(int score, String message) {
            this.score = score;
            this.message = message;
        }

        public int getScore() {
            return score;
        }

        public String getMessage() {
            return message;
        }
    }

}
