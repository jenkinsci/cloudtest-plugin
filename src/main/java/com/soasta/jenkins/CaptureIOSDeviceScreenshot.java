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

public class CaptureIOSDeviceScreenshot extends iOSAppInstallerBase {
    @DataBoundConstructor
    public CaptureIOSDeviceScreenshot(String url, String cloudTestServerID, String additionalOptions) {
        super(url, cloudTestServerID, additionalOptions);
    }

    public Object readResolve() throws IOException {
      if (getCloudTestServerID() != null)
          return this;

      LOGGER.info("Re-creating object to get server ID.");

      CloudTestServer s = CloudTestServer.getByURL(getUrl());

      return new CaptureIOSDeviceScreenshot(getUrl(), s.getId(), getAdditionalOptions());
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

    private static final Logger LOGGER = Logger.getLogger(CaptureIOSDeviceScreenshot.class.getName());
}
