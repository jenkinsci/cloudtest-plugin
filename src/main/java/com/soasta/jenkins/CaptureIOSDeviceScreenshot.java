/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

public class CaptureIOSDeviceScreenshot extends iOSAppInstallerBase {
    @DataBoundConstructor
    public CaptureIOSDeviceScreenshot(String url, String additionalOptions) {
        super(url, additionalOptions);
    }

    @Override
    protected void addArgs(EnvVars envs, ArgumentListBuilder args) {
        args.add("--screenshot");
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Capture iOS Device Screen Shot";
        }
    }
}
