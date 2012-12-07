package com.soasta.jenkins;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.util.VersionNumber;

import java.io.IOException;
import java.net.URL;

/**
 * Installs "iOS App Installer" from CloudTest.
 *
 * @author Kohsuke Kawaguchi
 */
public class iOSAppInstallerInstaller extends DownloadFromUrlInstaller {
    private final CloudTestServer server;
    private final VersionNumber buildNumber;

    private iOSAppInstallerInstaller(CloudTestServer server, VersionNumber buildNumber) {
        super("cloudtest-iosappinstaller-"+buildNumber);
        this.server = server;
        this.buildNumber = buildNumber;
    }

    public iOSAppInstallerInstaller(CloudTestServer server) {
        this(server,server.getBuildNumber());
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable i = new Installable();
        i.url = new URL(server.getUrl(),"downloads/mobile/iOSAppInstaller.zip").toExternalForm();
        i.id = id;
        i.name = buildNumber.toString();
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

    public FilePath ios_sim_installer(Node node, TaskListener log) throws IOException, InterruptedException {
        return performInstallation(node,log).child("bin/ios_sim_installer");
    }


    // this is internal use only
    // @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MakeAppTouchTestableInstaller> {
        public String getDisplayName() {
            return "Install CloudTest iOS App Installer";
        }
    }
}
