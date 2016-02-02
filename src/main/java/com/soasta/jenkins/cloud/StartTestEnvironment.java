/**
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud;

import hudson.Extension;
import hudson.util.FormValidation;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.soasta.jenkins.AbstractCloudTestBuilderDescriptor;


public class StartTestEnvironment extends CloudCommandBase
{
  
  @DataBoundConstructor
  public StartTestEnvironment(String url, String cloudTestServerID, String name, String ctmUserName, String ctmPassword)
  {
    super(url, cloudTestServerID, name);
    CTMInfo info = new CTMInfo().setCtmPassword(ctmPassword).setCtmUserName(ctmUserName);
    // set the CTM url to be used by scommand. 
    super.setCTMInfo(info);
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start Test Environment";
      }

      /**
       * Called automatically by Jenkins whenever the "EnvironmentName"
       * field is modified by the user.
       * @param value the new composition name.
       */
      public FormValidation doCheckEnvironmentName(@QueryParameter String value) 
      {
          if (value == null || value.trim().isEmpty()) 
          {
              return FormValidation.error("Test Environment Name is Required");
          } 
          else 
          {
              return FormValidation.ok();
          }
      }
  }
  
  @Override
  public String getCommand()
  {
    return "start-env";
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.READY;
  }

}
