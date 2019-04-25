/**
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.build;

import hudson.Extension;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.soasta.jenkins.cloud.CloudStatus;


public class StopTestEnvironment extends CloudCommandBaseBuild
{
  @DataBoundConstructor
  public StopTestEnvironment(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID, name);
  }
  
  @Extension
  @Symbol("stopTestEnvironment")
  public static class DescriptorImpl extends AbstractCloudCommandBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Stop Test Environment";
      }
  }
  
  @Override
  public String getCommand()
  {
    return "terminate-env";
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
