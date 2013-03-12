/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

public abstract class AbstractSCommandBuilder extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;
    
    private FilePath scommand;
  
    public AbstractSCommandBuilder(String url) {
        this.url = url;
    }
  
    public CloudTestServer getServer() {
        return CloudTestServer.get(url);
    }
  
    public String getUrl() {
        return url;
    }
  
    protected ArgumentListBuilder getSCommandArgs(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
  
      // Download SCommand, if needed.

      // We remember the location for next time, since this might be called
      // more than once for a single build step (e.g. TestCompositionRunner
      // with a list of compositions).
      
      // As far as I know, this null check does not need to be thread-safe.
      if (scommand == null)
          scommand = new SCommandInstaller(s).scommand(build.getBuiltOn(), listener);
  
      ArgumentListBuilder args = new ArgumentListBuilder();
      args.add(scommand)
          .add("url=" + s.getUrl())
          .add("username="+s.getUsername())
          .addMasked("password=" + s.getPassword());
      
      return args;
    }
}
