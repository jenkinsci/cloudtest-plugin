/**
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

import hudson.Extension;
import hudson.util.FormValidation;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.soasta.jenkins.AbstractCloudTestBuilderDescriptor;
import com.soasta.jenkins.cloud.CloudStatus;


public class StartTestEnvironment extends CloudCommandBasePostBuild
{
  
  @DataBoundConstructor
  public StartTestEnvironment(String url, String cloudTestServerID, String name, int timeOut)
  {
    super(url, cloudTestServerID, name, timeOut);
  }
  
  @Extension
  public static class DescriptorImpl extends AbstractCloudCommandPostBuildDescriptor
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
