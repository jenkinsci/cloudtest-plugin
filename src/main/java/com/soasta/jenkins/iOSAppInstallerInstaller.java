/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;

import java.io.IOException;
import java.net.URL;

/**
 * Installs "iOS App Installer" from CloudTest.
 *
 * @author Kohsuke Kawaguchi
 */
public class iOSAppInstallerInstaller extends CommonInstaller {
  
    public iOSAppInstallerInstaller(CloudTestServer server) throws IOException {
      super(server, Installers.iOS_APP_INSTALLER);
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable i = new Installable();
        i.url = getServer().getUrl() + getInstallerType().getInstallerDownloadPath();
        i.id = id;
        i.name = getBuildNumber().toString();
        return i;
    }
    
    /**
     * We implement {@link ToolInstaller} just so that we can reuse its installation code.
     * And because of this, we collapse {@link ToolInstallation} and {@link ToolInstaller}.
     */
    public FilePath performInstallation(Node node, TaskListener log) throws IOException, InterruptedException {
      return super.performInstallation(
                new FakeInstallation(id), node, log);
    }

    public FilePath ios_app_installer(Node node, TaskListener log) throws IOException, InterruptedException {
        return performInstallation(node,log).child("bin/ios_app_installer");
    }

    public FilePath ios_sim_launcher(Node node, TaskListener log) throws IOException, InterruptedException {
        return performInstallation(node,log).child("bin/ios_sim_launcher");
    }

    // this is internal use only
    // @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MakeAppTouchTestableInstaller> {
        public String getDisplayName() {
            return "Install CloudTest iOS App Installer";
        }
    }
}
