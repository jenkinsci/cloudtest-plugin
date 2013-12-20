/*
 * Copyright (c) 2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

public enum Installers
{
  /**
   * iOSAppInstaller CT link name.
   */
  iOS_APP_INSTALLER ("iosappinstaller", "mobile/iOSAppInstaller.zip"),
  /**
   * MATT installer CT link name.
   */
  MATT_INSTALLER ("makeTouchTestable", "mobile/MakeAppTouchTestable.zip"),
  /**
   * SCommand CT link name.
   */
  SCOMMAND_INSTALLER ("scommand", "scommand/scommand.zip");
  
  private final String CT = "cloudtest";
  private final String DASH = "-";
  private final String DOWNLOADS_DIR = "downloads/";
  
  private String installerType;
  private String downloadFile;
  
  private Installers(String installerType, String downloadFile) {
    this.installerType = installerType;
    this.downloadFile = downloadFile;
  }
  
  public String getInstallerType()
  {
    return installerType;
  }
  
  public String getCTInstallerType()
  {
    StringBuilder CTInstallerType = new StringBuilder(CT);
    CTInstallerType.append(DASH);
    CTInstallerType.append(installerType);
    CTInstallerType.append(DASH);
    
    return CTInstallerType.toString();
  }
  
  public String getInstallerDownloadPath()
  {
    StringBuilder download = new StringBuilder(DOWNLOADS_DIR);
    download.append(downloadFile);
    
    return download.toString();
  }
}
