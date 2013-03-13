/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.QuotedStringTokenizer;

public abstract class iOSAppInstallerBase extends Builder {
  /**
   * URL of {@link CloudTestServer}.
   */
  private final String url;
  private final String additionalOptions;

  protected iOSAppInstallerBase(String url, String additionalOptions) {
    this.url = url;
    this.additionalOptions = additionalOptions;
  }

  public String getUrl() {
      return url;
  }

  public String getAdditionalOptions() {
      return additionalOptions;
  }

  public CloudTestServer getServer() {
      return CloudTestServer.get(url);
  }
  
  protected abstract void addArgs(EnvVars envs, ArgumentListBuilder args);

  @Override
  public final boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
      ArgumentListBuilder args = new ArgumentListBuilder();
  
      EnvVars envs = build.getEnvironment(listener);
  
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
  
      FilePath bin = new iOSAppInstallerInstaller(s).ios_app_installer(build.getBuiltOn(), listener);
  
      args.add(bin);
      addArgs(envs, args);
      args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());
  
      int exitCode = launcher
          .launch()
          .cmds(args)
          .pwd(build.getWorkspace())
          .stdout(listener)
          .join();

      return exitCode == 0;
  }
}
