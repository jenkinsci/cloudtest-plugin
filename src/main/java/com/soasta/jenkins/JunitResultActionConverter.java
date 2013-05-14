//**************************************************
//
//  Copyright 2013 SOASTA, Inc.
//  All rights reserved.
//  Proprietary and confidential.
//
//  File:  MessageConverter.java
//  Contains the MessageConverter class.
//
//  This file contains the MessageConverter class.
//
//**************************************************

package com.soasta.jenkins;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class JunitResultActionConverter implements Converter
{
  public boolean canConvert(Class clazz)
  {
    return clazz.equals(JunitResultAction.class);
  }

  // Convert this JunitResultAction object to XML
  public void marshal(Object value, HierarchicalStreamWriter writer,
    MarshallingContext context)
  {
    // Create JunitResultAction object
    JunitResultAction resultAction = (JunitResultAction) value;

    writer.startNode("resultID");
    writer.setValue(resultAction.getResultID());
    writer.endNode();

    writer.startNode("url");
    writer.setValue(resultAction.getUrl());
    writer.endNode();

    writer.startNode("isPlayList");
    writer.setValue(Boolean.toString(resultAction.isPlayList()));
    writer.endNode();

    writer.startNode("exception");
    writer.setValue(resultAction.getExceptionMessage());
    writer.endNode();
    
    writer.startNode("messages");
    // sets the messages one at a time
    if (resultAction.getMessages() != null &
      resultAction.getErrorMessages().isEmpty())
    {
      for (Message message : resultAction.getMessages())
      {
        writer.startNode("message");
        context.convertAnother(message);
        writer.endNode();
      }
    }
    else if (resultAction.getErrorMessages() != null &&
      !resultAction.getErrorMessages().isEmpty())
    {
      // if this list is not null then this is from an older
      // version.  Store it in the new Message object version.
      for (String error : resultAction.getErrorMessages())
      {
        writer.startNode("message");
        context.convertAnother(new Message(null, error));
        writer.endNode();
      }
    }
    writer.endNode();
  }

  // Convert the JunitResultAction XML to a JunitResultAction object
  public Object unmarshal(HierarchicalStreamReader reader,
    UnmarshallingContext context)
  {
    // Create JunitResultAction object
    JunitResultAction resultAction = new JunitResultAction();

    // Traverse the XML tree
    while (reader.hasMoreChildren())
    {
      reader.moveDown();
      String nodeName = reader.getNodeName();
      if ("m__resultID".equals(nodeName) || "resultID".equals(nodeName))
      {
        resultAction.setResultID(reader.getValue());
      }
      else if ("m__url".equals(nodeName) || "url".equals(nodeName))
      {
        resultAction.setUrl(reader.getValue());
      }
      else if ("m__isPlayList".equals(nodeName) || "isPlayList".equals(nodeName))
      {
        resultAction.setPlayList(Boolean.valueOf(reader.getValue()));
      }
      else if ("m__exception".equals(nodeName) || "exception".equals(nodeName))
      {
        resultAction.setExceptionMessage(reader.getValue());
      }
      else if ("messages".equals(nodeName))
      {
        // loop through all the message-xml
        while (reader.hasMoreChildren())
        {
          reader.moveDown();
          if ("message".equals(reader.getNodeName()))
          {
            Message message = (Message) context.convertAnother(resultAction, Message.class);
            resultAction.addMessage(message);
          }
          reader.moveUp();
        }

      }
      // Convert the errorMessages String objects to
      // messages Message objects.
      else if ("m__errorMessages".equals(nodeName))
      {
        // loop through all the string-xml
        while (reader.hasMoreChildren())
        {
          reader.moveDown();
          if ("string".equals(reader.getNodeName()))
          {
            resultAction.addErrorMessage(reader.getValue());
          }
          reader.moveUp();
        }
      }
      reader.moveUp();
    }

    return resultAction;
  }
}