/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.soasta.jenkins.AbstractCloudTestBuilderDescriptor;

import hudson.tasks.Builder;

public class StopGrid extends CloudCommandBase
{
  private final String name;
  
  @DataBoundConstructor
  public StopGrid(String url, String cloudTestServerID, String name)
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
          return "Stop Grid";
      }

      /**
       * Called automatically by Jenkins whenever the "Grid Name"
       * field is modified by the user.
       * @param value the new composition name.
       */
      public FormValidation doCheckName(@QueryParameter String value) 
      {
          if (value == null || value.trim().isEmpty()) 
          {
              return FormValidation.error("Grid Name is Required");
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
    return "terminate-grid";
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.TERMINATED;
  }

}
