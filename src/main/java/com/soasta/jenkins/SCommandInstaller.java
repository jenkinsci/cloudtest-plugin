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
public class SCommandInstaller extends CommonInstaller {

    public SCommandInstaller(CloudTestServer server) throws IOException {
      super(server, Installers.SCOMMAND_INSTALLER);
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

    public FilePath scommand(Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath scommandHome = performInstallation(node,log);
        String os = (String)node.toComputer().getSystemProperties().get("os.name");
        if (os != null && os.startsWith("Windows")) {
            return scommandHome.child("bin/scommand.bat");
        } else {
            return scommandHome.child("bin/scommand");
        }
    }

    // this is internal use only
    // @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MakeAppTouchTestableInstaller> {
        public String getDisplayName() {
            return "Install CloudTest Command-Line Client";
        }
    }
}
