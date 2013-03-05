/*
 * Copyright (c) 2012, CloudBees, Inc.
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
import hudson.util.QuotedStringTokenizer;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class iOSAppInstaller extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;
    private final String ipa, additionalOptions;

    @DataBoundConstructor
    public iOSAppInstaller(String url, String ipa, String additionalOptions) {
        this.url = url;
        this.ipa = ipa;
        this.additionalOptions = additionalOptions;
    }

    public String getUrl() {
        return url;
    }

    public String getIpa() {
        return ipa;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
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

        FilePath bin = new iOSAppInstallerInstaller(s).ios_app_installer(build.getBuiltOn(), listener);

        args.add(bin)
            .add("--ipa", envs.expand(ipa));
        args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilerDescriptor {
        @Override
        public String getDisplayName() {
            return "Install iOS App on Device";
        }

        /**
         * Called automatically by Jenkins whenever the "ipa"
         * field is modified by the user.
         * @param value the new IPA.
         */
        public FormValidation doCheckIpa(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("IPA file is required.");
            } else {
                // TODO: Check if the actual file is present.
                return FormValidation.ok();
            }
        }
    }
}
