package com.soasta.jenkins.httpclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.util.EntityUtils;

import com.soasta.jenkins.ProxyChecker;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class GenericSelfClosingHttpClient
{
  private static Logger m_log = Logger.getLogger(GenericSelfClosingHttpClient.class.getName());
  private CloseableHttpClient m_client; 
  private boolean m_closeAfterUse = true;
  
  /**
   * HttpClient designed to be used for only one HttpCall.
   * @param settings
   */
  public GenericSelfClosingHttpClient(HttpClientSettings settings)
  {
    buildClient(settings);
  }
  
  public GenericSelfClosingHttpClient(HttpClientSettings settings, boolean closeAfterUse)
  {
    buildClient(settings);
    this.m_closeAfterUse = closeAfterUse;
  }
  
  public GenericSelfClosingHttpClient(CloseableHttpClient client)
  {
    m_client = client;
  }
  
  public String sendRequest(HttpUriRequest httpRequest) throws IOException
  {
    // Jira Bug JENKINS-21033: Changing the User-Agent from "Java/<Java version #>" to "Jenkins/<Jenkins version #>"
    httpRequest.addHeader("User-Agent", "Jenkins/" + Jenkins.getVersion().toString());
    HttpResponse httpResponse = m_client.execute(httpRequest);
    try
    {
      try
      {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        
        
        String responseBody = httpResponse.getEntity() == null ? null : EntityUtils.toString(httpResponse.getEntity(), getDefaultResponseCharacterSet());
        
        return processResponse(httpResponse, statusCode, responseBody);
      }
      finally
      {
        // Make sure the connection can be returned to the pool.
        EntityUtils.consume(httpResponse.getEntity());
      }
    }
    finally
    {
      if (m_closeAfterUse)
      {
        m_client.close();
      }
    }
  }
  
  /**
   * Returns the character set to use when reading responses, if the response does not specify a character set
   * in the "Content-Type" header. 
   * @return the default character set, or {@code null} to use the HTTP client default (currently "ISO-8859-1").
   */
  private String getDefaultResponseCharacterSet()
  {
    return null;
  }
  
  private String processResponse(HttpResponse httpResponse, int statusCode, String responseBody)
  {
    if (statusCode < 300)
    {
      return responseBody;
    }
    else
    {
      throw new HttpException(statusCode, responseBody);
    }
  }
  
  public void close() throws IOException
  {
    m_client.close();
  }
  
  private void buildClient(HttpClientSettings settings)
  {
    HttpClientBuilder builder = HttpClientBuilder.create();
    RegistryBuilder<ConnectionSocketFactory> schemeRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
    schemeRegistryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
    
    KeyManager[]  keyManagers = getKeyManagers(settings);
   
    TrustManager[] trustManagers = null;
    
    // trust all self signed certs
    if (settings.trustSelfSigned())
    {
      trustManagers = getTrustAllSelfSigned();
    }
    
    try
    {
     
      SSLConnectionSocketFactory sslConnectionFactory = getSSLFactory(keyManagers, trustManagers);
      builder.setSSLSocketFactory(sslConnectionFactory);
      schemeRegistryBuilder.register("https", sslConnectionFactory);
      
      BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(schemeRegistryBuilder.build());
      builder.setConnectionManager(connectionManager);
    }
    catch (Exception e)
    {
      m_log.log(Level.SEVERE, "Error Creating HTTP Client", e);
    }
    
    Jenkins jenkins = Jenkins.getInstance();
    ProxyConfiguration proxyInfo = jenkins != null ? jenkins.proxy : null;
    if (proxyInfo != null)
    {
      HttpHost proxy = new HttpHost(proxyInfo.name, proxyInfo.port);
      String host = null;
      try
      {
        host = new URL(settings.getUrl()).getHost();
      }
      catch (MalformedURLException e)
      {
        m_log.log(Level.SEVERE, "Error Creating HTTP Client", e);
      }
      builder.setDefaultCredentialsProvider(getProxyCreds(proxyInfo, host));
      builder.setProxy(proxy);
    }
    
    m_client = builder.build();
  }
  
  public static KeyManager[] getKeyManagers(HttpClientSettings settings)
  {
    if (settings.getKeyStore() == null)
    {
      return null;
    }
    try
    {
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(settings.getKeyStore(), settings.getKeyStorePassword() == null ? null : settings.getKeyStorePassword().toCharArray());
      
      return keyManagerFactory.getKeyManagers();
    }
    catch (Exception e)
    {
      m_log.log(Level.SEVERE, "Error Creating HTTP Client", e);
      return null; // as if no keystore was going to be used. 
    }
  }
  
  public static TrustManager[] getTrustAllSelfSigned()
  {
    return new TrustManager[]
      {
        new X509TrustManager() 
        {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() 
          {
            return new X509Certificate[] {};
          }
          public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) 
          {
          }
          public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
          {
          }
        }
      }; 
  }
  
  public static SSLConnectionSocketFactory getSSLFactory(KeyManager[] keyManager, TrustManager[] trustManagers) throws Exception
  {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    
    sslContext.init(keyManager, trustManagers, new java.security.SecureRandom());
    SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    
    return sslConnectionFactory;
  }
  
  private CredentialsProvider getProxyCreds(ProxyConfiguration proxyInfo, String host)
  {
    CredentialsProvider credentialsProvider = null;
    // is the host on the no proxy list? 
    if (proxyInfo != null && proxyInfo.name != null && ProxyChecker.useProxy(host, proxyInfo))
    {
      credentialsProvider = new BasicCredentialsProvider();
      
      if (proxyInfo.getUserName() != null)
      {
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxyInfo.getUserName(),proxyInfo.getPassword()));
      }
    }
    return credentialsProvider;
  }
}
