package com.soasta.jenkins;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.QuotedStringTokenizer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class MakeAppTouchTestable extends Builder {
    private final String url;
    private final String projectFile,target;
    private final String additionalOptions;

    @DataBoundConstructor
    public MakeAppTouchTestable(String url, String projectFile, String target, String additionalOptions) {
        this.url = url;
        this.projectFile = projectFile;
        this.target = target;
        this.additionalOptions = additionalOptions;
    }

    public String getUrl() {
        return url;
    }

    public String getProjectFile() {
        return projectFile;
    }

    public String getTarget() {
        return target;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.get(url);
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

        FilePath path = new MakeAppTouchTestableInstaller(s).performInstallation(build.getBuiltOn(), listener);

        args.add("-jar").add(path.child("MakeAppTouchTestable.jar"))
            .add("-overwriteapp")
            .add("-project", projectFile)
            .add("-target", target)
            .add("-url").add(s.getUrl())
            .add("-username",s.getUsername())
            .add("-password").addMasked(s.getPassword().getPlainText());
        args.add(new QuotedStringTokenizer(additionalOptions).toArray());

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilerDescriptor {
        @Override
        public String getDisplayName() {
            return "Make App TouchTestable";
        }
    }
}
