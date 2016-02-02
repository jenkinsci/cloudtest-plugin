/*
 * Copyright (c) 2015, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

public abstract class CloudCommandBase extends AbstractSCommandBuilder
{
  protected static final Logger LOGGER = Logger.getLogger(CloudCommandBase.class.getName());
  
  private final String name;
  // TODO we want the additional functionality, e.g the wait, etc. 
  
  public CloudCommandBase(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID);
    this.name = name;
  }
  
  public String getName()
  {
    return name;
  }
  
  abstract protected String getCloudCommand();
  
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException
  {
    // Create a unique sub-directory to store all test results.
    String resultsDir = "." + getClass().getName();
    ArgumentListBuilder args = getSCommandArgs(build, listener);
    
    // we need to make the output file for the results from the launch 
    
    String fileName = name + ".xml";
    fileName = resultsDir + File.separator + fileName;
    LOGGER.info("Creating file: " + fileName );
    FilePath xml = new FilePath(build.getWorkspace(), fileName);
    // Make sure the directory exists.
    xml.getParent().mkdirs();
    
    // add the cloud command 
    args.add("cmd=" + getCloudCommand());
    args.add("name=\"" + name + "\"");
    
    LOGGER.info("Running SCommand " + args.toStringWithQuote());
    // Run it!
    int exitCode = launcher
        .launch()
        .cmds(args)
        .pwd(build.getWorkspace())
        .stdout(xml.write())
        .stderr(listener.getLogger())
        .join();

    if (xml.length() == 0)
    {
        // SCommand did not produce any output.
        // This should never happen, but just in case...
        return false;
    }
    
    return exitCode == 0;
  } 

}

