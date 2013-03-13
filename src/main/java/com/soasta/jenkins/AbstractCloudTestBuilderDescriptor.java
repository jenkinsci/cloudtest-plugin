/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import javax.inject.Inject;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractCloudTestBuilderDescriptor extends BuildStepDescriptor<Builder> {
    @Inject
    CloudTestServer.DescriptorImpl serverDescriptor;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public ListBoxModel doFillUrlItems() {
        ListBoxModel r = new ListBoxModel();
        for (CloudTestServer s : serverDescriptor.getServers()) {
            r.add(s.getUrl().toExternalForm());
        }
        return r;
    }

    public boolean showUrlField() {
        return serverDescriptor.getServers().size()>1;
    }
}
