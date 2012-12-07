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
 * @author Kohsuke Kawaguchi
 */
public class SCommandInstaller extends DownloadFromUrlInstaller {
    private final CloudTestServer server;
    private final VersionNumber buildNumber;

    private SCommandInstaller(CloudTestServer server, VersionNumber buildNumber) {
        super("cloudtest-scommand-"+buildNumber);
        this.server = server;
        this.buildNumber = buildNumber;
    }

    public SCommandInstaller(CloudTestServer server) {
        this(server,server.getBuildNumber());
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable i = new Installable();
        i.url = new URL(server.getUrl(),"downloads/scommand/scommand.zip").toExternalForm();
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
                new SCommandInstallation(id), node, log);
    }

    public FilePath scommand(Node node, TaskListener log) throws IOException, InterruptedException {
        return performInstallation(node,log).child("bin/scommand");
    }

    // this is internal use only
    // @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<MakeAppTouchTestableInstaller> {
        public String getDisplayName() {
            return "Install CloudTest Command-Line Client";
        }
    }
}
