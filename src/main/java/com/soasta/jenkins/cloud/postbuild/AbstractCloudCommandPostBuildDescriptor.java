/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.cloud.postbuild;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import javax.inject.Inject;

import org.kohsuke.stapler.QueryParameter;

import com.soasta.jenkins.CloudTestServer;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractCloudCommandPostBuildDescriptor extends BuildStepDescriptor<Publisher> {
    @Inject
    CloudTestServer.DescriptorImpl serverDescriptor;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }
    
    /**
     * Called automatically by Jenkins whenever the "Name"
     * field is modified by the user.
     */
    public FormValidation doCheckName(@QueryParameter String value) 
    {
        if (value == null || value.trim().isEmpty()) 
        {
            return FormValidation.error("Name is Required");
        } 
        else 
        {
            return FormValidation.ok();
        }
    }

    public ListBoxModel doFillCloudTestServerIDItems() {
        ListBoxModel r = new ListBoxModel();
        for (CloudTestServer s : serverDescriptor.getServers()) {
            r.add(s.getName(), s.getId());
        }
        return r;
    }

    protected FormValidation validateFileMask(AbstractProject project, String value) throws IOException {
        if (value.contains("${")) {
            // if the value contains a variable reference, bail out from the check because we can end up
            // warning a file that actually resolves correctly at the runtime
            // the same change is made in FilePath.validateFileMask independently, and in the future
            // we can remove this check from here
            return FormValidation.ok();
        }

        // Make sure the file exists.
        return FilePath.validateFileMask(project.getSomeWorkspace(), value);
    }
}
