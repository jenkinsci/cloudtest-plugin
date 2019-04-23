/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

import hudson.Extension;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import com.soasta.jenkins.cloud.CloudStatus;

public class StopRSDB extends CloudCommandBasePostBuild
{
  @DataBoundConstructor
  public StopRSDB(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);   
  }
  
  @Extension
  @Symbol("stopRSDB")
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
