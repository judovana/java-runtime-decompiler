package org.jrd.frontend.frame.filesystem;

import org.jrd.backend.data.VmManager;

import javax.swing.JOptionPane;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NewFsVmController {

    NewFsVmView newConnectionView;
    VmManager vmManager;

    public NewFsVmController(NewFsVmView newConnectionView, VmManager vmManager) {
        this.newConnectionView = newConnectionView;
        this.vmManager = vmManager;
        newConnectionView.setAddButtonListener(e -> addFsVm());
    }

    private void addFsVm() {
        String cp = newConnectionView.getCp();
        String name = newConnectionView.getNameHelper();
        boolean shouldBeSaved = newConnectionView.shouldBeSaved();
        List<File> r;

        try {
            r = cpToFiles(cp);
        } catch (InvalidClasspathException e) {
            JOptionPane.showMessageDialog(
                    newConnectionView, e.getMessage(), "Unable to create FS VM", JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        vmManager.createFsVM(r, name, shouldBeSaved);
        newConnectionView.dispose();
    }

    public static List<File> cpToFilesCaught(String input) {
        try {
            return cpToFiles(input);
        } catch (InvalidClasspathException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<File> cpToFiles(String input) throws InvalidClasspathException {
        String[] classpathElements = input.split(File.pathSeparator);

        if (classpathElements.length == 0) {
            throw new InvalidClasspathException("Classpath is empty.");
        }

        List<File> classpathList = new ArrayList<>(classpathElements.length);
        for (String element : classpathElements) {
            File file = new File(element);

            if (!file.exists()) {
                throw new InvalidClasspathException("File '" + file.getAbsolutePath() + "' does not exist.");
            } else {
                classpathList.add(file);
            }
        }

        return classpathList;
    }


    public static class InvalidClasspathException extends Exception {
        public InvalidClasspathException(String s, Throwable cause) {
            super(s, cause);
        }

        public InvalidClasspathException(String s) {
            super(s);
        }

    }
}
