package com.soasta.jenkins;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.QuotedStringTokenizer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class iOSAppInstaller extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;
    private final String ipa, additionalOptions;

    @DataBoundConstructor
    public iOSAppInstaller(String url, String ipa, String additionalOptions) {
        this.url = url;
        this.ipa = ipa;
        this.additionalOptions = additionalOptions;
    }

    public String getUrl() {
        return url;
    }

    public String getIpa() {
        return ipa;
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

        CloudTestServer s = getServer();
        if (s==null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath bin = new iOSAppInstallerInstaller(s).ios_app_installer(build.getBuiltOn(), listener);

        args.add(bin)
            .add("--ipa", ipa);
        args.add(new QuotedStringTokenizer(additionalOptions).toArray());

        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(listener).join();
        return r==0;
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilerDescriptor {
        @Override
        public String getDisplayName() {
            return "Install IPA to the device";
        }
    }
}
