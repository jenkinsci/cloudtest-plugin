/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.util.ArgumentListBuilder;

import org.kohsuke.stapler.DataBoundConstructor;

public class RebootIOSDevice extends iOSAppInstallerBase {
    @DataBoundConstructor
    public RebootIOSDevice(String url, String cloudTestServerID, String additionalOptions) {
        super(url, cloudTestServerID, additionalOptions);
    }
    
    public Object readResolve() throws IOException {
        if (getCloudTestServerID() != null)
            return this;

        LOGGER.info("Re-creating object to get server ID.");

        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        return new RebootIOSDevice(getUrl(), s.getId(), getAdditionalOptions());
    }

    @Override
    protected void addArgs(EnvVars envs, ArgumentListBuilder args) {
        args.add("--reboot");
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Reboot iOS Device";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(RebootIOSDevice.class.getName());
}
