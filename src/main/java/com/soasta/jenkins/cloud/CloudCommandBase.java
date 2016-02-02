package com.soasta.jenkins.cloud;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.soasta.jenkins.AbstractSCommandBuilder;
import com.soasta.jenkins.JunitResultPublisher;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.*;

public abstract class CloudCommandBase extends AbstractSCommandBuilder
{
  private final String name;

  public CloudCommandBase(String url, String cloudTestServerID, String name)
  {
    super(url, cloudTestServerID);
    this.name = name;
  }
  
  
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    String command = getCommand();
    // Create a unique sub-directory to store all test results.
    String resultsDir = "." + command;
    
    // Split by newline.
     EnvVars envs = build.getEnvironment(listener);
    //String additionalOptionsExpanded = additionalOptions == null ? 
      //  null : envs.expand(additionalOptions);
    
    //String[] options = additionalOptionsExpanded == null ?
      //  null : new QuotedStringTokenizer(additionalOptionsExpanded).toArray();

      System.out.println("RUNNING COMMAND: " +command + " for item : " + name);
      ArgumentListBuilder args = getSCommandArgs(build, listener);
      
      args.add("cmd=" + command, "wait=true", "format=xml")
          .add("name=" + name);
      
      
      String fileName = name + ".xml";

      // Strip off any leading slash characters (composition names
      // will typically be the full CloudTest folder path).
      if (fileName.startsWith("/")) {
          fileName = fileName.substring(1);
      }

      // Put the file in the test results directory.
      fileName = resultsDir + File.separator + fileName;
      
      FilePath xml = new FilePath(build.getWorkspace(), fileName);
      
      // Make sure the directory exists.
      xml.getParent().mkdirs();

      // Run it!
      int exitCode = launcher
          .launch()
          .cmds(args)
          .pwd(build.getWorkspace())
          .stdout(xml.write())
          .stderr(listener.getLogger())
          .join();

      if (xml.length() == 0) {
          // SCommand did not produce any output.
          // This should never happen, but just in case...
          return false;
      }
    // hard coded to get to pass
    try
    {
      return isSucessful(xml.readToString());
    }
    catch (Exception e)
    {
      e.printStackTrace();
      
      return false;
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
  

}
