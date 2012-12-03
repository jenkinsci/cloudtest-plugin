// **************************************************
//
// Copyright 2006-2011 SOASTA, Inc.
// All rights reserved.
// Proprietary and confidential.
//
// File: JunitResultAction.java
//
// This file is to extend to TestAction
//
// **************************************************

package com.soasta.jenkins;

import java.util.List;

import hudson.tasks.junit.TestAction;

public class JunitResultAction extends TestAction
{
  private String m_resultID = "";
  private String m_url;
  private boolean m_isPlayList = false;
  private String m_exception = "";
  private List<String> m_errorMessages;

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
}
