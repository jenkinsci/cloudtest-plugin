/*
 * Copyright (c) 2012-2016, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.util.regex.Pattern;

import hudson.ProxyConfiguration;

public class ProxyChecker
{
  public static boolean useProxy(String host, ProxyConfiguration proxyConfig)
  {
    // Check if the proxy applies for this destination host.
    // This code is more or less copied from ProxyConfiguration.createProxy() :-(.
    if (proxyConfig != null && proxyConfig.name != null) 
    {
      for (Pattern p : proxyConfig.getNoProxyHostPatterns()) 
      {   
          if (p.matcher(host).matches()) 
          {
              // It's a match.
              // Don't use the proxy.
              return false;
          }
      }
      // we have checked, and the proxy host pattern doesn't match our whitelist, So use the proxy.
      return true;
    }
    else
    {
      // jenkins is not configured to use a proxy.
      return false;
    }
  }
}
