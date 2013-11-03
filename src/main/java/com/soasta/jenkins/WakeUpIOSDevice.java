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

public class WakeUpIOSDevice extends iOSAppInstallerBase {
    @DataBoundConstructor
    public WakeUpIOSDevice(String url, String cloudTestServerID, String additionalOptions) {
        super(url, cloudTestServerID, additionalOptions);
    }

    public Object readResolve() throws IOException {
        if (getCloudTestServerID() != null)
            return this;

        LOGGER.info("Re-creating object to get server ID.");

        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        return new WakeUpIOSDevice(getUrl(), s.getId(), getAdditionalOptions());
    }

    @Override
    protected void addArgs(EnvVars envs, ArgumentListBuilder args) {
        args.add("--wakeup");
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Wake up iOS Device";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(WakeUpIOSDevice.class.getName());
}
