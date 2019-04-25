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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.QuotedStringTokenizer;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
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
    private String excludes;
    
    /**
     * How to handle duplicates (if any).
     */
    private final String mode;
    
    /**
     * Additional parameters to pass to SCommand (if any).
     */
    private String additionalOptions;

    @DataBoundConstructor
    public ImportFiles(String cloudTestServerID, String files, String mode) {
        super(cloudTestServerID);
        this.files = files.trim();
        this.mode = mode;
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
      return Util.fixEmptyAndTrim(additionalOptions);
    }
    
    @DataBoundSetter
    public void setAdditionalOptions(String additionalOptions)
    {
      this.additionalOptions = additionalOptions;
    }
    
    @DataBoundSetter
    public void setExcludes(String excludes)
    {
      this.excludes = Util.fixEmptyAndTrim(excludes);
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

        ImportFiles result = new ImportFiles(s.getId(), files, mode);
        result.setExcludes(excludes);
        result.setAdditionalOptions(additionalOptions);
        return result;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
      FilePath filePath = build.getWorkspace();
      if(filePath == null) {
          return false;
      }else {
          perform(build, filePath, launcher, listener);
          return true;
      }
    }

    @Extension
    @Symbol("importFiles")
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

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException
    {
      ArgumentListBuilder args = getSCommandArgs(run, workspace, listener);

      args.add("cmd=import");
      
      if (mode != null)
          args.add("mode=" + mode);
      
      String includes = convertFileListToIncludePattern(files);
      
      FilePath[] filePaths = workspace.list(includes, excludes);
      
      if (filePaths.length == 0) {
          // Didn't match anything.
          // No work required.
        
          // Give the user a heads-up.
          listener.error("Import pattern did not match any files.");
          return;
      }

      for (FilePath filePath : filePaths) {
          args.add("file=" + filePath.getRemote());
      }

      EnvVars envs = run.getEnvironment(listener);
      if(additionalOptions != null)
      {
        args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());
      }
      
      // Run it!
      launcher.launch()
          .cmds(args)
          .pwd(workspace)
          .stdout(listener)
          .join();

      return;
      
    }
}
