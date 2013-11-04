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

        // We don't have a server ID.
        // This means the builder config is based an older version the plug-in.

        // Look up the server by URL instead.
        // We'll use the ID going forward.
        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

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
