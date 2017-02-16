/*
 * Copyright (c) 2012-2014, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.QuotedStringTokenizer;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class MakeAppTouchTestable extends Builder {
    /**
     * URL of the server to use (deprecated).
     */
    private final String url;
    /**
     * ID of the server to use.
     * @see CloudTestServer
     */
    private final String cloudTestServerID;
    private final InputType inputType;
    /**
     * The input file associated with inputType.
     * In order to keep the backwards compatibility of the project file name
     * from before inputType was added, we are not changing the projectFile to 
     * inputFile.
     */
    private final String projectFile;
    private final String target;
    private final String launchURL;
    private final boolean backupModifiedFiles;
    private final String additionalOptions;
    private final String javaOptions;

    private final String DEFAULT_JAVA_OPTION = "-Xmx1024m";
    
    @DataBoundConstructor
    public MakeAppTouchTestable(String url, String cloudTestServerID, String inputType, String projectFile, 
      String target, String launchURL, boolean backupModifiedFiles, String additionalOptions, 
      String javaOptions) {
        this.url = url;
        this.cloudTestServerID = cloudTestServerID;
        this.inputType = InputType.getInputType(inputType);
        this.projectFile = projectFile;
        this.target = target;
        this.launchURL = launchURL;
        this.backupModifiedFiles = backupModifiedFiles;
        this.additionalOptions = additionalOptions;
        this.javaOptions = javaOptions;
    }

    public String getUrl() {
        return url;
    }

    public String getCloudTestServerID() {
        return cloudTestServerID;
    }
    
    public InputType getInputType() {
        return inputType;
    }
    public String getProjectFile() {
        return projectFile;
    }

    public String getTarget() {
        return target;
    }

    public String getLaunchURL() {
        return launchURL;
    }

    public boolean getBackupModifiedFiles() {
        return backupModifiedFiles;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
    }
    
    public String getJavaOptions() {
        return javaOptions;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.getByID(cloudTestServerID);
    }

    public Object readResolve() throws IOException {
        if (cloudTestServerID != null)
            return this;

        // We don't have a server ID.
        // This means the builder config is based an older version the plug-in.

        // Look up the server by URL instead.
        // We'll use the ID going forward.
        CloudTestServer s = CloudTestServer.getByURL(getUrl());

        LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

        return new MakeAppTouchTestable(url, s.getId(), inputType.getInputType(), projectFile, 
            target, launchURL, backupModifiedFiles, additionalOptions, javaOptions);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        JDK java = build.getProject().getJDK();
        if (java!=null)
            args.add(java.getHome()+"/bin/java");
        else
            args.add("java");

        CloudTestServer s = getServer();
        if (s==null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        EnvVars envs = build.getEnvironment(listener);

        FilePath path = new MakeAppTouchTestableInstaller(s).performInstallation(build.getBuiltOn(), listener);

        args.add(DEFAULT_JAVA_OPTION)
            .add(new QuotedStringTokenizer(envs.expand(javaOptions)).toArray())
            .add("-jar")
            .add(path.child("MakeAppTouchTestable.jar"))
            .add("-overwriteapp")
            .add("-url").add(s.getUrl());

        if(s.getApitoken() != null && !s.getApitoken().isEmpty()) {
            args.add("-apitoken")
                .add(s.getApitoken()); 
        }
        else {
            args.add("-username",s.getUsername()); 
            
            if (s.getPassword() != null) {
                args.add("-password")
                    .addMasked(s.getPassword().getPlainText()); 
            } 
        }       

        args.add(inputType.getInputType(), envs.expand(projectFile));
        
        if (target!=null && !target.trim().isEmpty())
            args.add("-target", envs.expand(target));
        if (launchURL!=null && !launchURL.trim().isEmpty())
            args.add("-launchURL", envs.expand(launchURL));
        if (!backupModifiedFiles)
            args.add("-nobackup");

        args.add(new QuotedStringTokenizer(envs.expand(additionalOptions)).toArray());

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Make App TouchTestable";
        }

        /**
         * Called automatically by Jenkins whenever the "projectFile"
         * field (known to the user as the input file) is modified by 
         * the user.
         * @param value of the new path.
         */
        public FormValidation doCheckProjectFile(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Input file is required.");
            } else {
                // Make sure the directory exists.
                return validateFileMask(project, value);
            }
        }
        
        /**
         * Called automatically by Jenkins to fill the drop-down "inputType".
         * @return items the values of "inputType".
         */
        public ListBoxModel doFillInputTypeItems() {
            ListBoxModel items = new ListBoxModel();
            // Because the Project is first, it's the default.
            items.add("Project", InputType.PROJECT.toString());
            items.add("APK", InputType.APK.toString());
            items.add("IPA", InputType.IPA.toString());
            items.add("iOS App Bundle", InputType.APP.toString());
            return items;
        }
    }

    static enum InputType {
      APK ("-apk"),
      APP ("-appbundle"),
      IPA ("-ipa"),
      PROJECT ("-project");
    
      String inputType;
    
      private InputType(String inputType) {
          this.inputType = inputType;
      }
      
      private String getInputType() {
          return inputType;
      }
    
      // The inputType should never be anything than the options
      // listed in the doFillInputTypeItems() as it is a drop-down.
      // The values given should always be valid.
      private static InputType getInputType(String inputType) {
          return InputType.valueOf(inputType);
      }
    }
    
    private static final Logger LOGGER = Logger.getLogger(MakeAppTouchTestable.class.getName());
}
