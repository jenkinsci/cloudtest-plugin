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

public class MessageConverter implements Converter
{
  // Can only convert Message objects
  public boolean canConvert(Class clazz)
  {
    return clazz.equals(Message.class);
  }

  // Take the Message object and change it to a Message-XML
  public void marshal(Object value, HierarchicalStreamWriter writer,
    MarshallingContext context)
  {
    Message message = (Message) value;

    writer.startNode("message");

    writer.startNode("messageType");
    writer.setValue(message.getType());
    writer.endNode();

    writer.startNode("messageClipName");
    writer.setValue(message.getClipName());
    writer.endNode();
    
    writer.startNode("messageContent");
    writer.setValue(message.getContent());
    writer.endNode();

    writer.endNode();
  }

  // Convert the XML to a Message object.
  public Object unmarshal(HierarchicalStreamReader reader,
    UnmarshallingContext context)
  {
    Message message = new Message();

    while (reader.hasMoreChildren())
    {
      reader.moveDown();
      String nodeName = reader.getNodeName();
      if ("messageType".equals(nodeName))
      {
        message.setType(reader.getValue());
      }
      else if ("messageClipName".equals(nodeName))
      {
        message.setClipName(reader.getValue());
      }
      else if ("messageContent".equals(nodeName))
      {
        message.setContent(reader.getValue());
      }
      reader.moveUp();
    }

    return message;
  }
}