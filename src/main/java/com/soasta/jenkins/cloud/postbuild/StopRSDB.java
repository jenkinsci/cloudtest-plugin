/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

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
import com.soasta.jenkins.cloud.CloudStatus;

import hudson.tasks.Builder;

public class StopRSDB extends CloudCommandBasePostBuild
{
  @DataBoundConstructor
  public StopRSDB(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);   
  }
  
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudCommandPostBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Stop RSDB";
      }
  }

  @Override
  public String getCommand()
  {
    return "terminate-rsdb";
  }
  
  @Override
  public int getDefaultTimeout() 
  {
    return 1200;
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.TERMINATED;
  }

}
