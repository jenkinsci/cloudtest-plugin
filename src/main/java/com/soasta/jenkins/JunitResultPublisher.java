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
              // Initialize the values that will be taken out
              String type = null;      // The type of message (i.e. "validation-pass").
              String path = null;  // The full path of the clip the message is from.
              String message = null;   // The result message itself.
              Message resultsMessage = null;    // The message object that will be created from the strings: type and message.
              for (int i = 0; i < messageNodes.getLength(); i++)
              {
                // Checks to see if there's a type attribute associated with this message
                // tag. This check is to make the code is backwards compatible with older
                // versions of CloudTest, where the result messages did not contain type
                // attributes.
                type = messageNodes.item(i).hasAttributes() && messageNodes.item(i).getAttributes().getNamedItem(MESSAGE_CLIP_TYPE) != null ?
                  messageNodes.item(i).getAttributes().getNamedItem(MESSAGE_CLIP_TYPE).getTextContent() : null;
                path = messageNodes.item(i).hasAttributes() && messageNodes.item(i).getAttributes().getNamedItem(MESSAGE_PATH) != null ?
                  messageNodes.item(i).getAttributes().getNamedItem(MESSAGE_PATH).getTextContent() : null;
                message = messageNodes.item(i).getTextContent();
                
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