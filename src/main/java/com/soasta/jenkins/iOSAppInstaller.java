/*
 * Copyright (c) 2012, CloudBees, Inc.
 * Copyright (c) 2012-2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class iOSAppInstaller extends iOSAppInstallerBase {
    private final String ipa;

    @DataBoundConstructor
    public iOSAppInstaller(String url, String ipa, String additionalOptions) {
        super(url, additionalOptions);
        this.ipa = ipa;
    }

    public String getIpa() {
        return ipa;
    }
    
    @Override
    protected void addArgs(EnvVars envs, ArgumentListBuilder args) {
        args.add("--ipa", envs.expand(ipa));
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Install iOS App on Device";
        }

        /**
         * Called automatically by Jenkins whenever the "ipa"
         * field is modified by the user.
         * @param value the new IPA.
         */
        public FormValidation doCheckIpa(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("IPA file is required.");
            } else {
                // Make sure the file exists.
                return FilePath.validateFileMask(project.getSomeWorkspace(), value);
            }
        }
    }
}
