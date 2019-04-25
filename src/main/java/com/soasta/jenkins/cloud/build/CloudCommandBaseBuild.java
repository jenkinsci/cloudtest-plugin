package com.soasta.jenkins.cloud.build;

import java.io.File;
import java.io.IOException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import com.soasta.jenkins.cloud.CloudCommandBuilder;
import com.soasta.jenkins.cloud.CloudStatus;

import javax.xml.parsers.*;
import java.io.*;

import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;

public abstract class CloudCommandBaseBuild extends Builder implements SimpleBuildStep
{
  private final String name;
  private final String cloudTestServerID;
  private final String url;
  private final int timeOut; 

  public CloudCommandBaseBuild(String url, String cloudTestServerID, String name)
  {
    this.name = name;
    this.cloudTestServerID = cloudTestServerID;
    this.url = url;
    this.timeOut = -1;
  }
  
  public CloudCommandBaseBuild(String url, String cloudTestServerID, String name, int timeOut)
  {
    this.name = name;
    this.cloudTestServerID = cloudTestServerID;
    this.url = url;
    // timeout wasn't specifed in the UI, use a default value
    if (timeOut == 0)
    {
      this.timeOut = getDefaultTimeout();
    }
    else
    {
      this.timeOut = timeOut;
    }
  }
  
  public final String getUrl()
  {
    return this.url;
  }
  
  public final String cloudTestServerID()
  {
    return this.cloudTestServerID;
  }
  
  public String getCloudTestServerID()
  {
    return cloudTestServerID;
  }
  
  public final String getName()
  {
    return name;
  }
  
  public final int getTimeOut()
  {
    return timeOut;
  }
  
  @Override 
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    String command = getCommand();
    // Create a unique sub-directory to store all test results.
      String resultsDir = "." + command;
      // set the basic commands. 
      ArgumentListBuilder args =
      new CloudCommandBuilder()
      .setWorkspace(workspace)
      .setUrl(url)
      .setListener(listener)
      .setCloudTestServerID(cloudTestServerID)
      .build();
      
      args.add("cmd=" + command, "wait=true", "format=xml")
          .add("name=" + name);
      
      if (timeOut >= 0)
      {
        args.add("timeout=" + timeOut);
      }
      
      String fileName = name + ".xml";

      // Strip off any leading slash characters (composition names
      // will typically be the full CloudTest folder path).
      if (fileName.startsWith("/")) {
          fileName = fileName.substring(1);
      }

      // Put the file in the test results directory.
      fileName = resultsDir + File.separator + fileName;
      
      FilePath xml = new FilePath(workspace, fileName);
      
      // Make sure the directory exists.
      xml.getParent().mkdirs();

      // Run it!
      launcher.launch()
          .cmds(args)
          .pwd(workspace)
          .stdout(xml.write())
          .stderr(listener.getLogger())
          .join();

      if (xml.length() == 0)
      {
          // SCommand did not produce any output.
          // This should never happen, but just in case...
          return;
      }
      
      try
      {
        isSucessful(xml.readToString());
      }
      catch (Exception e)
      {
        e.printStackTrace();
        
        return;
      }
  }
  
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FilePath filePath = build.getWorkspace();
    if(filePath == null) {
        return false;
    } else {
        perform(build, filePath, launcher, listener);
        return true;
    }
  }
  /**
   * Returns the specific cloud command. E.g 'start-grid', 'start-env', 'terminate-env'
   * @return
   */
  public abstract String getCommand();
  
  /**
   * Returns the expected str for a sucessful start / terminate. 
   * @return
   */
  public abstract CloudStatus getSuccessStatus();
  
  public abstract int getDefaultTimeout();
  /**
   * Parses the output xml for a success code. 
   * @param xml
   * @return
   */
  private boolean isSucessful(String xml) throws Exception
  {
    DocumentBuilderFactory factory =
    DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
    
    NodeList list = doc.getElementsByTagName("Status");
    
    if (list != null && list.getLength() > 0)
    {
      String successCriteria = getSuccessStatus().name();
      return list.item(0).getTextContent().equals(successCriteria);
    }
    
    return false;
  }
  
  @Override
  public BuildStepMonitor getRequiredMonitorService()
  {
    return BuildStepMonitor.NONE;
  }

}
