package org.jrd.frontend.NewFsVmFrame;

import org.jrd.backend.data.VmManager;

import javax.swing.JOptionPane;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NewFsVmController {

    NewFsVmView newConnectionView;
    VmManager vmManager;

    public NewFsVmController(NewFsVmView newConnectionView, VmManager vmManager){
        this.newConnectionView = newConnectionView;
        this.vmManager = vmManager;
        newConnectionView.setAddButtonListener(e -> addFsVm());
    }

    private void addFsVm(){
        String cp = newConnectionView.getCP();
        String name = newConnectionView.getNameHelper();
        if (cp.isEmpty()) {
            JOptionPane.showMessageDialog(newConnectionView, "CP is Empty.", " ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] cpe = cp.split(File.pathSeparator);
        List<File> r = new ArrayList<>(cpe.length);
        for(String ccp: cpe){
            File f = new File(ccp);
            if (!f.exists()){
                JOptionPane.showMessageDialog(newConnectionView, ccp+" does not exists", " ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            r.add(f);
        }
        vmManager.createFsVM(r,name);
        newConnectionView.dispose();
    }



}
