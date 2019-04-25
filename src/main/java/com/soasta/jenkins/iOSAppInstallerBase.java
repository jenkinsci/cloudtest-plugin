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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.QuotedStringTokenizer;
import jenkins.tasks.SimpleBuildStep;

public abstract class iOSAppInstallerBase extends Builder implements SimpleBuildStep {
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
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
      ArgumentListBuilder args = new ArgumentListBuilder();
  
      EnvVars envs = run.getEnvironment(listener);
  
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
  
      FilePath bin = new iOSAppInstallerInstaller(s).ios_app_installer(workspace.toComputer().getNode(), listener);
  
      args.add(bin);
      addArgs(envs, args);
      args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());
  
      launcher.launch().cmds(args).pwd(workspace).stdout(listener).join();

      return;
  }

  @Override
  public final boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FilePath filePath = build.getWorkspace();
    if(filePath == null) {
        return false;
    } else {
        perform(build, filePath, launcher, listener);
        return true;
    }
  }
}
