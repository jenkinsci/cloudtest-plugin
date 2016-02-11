/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.build;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.soasta.jenkins.cloud.CloudStatus;
import com.soasta.jenkins.cloud.postbuild.CloudCommandBasePostBuild;

public class StopRSDB extends CloudCommandBaseBuild
{
  
  @DataBoundConstructor
  public StopRSDB(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudCommandBuildDescriptor
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
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.TERMINATED;
  }

}
