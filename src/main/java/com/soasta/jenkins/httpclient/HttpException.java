package com.soasta.jenkins.httpclient;

public class HttpException extends RuntimeException
{
  
  private static final long serialVersionUID = 1L;
  private final int m_statusCode;
  private final String m_body; 
  
  public HttpException(int statusCode, String body)
  {
    m_statusCode = statusCode;
    m_body = body;
  }
  
  @Override
  public String getMessage()
  {
    return "HTTP Request Failed. Code [" + m_statusCode + "] . Message [" + getBody() +"]";
  }
  
  private String getBody()
  {
    if (m_body != null)
    {
      if (m_body.length() < 50)
      {
        return m_body;
      }
      else
      {
        return m_body.subSequence(0, 50) + "...";
      }
    }
    else
    {
      return "...No Response Body...";
    }
  }
}
