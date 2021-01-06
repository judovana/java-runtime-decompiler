package org.jrd.frontend.NewFsVmFrame;

import org.jrd.backend.data.Cli;
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
        String cp = newConnectionView.getCP();
        String name = newConnectionView.getNameHelper();
        if (cp.isEmpty()) {
            JOptionPane.showMessageDialog(newConnectionView, "CP is Empty.", " ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<File> r;
        try {
            r = cpToFiles(cp);
        } catch (ProbablyNotClassPathElementException ccp) {
            JOptionPane.showMessageDialog(newConnectionView, ccp + " does not exists", " ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        vmManager.createFsVM(r, name);
        newConnectionView.dispose();
    }

    public static List<File> cpToFilesCatched(String input) {
        try {
            return cpToFiles(input);
        } catch (ProbablyNotClassPathElementException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<File> cpToFiles(String input) throws ProbablyNotClassPathElementException {
        String[] cpElements = input.split(File.pathSeparator);
        if (cpElements.length == 0) {
            throw new ProbablyNotClassPathElementException("Second param was supposed to be PUC");
        }
        List<File> cp = new ArrayList<>(cpElements.length);
        for (String cpe : cpElements) {
            File f = new File(cpe);
            if (!f.exists()) {
                throw new ProbablyNotClassPathElementException(f.toString() + " does not exists");
            } else {
                cp.add(f);
            }
        }
        return cp;
    }


    public static class ProbablyNotClassPathElementException extends Exception {
        public ProbablyNotClassPathElementException(String s, Throwable cause) {
            super(s, cause);
        }

        public ProbablyNotClassPathElementException(String s) {
            super(s);
        }

    }
}
