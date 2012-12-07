package com.soasta.jenkins;

import hudson.AbortException;
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
import hudson.tasks.Builder;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCompositionRunner extends Builder {
    /**
     * URL of {@link CloudTestServer}.
     */
    private final String url;

    /**
     * Composition to execute.
     */
    private final String composition;

    @DataBoundConstructor
    public TestCompositionRunner(String url, String composition) {
        this.url = url;
        this.composition = composition;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.get(url);
    }

    public String getUrl() {
        return url;
    }

    public String getComposition() {
        return composition;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        CloudTestServer s = getServer();
        if (s==null)
            throw new AbortException("No TouchTest server is configured in the system configuration.");

        FilePath scommand = new SCommandInstaller(s).scommand(build.getBuiltOn(), listener);

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(scommand)
            .add("cmd=play","wait","format=junitxml")
            .add("name="+composition)
            .add("url=" + s.getUrl())
            .add("username="+s.getUsername())
            .addMasked("password=" + s.getPassword());

        FilePath xml = new FilePath(build.getWorkspace(),composition+".xml");
        int r = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(xml.write()).stderr(listener.getLogger()).join();
        if (r!=0)   return false;

        // archive this result
        JUnitResultArchiver archiver = new JUnitResultArchiver(composition+".xml",true,
                new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP, Collections.singleton(new JunitResultPublisher(null))));
        return archiver.perform(build,launcher,listener);
    }

    @Extension
    public static class DescriptorImpl extends AbstractCloudTestBuilerDescriptor {
        @Override
        public String getDisplayName() {
            return "Execute CloudTest";
        }

        public AutoCompletionCandidates doAutoCompleteComposition(@QueryParameter String url) throws IOException, InterruptedException {
            CloudTestServer s = CloudTestServer.get(url);

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(install(s))
                .add("list","type=composition")
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
}
