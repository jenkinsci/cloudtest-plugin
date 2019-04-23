/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

import hudson.Extension;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import com.soasta.jenkins.cloud.CloudStatus;


public class StopGrid extends CloudCommandBasePostBuild
{
  @DataBoundConstructor
  public StopGrid(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);
  }
  
  @Extension
  @Symbol("stopGrid")
  public static class DescriptorImpl extends AbstractCloudCommandPostBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Stop Grid";
      } 
  }
  
  @Override
  public String getCommand()
  {
    return "terminate-grid";
  }
  
  @Override
  public int getDefaultTimeout() 
  {
    return 600;
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.TERMINATED;
  }

}
