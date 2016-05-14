package com.soasta.jenkins.httpclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class HttpClientSettings
{
  private KeyStore keyStore;
  private String keyStorePassword;
  private boolean trustSelfSigned;
  private String url;
  
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
  
  public static KeyStore loadKeyStore(String path, String password) throws IOException
  {
    
    if (path == null || path.isEmpty())
    {
      return null; // no keystore path specified. 
    }
    try
    {
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      FileInputStream fis = new FileInputStream(path);
      try 
      {
          ks.load(fis, password == null || password.isEmpty() ? null : password.toCharArray());
      }
      finally
      {
        fis.close();
      }
      return ks;
    }
    catch (GeneralSecurityException e)
    {
      throw new RuntimeException(e);
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
