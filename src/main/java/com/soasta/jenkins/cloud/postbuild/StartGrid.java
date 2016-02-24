/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import com.soasta.jenkins.cloud.CloudStatus;
import com.soasta.jenkins.cloud.postbuild.AbstractCloudCommandPostBuildDescriptor;

public class StartGrid extends CloudCommandBasePostBuild
{
  @DataBoundConstructor
  public StartGrid(String url, String cloudTestServerID, String name, int timeOut)
  {
    super(url, cloudTestServerID, name, timeOut);
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudCommandPostBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start Grid";
      }
  }
  
  @Override
  public String getCommand()
  {
    return "start-grid";
  }
  
  @Override
  public int getDefaultTimeout() 
  {
    return 600;
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.READY;
  }
}
