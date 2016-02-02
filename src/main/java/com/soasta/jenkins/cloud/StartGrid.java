/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud;

import hudson.Extension;
import hudson.util.FormValidation;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.soasta.jenkins.AbstractCloudTestBuilderDescriptor;

public class StartGrid extends CloudCommandBase
{
  private final String name;
  
  @DataBoundConstructor
  public StartGrid(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);
    this.name = name;
    
  }
  
  public String getName() 
  {
    return name;
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start Grid";
      }

      /**
       * Called automatically by Jenkins whenever the "tName"
       * field is modified by the user.
       */
      public FormValidation doCheckName(@QueryParameter String value) 
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
    return "start-grid";
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.READY;
  }

}
