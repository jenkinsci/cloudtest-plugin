/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import hudson.AbortException;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

public abstract class AbstractSCommandBuilder extends Builder implements SimpleBuildStep {
    /**
     * URL of the server to use (deprecated).
     */
    private String url;
    /**
     * ID of the server to use.
     * @see CloudTestServer
     */
    private final String cloudTestServerID;

    public AbstractSCommandBuilder(String cloudTestServerID) {
        this.cloudTestServerID = cloudTestServerID;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.getByID(cloudTestServerID);
    }
    
    @DataBoundSetter
    public final void setUrl(String url)
    {
      this.url = url;
    }

    public final String getUrl() {
        return url;
    }

    public final String getCloudTestServerID() {
        return cloudTestServerID;
    }

    protected ArgumentListBuilder getSCommandArgs(Run<?, ?> run, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
  
      // Download SCommand, if needed.
      FilePath scommand = new SCommandInstaller(s).scommand(workspace.toComputer().getNode(), listener);
  
      ArgumentListBuilder args = new ArgumentListBuilder();
      args.add(scommand);
      args.add("url=" + s.getUrl());
      
      boolean usingAPIToken = s.getApitoken() != null && !s.getApitoken().trim().isEmpty();
      boolean usingUserNameAndPass = s.getUsername() != null && !s.getUsername().isEmpty(); // we validate that if they set username, they also set the password. 
      
      // what do they want to use? We want to alert the user to this. 
      if (usingAPIToken && usingUserNameAndPass) {
        throw new AbortException("Cannot set both Username or Password and API Token. Please either remove the API token, or userName and password.");
      }
     
      if(usingAPIToken) {
        args.add("apitoken=" + s.getApitoken());
      }
      else {
        // default behavior. 
        args.add("username="+s.getUsername());
        args.addMasked("password=" + s.getPassword());
      }
      
      if (s.getKeyStoreLocation() != null && !s.getKeyStoreLocation().isEmpty()) {
        args.add("keystore=" + s.getKeyStoreLocation());
        
        if (s.getKeyStorePassword() != null) {
          args.addMasked("keystorepass=" + s.getKeyStorePassword());
        }
      }
         
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
