/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud;

import java.io.IOException;
import com.soasta.jenkins.AbstractSCommandBuilder;
import com.soasta.jenkins.CloudTestServer;
import com.soasta.jenkins.SCommandInstaller;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
      
      return AbstractSCommandBuilder.getSCommandArgs(scommand, s);
    }
}
