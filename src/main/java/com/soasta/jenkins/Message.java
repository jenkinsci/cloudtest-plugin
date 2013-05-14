//**************************************************
//
//  Copyright 2013 SOASTA, Inc.
//  All rights reserved.
//  Proprietary and confidential.
//
//  File:  Message.java
//  Contains the Message class.
//
//  This file contains the Message class.
//
//**************************************************

package com.soasta.jenkins;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This class contains the results sent back from a composition run.
 * Currently, it holds the messages for things either of type 
 * "validation-pass" or "validation-fail".
 */
@XStreamAlias("message")
public class Message
{
  @XStreamAlias("messageType")
  private String m_type;
  @XStreamAlias("messageContent")
  private String m_content;
  
  public Message()
  {
  }
  
  public Message(String type, String content)
  {
    setType(type);
    m_content = content;
  }
  
  public void setContent(String content)
  {
    m_content = content;
  }
  
  public String getContent()
  {
    return m_content;
  }

  public void setType(String type)
  {
    if (type != null && type.length() > 0 )
    {
      m_type = type;
    }
    else
    {
      // Assume this is a failure if no type is passed in.  This is the
      // case for older versions of result messages where all messages were
      // failures and no attribute was associated with <message> tag.
      // (This check is only for back-ward compatibility purposes.)
      m_type = "validation-fail";
    }

    m_type = type;
  }

  public String getType()
  {
    return m_type;
  }

  @Override
  public String toString()
  {
    return m_type + ": " + m_content;
  }
}
  
