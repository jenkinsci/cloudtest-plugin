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
   * URL of the server to use (deprecated).
   */
  private final String url;
  /**
   * ID of the server to use.
   * @see CloudTestServer
   */
  private final String cloudTestServerID;
  private final String additionalOptions;

  protected iOSAppInstallerBase(String url, String cloudTestServerID, String additionalOptions) {
    this.url = url;
    this.cloudTestServerID = cloudTestServerID;
    this.additionalOptions = additionalOptions;
  }

  public String getUrl() {
      return url;
  }

  public String getCloudTestServerID() {
      return cloudTestServerID;
  }

  public String getAdditionalOptions() {
      return additionalOptions;
  }

  public CloudTestServer getServer() {
      return CloudTestServer.getByID(cloudTestServerID);
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
