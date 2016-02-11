/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.build;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.soasta.jenkins.cloud.CloudStatus;
import com.soasta.jenkins.cloud.postbuild.CloudCommandBasePostBuild;


public class StartRSDB extends CloudCommandBaseBuild
{
  @DataBoundConstructor
  public StartRSDB(String url, String cloudTestServerID, String name, int timeOut)
  {
    super(url, cloudTestServerID, name, timeOut);
  }
 
  @Extension
  public static class DescriptorImpl extends AbstractCloudCommandBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start RSDB";
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
