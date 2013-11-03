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
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Logger;

public class iOSSimulatorLauncher extends Builder {
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

        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

        return new iOSSimulatorLauncher(url, s.getId(), app, sdk, family);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        EnvVars envs = build.getEnvironment(listener);

        CloudTestServer server = getServer();
        if (server == null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath bin = new iOSAppInstallerInstaller(server).ios_sim_launcher(build.getBuiltOn(), listener);

        // Determine TouchTest Agent URL for this server.
        // The simulator will automatically open this URL
        // in Mobile Safari.
        String agentURL;
        String cloudTestServerUrl = server.getUrl().toString();
        
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

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
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
