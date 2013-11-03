/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.QuotedStringTokenizer;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Logger;

public class ImportFiles extends AbstractSCommandBuilder {
    /**
     * Comma- or space-separated list of patterns of files to be imported.
     */
    private final String files;

    /**
     * Possibly null 'excludes' pattern as in Ant.
     */
    private final String excludes;
    
    /**
     * How to handle duplicates (if any).
     */
    private final String mode;
    
    /**
     * Additional parameters to pass to SCommand (if any).
     */
    private final String additionalOptions;

    @DataBoundConstructor
    public ImportFiles(String url, String cloudTestServerID, String files, String excludes, String mode, String additionalOptions) {
        super(url, cloudTestServerID);
        this.files = files.trim();
        this.excludes = Util.fixEmptyAndTrim(excludes);
        this.mode = mode;
        this.additionalOptions = additionalOptions;
    }

    public String getFiles() {
        return files;
    }
    
    public String getExcludes() {
        return excludes;
    }
    
    public String getMode() {
        return mode;
    }
    
    public String getAdditionalOptions() {
        return additionalOptions;
    }

    public Object readResolve() throws IOException {
        if (getCloudTestServerID() != null)
            return this;

        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

        return new ImportFiles(getUrl(), s.getId(), files, excludes, mode, additionalOptions);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = getSCommandArgs(build, listener);

        args.add("cmd=import");
        
        if (mode != null)
            args.add("mode=" + mode);
        
        String includes = convertFileListToIncludePattern(files);
        
        FilePath[] filePaths = build.getWorkspace().list(includes, excludes);
        
        if (filePaths.length == 0) {
            // Didn't match anything.
            // No work required.
          
            // Give the user a heads-up.
            listener.error("Import pattern did not match any files.");
            return true;
        }

        for (FilePath filePath : filePaths) {
            args.add("file=" + filePath.getRemote());
        }

        EnvVars envs = build.getEnvironment(listener);
        args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());
        
        // Run it!
        int exitCode = launcher
            .launch()
            .cmds(args)
            .pwd(build.getWorkspace())
            .stdout(listener)
            .join();

        return exitCode == 0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Import CloudTest Objects";
        }

        /**
         * Called automatically by Jenkins whenever the "files"
         * field is modified by the user.
         * @param value the new file pattern.
         */
        public FormValidation doCheckFiles(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            String includes = convertFileListToIncludePattern(value);
            return validateFileMask(project, includes);
        }

        public ListBoxModel doFillModeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Replace the existing object(s)", "overwrite");
            items.add("Fail the import", "error");
            items.add("Ignore", "skip");
            items.add("Generate a non-conflicting name", "rename");
            return items;
        }
    }
    
    private static String convertFileListToIncludePattern(String files) {
        // Convert newlines to commas.  If the user has
        // expanded the textbox to make it a multi-line
        // input, they should not have to enter a comma
        // after each file name, but the FilePath.list()
        // method requires commas.
        return files.replaceAll("[\r\n]+", ",");
    }

    private static final Logger LOGGER = Logger.getLogger(ImportFiles.class.getName());
}
