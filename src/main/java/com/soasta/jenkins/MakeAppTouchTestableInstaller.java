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
 * @author Kohsuke Kawaguchi
 */
public class MakeAppTouchTestableInstaller extends CommonInstaller {

    public MakeAppTouchTestableInstaller(CloudTestServer server) throws IOException {
        super(server,Installers.MATT_INSTALLER);
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable i = new Installable();
        i.url = new URL(getServer().getUrl(), getInstallerType().getInstallerDownloadPath()).toExternalForm();
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

    // this is internal use only
    // @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MakeAppTouchTestableInstaller> {
        public String getDisplayName() {
            return "Install Make App TouchTestable";
        }
    }
}
