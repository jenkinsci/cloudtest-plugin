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
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.QuotedStringTokenizer;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

public class iOSSimulatorLauncher extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;
    private final String app, sdk, family, agentURL;

    @DataBoundConstructor
    public iOSSimulatorLauncher(String url, String app, String sdk, String family, String agentURL) {
        this.url = url;
        this.app = app;
        this.sdk = sdk;
        this.family = family;
        this.agentURL = agentURL;
    }

    public String getUrl() {
        return url;
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

    public String getAgentURL() {
        return agentURL;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.get(url);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        EnvVars envs = build.getEnvironment(listener);

        CloudTestServer s = getServer();
        if (s==null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath bin = new iOSAppInstallerInstaller(s).ios_sim_launcher(build.getBuiltOn(), listener);

        args.add(bin)
            .add("--app", envs.expand(app));

        if (sdk != null && !sdk.trim().isEmpty())
            args.add("--sdk", envs.expand(sdk));
        if (family != null && !family.trim().isEmpty())
            args.add("--family", envs.expand(family));
        if (agentURL != null && !agentURL.trim().isEmpty())
            args.add("--agenturl", envs.expand(agentURL));

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilerDescriptor {
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
        public FormValidation doCheckApp(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("App directory is required.");
            } else {
                // TODO: Check if the actual directory is present.
                return FormValidation.ok();
            }
        }
    }
}
