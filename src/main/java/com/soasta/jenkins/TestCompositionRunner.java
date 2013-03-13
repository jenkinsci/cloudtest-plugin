/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.model.AbstractBuild;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import jenkins.model.Jenkins;

import org.apache.commons.io.output.ByteArrayOutputStream;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCompositionRunner extends AbstractSCommandBuilder {
    /**
     * Composition to execute.
     */
    private final String composition;

    private final boolean deleteOldResults;

    private final int maxDaysOfResults;

    @DataBoundConstructor
    public TestCompositionRunner(String url, String composition, DeleteOldResultsSettings deleteOldResults) {
        super(url);
        this.composition = composition;
        this.deleteOldResults = (deleteOldResults != null);
        this.maxDaysOfResults = (deleteOldResults == null ? 0 : deleteOldResults.maxDaysOfResults);
    }

    public String getComposition() {
        return composition;
    }

    public boolean getDeleteOldResults() {
        return deleteOldResults;
    }

    public int getMaxDaysOfResults() {
        return maxDaysOfResults;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // Create a unique sub-directory to store all test results.
        String resultsDir = "." + getClass().getName();
        
        // Split by newline.
        EnvVars envs = build.getEnvironment(listener);
        String[] compositions = envs.expand(this.composition).split("[\r\n]+");

        for (String composition : compositions) {
            ArgumentListBuilder args = getSCommandArgs(build, listener);

            args.add("cmd=play", "wait", "format=junitxml")
                .add("name=" + composition);
  
            String fileName = composition + ".xml";
  
            // Strip off any leading slash characters (composition names
            // will typically be the full CloudTest folder path).
            if (fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }
            
            // Put the file in the test results directory.
            fileName = resultsDir + File.separator + fileName;
            
            FilePath xml = new FilePath(build.getWorkspace(), fileName);
            
            // Make sure the directory exists.
            xml.getParent().mkdirs();

            // Run it!
            int exitCode = launcher
                .launch()
                .cmds(args)
                .pwd(build.getWorkspace())
                .stdout(xml.write())
                .stderr(listener.getLogger())
                .join();

            if (xml.length() == 0) {
                // SCommand did not produce any output.
                // This should never happen, but just in case...
                return false;
            }
        }
        
        // Now that we've finished running all the compositions, pass
        // the results directory off to the JUnit archiver.
        String resultsPattern = resultsDir + "/**/*.xml";
        JUnitResultArchiver archiver = new JUnitResultArchiver(
            resultsPattern,
            true,
            new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(
                Saveable.NOOP,
                Collections.singleton(new JunitResultPublisher(null))));
        return archiver.perform(build,launcher,listener);
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
        @Override
        public String getDisplayName() {
            return "Play Composition";
        }

        /**
         * Called automatically by Jenkins whenever the "composition"
         * field is modified by the user.
         * @param value the new composition name.
         */
        public FormValidation doCheckComposition(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Composition name is required.");
            } else {
                return FormValidation.ok();
            }
        }

        /**
         * Called automatically by Jenkins whenever the "maxDaysOfResults"
         * field is modified by the user.
         * @param value the new maximum age, in days.
         */
        public FormValidation doCheckMaxDaysOfResults(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Days to keep results is required.");
            } else {
                try {
                    int maxDays = Integer.parseInt(value);

                    if (maxDays <= 0) {
                        return FormValidation.error("Value must be > 0.");
                    } else {
                        return FormValidation.ok();
                    }
                } catch (NumberFormatException e) {
                    return FormValidation.error("Value must be numeric.");
                }
            }
        }

        public AutoCompletionCandidates doAutoCompleteComposition(@QueryParameter String url) throws IOException, InterruptedException {
            CloudTestServer s = CloudTestServer.get(url);

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(install(s))
                .add("list", "type=composition")
                .add("url=" + s.getUrl())
                .add("username=" + s.getUsername())
                .addMasked("password=" + s.getPassword());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int exit = new LocalLauncher(TaskListener.NULL).launch().cmds(args).stdout(out).join();
            if (exit==0) {
                BufferedReader r = new BufferedReader(new StringReader(out.toString()));
                AutoCompletionCandidates a = new AutoCompletionCandidates();
                String line;
                while ((line=r.readLine())!=null) {
                    if (line.endsWith("object(s) found."))  continue;
                    a.add(line);
                }
                return a;
            }
            return new AutoCompletionCandidates(); // no candidate
        }

        private synchronized FilePath install(CloudTestServer s) throws IOException, InterruptedException {
            SCommandInstaller sCommandInstaller = new SCommandInstaller(s);
            return sCommandInstaller.scommand(Jenkins.getInstance(), TaskListener.NULL);
        }
    }

    public static class DeleteOldResultsSettings {
        private final int maxDaysOfResults;

        @DataBoundConstructor
        public DeleteOldResultsSettings(int maxDaysOfResults) {
            this.maxDaysOfResults = maxDaysOfResults;
        }

        public int getMaxDaysOfResults() {
            return maxDaysOfResults;
        }
    }
}
