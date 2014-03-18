/*
 * Copyright (c) 2012-2014, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

public enum MATTInputType {
  
    APK ("-apk"),
    APP ("-appfile"),
    IPA ("-ipa"),
    PROJECT ("-project");
  
    String inputType;
  
    private MATTInputType(String inputType) {
        this.inputType = inputType;
    }
    
    public String getInputType() {
        return inputType;
    }
  
    public static MATTInputType getMATTInputType(String inputType) {
        // In case there were no input type provided, the default is
        // always returned: PROJECT.
        if (inputType == null) {
            return MATTInputType.PROJECT;
        }
        
        try {
            return MATTInputType.valueOf(inputType);
        }
        catch (IllegalArgumentException e) {
            // The input type passed in was bad so we will
            // use the default type instead.
            return MATTInputType.PROJECT;
        }
    }
}
