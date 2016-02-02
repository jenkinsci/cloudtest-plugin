package com.soasta.jenkins.cloud;

public class CTMInfo
{
  private static final String ctmURL = "https://cloudtestmanager.soasta.com/concerto/";
  private String ctmUserName;
  private String ctmPassword; 
  
  public String getCtmURL()
  {
    return ctmURL;
  }
  
  public String getCtmUserName()
  {
    return ctmUserName;
  }
  public CTMInfo setCtmUserName(String ctmUserName)
  {
    this.ctmUserName = ctmUserName;
    return this;
  }
  public String getCtmPassword()
  {
    return ctmPassword;
  }
  public CTMInfo setCtmPassword(String ctmPassword)
  {
    this.ctmPassword = ctmPassword;
    return this;
  }
}
