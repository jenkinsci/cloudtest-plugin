/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import com.soasta.jenkins.CloudTestServer;
import com.soasta.jenkins.ProxyChecker;
import com.soasta.jenkins.SCommandInstaller;

import jenkins.model.Jenkins;
import hudson.AbortException;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

public class CloudCommandBuilder {
    /**
     * URL of the server to use (deprecated).
     */
    private String url;
    /**
     * ID of the server to use.
     * @see CloudTestServer
     */
    private String cloudTestServerID;
    private AbstractBuild<?, ?> build;
    private BuildListener listener;

    public CloudTestServer getServer() {
        return CloudTestServer.getByID(cloudTestServerID);
    }
    
    public CloudCommandBuilder setUrl(String url)
    {
      this.url = url;
      return this;
    }

    public String getUrl() {
        return url;
    }
    
    public CloudCommandBuilder setCloudTestServerID(String value)
    {
      this.cloudTestServerID = value;
      return this;
    }

    public String getCloudTestServerID() {
        return cloudTestServerID;
    }
    
    public CloudCommandBuilder setBuild(AbstractBuild<?, ?> build)
    {
      this.build = build;
      return this;
    }
    
    public CloudCommandBuilder setListener(BuildListener listener)
    {
      this.listener = listener;
      return this;
    }

    public ArgumentListBuilder build() throws IOException, InterruptedException {
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
      
      FilePath scommand = new SCommandInstaller(s).scommand(build.getBuiltOn(), listener);
      
      ArgumentListBuilder args = new ArgumentListBuilder();
      args.add(scommand)
          .add("url=" + s.getUrl())
          .add("username="+s.getUsername());
          
      if (s.getPassword() != null)
          args.addMasked("password=" + s.getPassword());
      
      ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;

      if (proxyConfig != null && proxyConfig.name != null) {
          // Jenkins is configured to use a proxy server.

          // Extract the destination CloudTest host.
          String host = new URL(s.getUrl()).getHost();

          if (ProxyChecker.useProxy(host, proxyConfig)) {
              // Add the SCommand proxy parameters.
              args.add("httpproxyhost=" + proxyConfig.name)
                  .add("httpproxyport=" + proxyConfig.port);

              // If there are proxy credentials, add those too.
              if (proxyConfig.getUserName() != null) {
                  args.add("httpproxyusername=" + proxyConfig.getUserName())
                      .addMasked("httpproxypassword=" + proxyConfig.getPassword());
              }
          }
      }

      return args;
    }
}
