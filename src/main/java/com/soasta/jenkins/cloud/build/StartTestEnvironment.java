/**
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.build;

import hudson.Extension;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.soasta.jenkins.cloud.CloudStatus;


public class StartTestEnvironment extends CloudCommandBaseBuild
{
  
  @DataBoundConstructor
  public StartTestEnvironment(String url, String cloudTestServerID, String name, int timeOut)
  {
    super(url, cloudTestServerID, name, timeOut);
  }
  
  @Extension
  @Symbol("startTestEnvironment")
  public static class DescriptorImpl extends AbstractCloudCommandBuildDescriptor
  {
      @Override
      public String getDisplayName()
      {
          return "Start Test Environment";
      }
  }
  
  @Override
  public String getCommand()
  {
    return "start-env";
  }
  
  @Override
  public int getDefaultTimeout() 
  {
    return 1200;
  }

  @Override
  public CloudStatus getSuccessStatus()
  {
    return CloudStatus.READY;
  }

}
