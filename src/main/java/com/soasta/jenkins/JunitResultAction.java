/*
 * Copyright (c) 2011-2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import hudson.tasks.junit.TestAction;

public class JunitResultAction extends TestAction
{
  @XStreamAlias("resultID")
  private String m_resultID = "";

  @XStreamAlias("url")
  private String m_url;

  @XStreamAlias("isPlayList")
  private boolean m_isPlayList = false;

  @XStreamAlias("exception")
  private String m_exception = "";

  // For backward-compatibility only, hence not serializing
  private transient List<String> m_errorMessages;

  @XStreamAlias("messages")
  private List<Message> m_messages;

  public String getIconFileName()
  {
    return null;
  }

  public String getUrlName()
  {
    return "Result Dashboard URL Name";
  }

  public String getDisplayName()
  {
    return "Result Dashboard Display Name";
  }

  public String getResultID()
  {
    return m_resultID;
  }

  public void setResultID(String resultID)
  {
    m_resultID = resultID;
  }

  public String getUrl()
  {
    return m_url;
  }

  public void setUrl(String url)
  {
    m_url = url;
  }

  public boolean isPlayList()
  {
    return m_isPlayList;
  }

  public void setPlayList(boolean isPlayList)
  {
    m_isPlayList = isPlayList;
  }

  public String getExceptionMessage()
  {
    return m_exception.replace("\n", "<br>");
  }

  public void setExceptionMessage(String exception)
  {
    m_exception = exception;
  }

  public boolean isException()
  {
    return !m_exception.equals("");
  }
  
  public List<String> getErrorMessages()
  {
    return m_errorMessages;
  }
  
  public void setErrorMessages(List<String> errorMessages)
  {
    m_errorMessages = errorMessages;
  }

  public void addErrorMessage(String errorMessage)
  {
    m_errorMessages.add(errorMessage);
  }

  public List<Message> getMessages()
  {
    return m_messages;
  }

  public void setMessages(List<Message> messages)
  {
    m_messages = messages;
  }

  public void addMessage(Message message)
  {
    m_messages.add(message);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    m_errorMessages = new ArrayList<String>();
  }
}
