/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.thermostat.vm.decompiler.data;

import com.redhat.thermostat.vm.decompiler.swing.MainFrameView;

/**
 *
 * @author pmikova
 */
public class Main {
        
    
    public static void main(String[] args){
        VmManager manager = new VmManager();
        MainFrameView view = new MainFrameView(manager);
        
    }

}
