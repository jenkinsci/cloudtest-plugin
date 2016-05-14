package com.soasta.jenkins.httpclient;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.logging.Logger;

import com.soasta.jenkins.CloudTestServer;

public class HttpClientSettings
{
  private KeyStore keyStore;
  private String keyStorePassword;
  private boolean trustSelfSigned;
  private String url;
  private static final Logger LOGGER = Logger.getLogger(HttpClientSettings.class.getName());
  
  public KeyStore getKeyStore()
  {
    return keyStore;
  }
  public HttpClientSettings setKeyStore(KeyStore keyStore)
  {
    this.keyStore = keyStore;
    return this;
  }
  public String getKeyStorePassword()
  {
    return keyStorePassword;
  }
  public HttpClientSettings setKeyStorePassword(String keyStorePassword)
  {
    this.keyStorePassword = keyStorePassword;
    return this;
  }
  public boolean trustSelfSigned()
  {
    return trustSelfSigned;
  }
  public HttpClientSettings setTrustSelfSigned(boolean trustSelfSigned)
  {
    this.trustSelfSigned = trustSelfSigned;
    return this;
  }
  
  public static KeyStore loadKeyStore(String path, String password)
  {
    if (path == null || path.isEmpty())
    {
      return null; // no keystore specified. 
    }
    try
    {
      KeyStore ks = null;
      ks = KeyStore.getInstance(KeyStore.getDefaultType());
      FileInputStream fis = new java.io.FileInputStream(path);
      ks.load(fis,password == null ? null : password.toCharArray());
      return ks;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
  }
  public String getUrl()
  {
    return url;
  }
  public HttpClientSettings setUrl(String url)
  {
    this.url = url;
    return this;
  }
}
