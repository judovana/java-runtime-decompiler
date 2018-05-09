/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.swing;

import com.redhat.thermostat.vm.decompiler.data.VmManager;
import java.util.ArrayList;
import workers.VmRef;

/**
 *
 * @author pmikova
 */
public class MainFrameView {
    
    public MainFrameView(VmManager manager){
    
    // create Main frame 
    
    // end
    ArrayList<VmRef> vmList = manager.getAllVm();
    // nejaky forcyklus ktery vypise do listu jmena (a pid) tech virtualek a vytvori ten list
    
    // listener na kazdy item listu Vmek, po kliknuti identifikuje vm a:
    
            //VmDecompilerInformationController controller  = new VmDecompilerInformationController(view, ref, manager); 
            // tohle uz by melo vytvorit teoreticky i ten view a musi se zajistit, ze se ten view zobrazi az pak (fakt netusim jak se to dela)
    
    //
    
    }
    
}
