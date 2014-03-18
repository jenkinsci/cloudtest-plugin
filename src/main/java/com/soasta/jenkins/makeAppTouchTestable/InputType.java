/*
 * Copyright (c) 2012-2014, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.makeAppTouchTestable;

public enum InputType {
  
    APP ("-appfile"),
    IPA ("-ipa"),
    PROJECT ("-project");
  
    String inputType;
  
    private InputType(String inputType) {
        this.inputType = inputType;
    }
    
    public String getInputType() {
        return inputType;
    }
  
    public static InputType getInputType(String inputType) {
        // In case there were no input type provided, the default is
        // always returned: PROJECT.
        if (inputType == null) {
            return InputType.PROJECT;
        }
        
        try {
            return InputType.valueOf(inputType);
        }
        catch (IllegalArgumentException e) {
            // The input type passed in was bad so we will
            // use the default type instead.
            return InputType.PROJECT;
        }
    }
}
