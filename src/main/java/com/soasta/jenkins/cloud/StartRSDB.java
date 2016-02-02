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

public class StartRSDB extends CloudCommandBase
{
  private final String name;
  
  @DataBoundConstructor
  public StartRSDB(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);
    this.name = name;
  }
  
  public String getName() 
  {
	  return name;
  }
  
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    return true;
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start RSDB";
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
    return "start-rsdb";
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.READY;
  }

}
