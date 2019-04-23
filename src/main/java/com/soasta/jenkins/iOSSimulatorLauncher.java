/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Logger;

public class iOSSimulatorLauncher extends Builder implements SimpleBuildStep {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;
    private final String cloudTestServerID;
    private final String app, sdk, family;

    @DataBoundConstructor
    public iOSSimulatorLauncher(String url, String cloudTestServerID, String app, String sdk, String family) {
        this.url = url;
        this.cloudTestServerID = cloudTestServerID;
        this.app = app;
        this.sdk = sdk;
        this.family = family;
    }

    public String getUrl() {
        return url;
    }

    public String getCloudTestServerID() {
        return cloudTestServerID;
    }

    public String getApp() {
        return app;
    }

    public String getSdk() {
        return sdk;
    }

    public String getFamily() {
        return family;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.getByID(cloudTestServerID);
    }

    public Object readResolve() throws IOException {
        if (cloudTestServerID != null)
            return this;

        // We don't have a server ID.
        // This means the builder config is based an older version the plug-in.

        // Look up the server by URL instead.
        // We'll use the ID going forward.
        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

        return new iOSSimulatorLauncher(url, s.getId(), app, sdk, family);
    }
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        EnvVars envs = run.getEnvironment(listener);

        CloudTestServer server = getServer();
        if (server == null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath bin = new iOSAppInstallerInstaller(server).ios_sim_launcher(workspace.toComputer().getNode(), listener);

        // Determine TouchTest Agent URL for this server.
        // The simulator will automatically open this URL
        // in Mobile Safari.
        String agentURL;
        String cloudTestServerUrl = server.getUrl();
        
        if (cloudTestServerUrl.endsWith("/"))
            agentURL = cloudTestServerUrl + "touchtest";
        else
            agentURL = cloudTestServerUrl + "/touchtest";

        args.add(bin)
            .add("--app", envs.expand(app))
            .add("--agenturl", agentURL);

        if (sdk != null && !sdk.trim().isEmpty())
            args.add("--sdk", envs.expand(sdk));
        if (family != null && !family.trim().isEmpty())
            args.add("--family", envs.expand(family));

        launcher.launch().cmds(args).pwd(workspace).stdout(listener).join();
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

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Run App in iOS Simulator";
        }

        public ListBoxModel doFillFamilyItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("iPhone", "iphone");
            items.add("iPhone (Retina)", "iphone_retina");
            items.add("iPad", "ipad");
            items.add("iPad (Retina)", "ipad_retina");
            return items;
        }

        /**
         * Called automatically by Jenkins whenever the "app"
         * field is modified by the user.
         * @param value the new app directory.
         */
        public FormValidation doCheckApp(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("App directory is required.");
            } else {
                // Make sure the directory exists.
                return validateFileMask(project, value);
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(iOSSimulatorLauncher.class.getName());
}
