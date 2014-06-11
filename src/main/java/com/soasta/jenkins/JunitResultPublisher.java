/*
 * Copyright (c) 2011-2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

@SuppressWarnings("deprecation")
public class JunitResultPublisher extends TestDataPublisher
{
  private static final String MESSAGE_PATH = "path";
  private static final String MESSAGE_CLIP_NAME = "clipName";
  private static final String MESSAGE_CLIP_TYPE = "type";
  
  private String urlOverride;

  /**
   * Called by Jenkins when the job is initialized.
   * @param the URL override setting from the job configuration (can be {@code null}).
   */
  @DataBoundConstructor
  public JunitResultPublisher(String urlOverride)
  {
    this.urlOverride = urlOverride;
  }

  /**
   * Called by Jenkins when rendering the job configuration page.
   * @return the current URL override (if any).
   */
  public String getUrlOverride()
  {
    return urlOverride;
  }

  @Override
  public TestResultAction.Data getTestData(AbstractBuild<?, ?> build,
    Launcher launcher, BuildListener listener, TestResult testResult) throws IOException
  {
    Data data = new Data();

    for (SuiteResult sr : testResult.getSuites())
    {
      JunitResultAction action = new JunitResultAction();

      // Get the local path of the JUnit XML file (may be on a slave node).
      String fileName = sr.getFile();

      if (fileName == null || fileName.length() <= 0)
      {
        listener.error("The selected JUnit XML file does not exist.  Skipping.");
        continue;
      }
      
      // Get local path of the build's workspace.
      String workspacePath = build.getWorkspace().getRemote();

      // Is the JUnit XML file in the workspace?
      if (fileName.startsWith(workspacePath))
      {
        // The JUnit XML file is in the workspace.
        try
        {
          // Load the JUnit XML file contents.  If the build is
          // on a slave, then this will load over the network.
          String relativePath = fileName.substring(workspacePath.length() + 1);
          String fileContent = build.getWorkspace().child(relativePath).readToString();
          Document junitXML = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(fileContent)));

          XPath xPath = XPathFactory.newInstance().newXPath();

          // Extract the CloudTest result ID (if any) from the JUnit XML file.
          String resultID = (String)xPath.evaluate("//testcase[1]/@resultID", junitXML, XPathConstants.STRING);

          // Did we find a result ID?
          if (resultID != null && resultID.trim().length() > 0)
          {
            // We found a result ID.
            String url;

            // Is the CloudTest URL specified at the job level?
            if (this.urlOverride != null && this.urlOverride.trim().length() > 0)
            {
              // The CloudTest URL is specified at the job level.
              // Use that.
              url = this.urlOverride;
            }
            else
            {
              // The CloudTest URL is not specified at the job level (normal case).
              // Extract it from the JUnit XML.
              url = (String)xPath.evaluate("//testsuite/@url", junitXML, XPathConstants.STRING);
            }

            // Extract the detailed error messages, if any.
            NodeList messageNodes = (NodeList)xPath.evaluate("//testcase[1]/messages/message", junitXML, XPathConstants.NODESET);
            List<Message> messages = new ArrayList<Message>();

            if (messageNodes != null)
            {
              Message resultsMessage = null;    // The message object that will be created from the strings: type and message.
              for (int i = 0; i < messageNodes.getLength(); i++)
              { 
                Node messageNode = messageNodes.item(i);
                
                // Checks to see if there are type and path attributes in this message.
                // This check ensures the code is backwards compatible with older
                // versions of CloudTest, where the result messages contained neither type
                // nor path attributes.
                String type = getNodeTextContent(messageNode, MESSAGE_CLIP_TYPE); // The type of message (i.e. "validation-pass").
                String path = getNodeTextContent(messageNode, MESSAGE_PATH); // The full path of the clip.
                
                if (path == null)
                {
                  // Get the name of the clip and use the clip name if clip path is not
                  // available.  This will also ensure backwards compatibility when a
                  // message's clip name was passed but the message's path was not.
                  path = getNodeTextContent(messageNode, MESSAGE_CLIP_NAME);
                }
                
                String message = messageNode.getTextContent(); // The result message itself.
                
                resultsMessage = new Message(type, path, message);
                // Add it to the list.  It is assumed that the messages being parsed
                // are already in chronological order.
                messages.add(resultsMessage);
              }
            }

            if (resultID.equals("NA"))
              action.setPlayList(true);

            // Store the result ID and URL in the Action object.
            // This will be used later on to render the test report.
            action.setResultID(resultID);
            action.setUrl(url);
            action.setMessages(messages);
          }

          data.addTestAction(sr.getCases().get(0).getId(), action);
        }
        catch (Exception e)
        {
          listener.error("File \"" + fileName + "\" could not be processed (" + e.getMessage() + ").  Skipping.");
        }
      }
      else
      {
        listener.error("File \"" + fileName + "\" does not appear to be in build workspace.  Skipping.");
      }
    }

    return data;
  }

  private String getNodeTextContent(Node node, String namedItem)
  {
    if (node == null || !node.hasAttributes())
    {
      return null;
    }
    
    Node nodeValue = node.getAttributes().getNamedItem(namedItem);
    
    if (nodeValue == null)
    {
      return null;
    }
    
    return nodeValue.getTextContent();
  }
  
  @Override
  public DescriptorImpl getDescriptor()
  {
    return (DescriptorImpl)super.getDescriptor();
  }

  private static class Data extends TestResultAction.Data
  {
    private Map<String,JunitResultAction> actions = new HashMap<String,JunitResultAction>();

    @Override
    public List<TestAction> getTestAction(TestObject testObject)
    {
      if (testObject instanceof CaseResult)
      {
        String id = testObject.getId();
        JunitResultAction a = actions.get(id);
        if (a!=null)
        {
          return Collections.<TestAction>singletonList(a);
        }
      }

      return Collections.emptyList();
    }

    public void addTestAction(String testObjectId, JunitResultAction action)
    {
      actions.put(testObjectId, action);
    }
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<TestDataPublisher>
  {
    /**
     * Called automatically by Jenkins when rendering the job configuration page.
     */
    @Override
    public String getDisplayName()
    {
      return "Include links to SOASTA CloudTest dashboards";
    }

    /**
     * Called automatically by Jenkins whenever the "urlOverride"
     * field is modified by the user.
     * @param value the new URL.
     */
    public FormValidation doCheckUrlOverride(@QueryParameter String value)
    {
      // Did the user enter a URL?
      if (value == null || value.trim().length() == 0)
      {
        // The user did not enter a URL.

        // This is always valid (and the usual case).
        return FormValidation.ok();
      }
      else
      {
        // The user entered a URL.
        // Make sure it's valid.

        try
        {
          // Attempt to parse the URL.
          new URL(value);

          // Success!
          return FormValidation.ok();
        }
        catch (Exception e)
        {
          // Failed to parse URL.
          return FormValidation.error("Invalid URL");
        }
      }
    }
  }
}
