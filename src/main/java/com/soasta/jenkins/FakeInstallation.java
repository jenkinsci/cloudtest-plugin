/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
class FakeInstallation extends ToolInstallation {
    public FakeInstallation(String name) {
        super(name, null, new ArrayList<ToolProperty<?>>());
    }

    @Override
    public Descriptor<ToolInstallation> getDescriptor() {
        // fake a descriptor just enough to make it happy
        // we don't register this as Extension because it's not supposed to be user visible
        return new DescriptorImpl();
    }

    public static class DescriptorImpl extends ToolDescriptor<FakeInstallation> {
        @Override
        public String getDisplayName() {
            return "CloudTest tool";
        }
    }
}