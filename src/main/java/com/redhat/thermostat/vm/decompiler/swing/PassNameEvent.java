package com.redhat.thermostat.vm.decompiler.swing;

import java.util.EventObject;

/**
 * Implementation of ActionEvent is saving class name as its parameter
 * @param <T> enum type of the request
 */
public class PassNameEvent extends EventObject{
    
    private String className;
     private String actionId;
    
    /**
     * Constructor of the event
     * @param source source of the event
     * @param actionId id of action that will be performed
     * @param className name of class to get bytecode
     */
    public PassNameEvent(Object source, String actionId, String className) {
        super(source);
        this.className = className;
        this.actionId = actionId;
    }
    
    public String getClassName(){
        return className;
    }
    
      
    public PassNameEvent(Object source, String actionId) {
        super(source);
        if (actionId == null) {
            throw new NullPointerException();
        }
        this.actionId = actionId;
    }

    public String getActionId() {
        return actionId;
    }
    
}
