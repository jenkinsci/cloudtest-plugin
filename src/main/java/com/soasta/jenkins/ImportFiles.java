/*
 * Copyright (c) 2012-2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.QuotedStringTokenizer;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

public class ImportFiles extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;

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
    public ImportFiles(String url, String files, String excludes, String mode, String additionalOptions) {
        this.url = url;
        this.files = files.trim();
        this.excludes = Util.fixEmptyAndTrim(excludes);
        this.mode = mode;
        this.additionalOptions = additionalOptions;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.get(url);
    }

    public String getUrl() {
        return url;
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        CloudTestServer s = getServer();
        if (s == null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath scommand = new SCommandInstaller(s).scommand(build.getBuiltOn(), listener);

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(scommand)
            .add("cmd=import")
            .add("url=" + s.getUrl())
            .add("username=" + s.getUsername())
            .addMasked("password=" + s.getPassword());
        
        if (mode != null)
            args.add("mode=" + mode);
        
        FilePath[] filePaths = build.getWorkspace().list(files, excludes);
        
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
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
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
}
