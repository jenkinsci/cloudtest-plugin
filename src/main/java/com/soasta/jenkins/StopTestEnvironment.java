/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.tasks.Builder;

public class StopTestEnvironment extends Builder
{
  private final String name;
  private final String cloudTestServerID; 
  private final String url; 
  
  @DataBoundConstructor
  public StopTestEnvironment(String url, String cloudTestServerID, String name)
  {
    this.url = url;
    this.cloudTestServerID = cloudTestServerID;
    this.name = name;
  }
  
  public String getName() 
  {
	  return name;
  }
  
  public String getUrl()
  {
	  return url;
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
          return "Stop Test Environment";
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

}
