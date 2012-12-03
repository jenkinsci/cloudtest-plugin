package com.soasta.jenkins;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Information about a specific CloudTest Server and access credential.
 *
 * @author Kohsuke Kawaguchi
 */
public class CloudTestServer extends AbstractDescribableImpl<CloudTestServer> {
    /**
     * URL like "http://testdrive.soasta.com/concerto/"
     */
    private final URL url;

    private final String username;
    private final Secret password;

    @DataBoundConstructor
    public CloudTestServer(String url, String username, Secret password) throws MalformedURLException {
        // normalization
        // TODO: can the service be running outside the /concerto/ URL?
        if (!url.endsWith("/")) url+='/';
        if (!url.endsWith("/concerto/"))
            url+="concerto/";
        this.url = new URL(url);
        this.username = username;
        this.password = password;
    }

    public URL getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    public FormValidation validate() throws IOException {
        HttpClient hc = createClient();

        PostMethod post = new PostMethod(new URL(getUrl(),"Login").toExternalForm());
        post.addParameter("userName",getUsername());
        post.addParameter("password",getPassword().getPlainText());

        hc.executeMethod(post);

        // if the login succeeds, we'll see a redirect
        Header loc = post.getResponseHeader("Location");
        if (loc!=null && loc.getValue().endsWith("/Central"))
            return FormValidation.ok("OK");

        if (!post.getResponseBodyAsString().contains("SOASTA"))
            return FormValidation.error(getUrl()+" doesn't look like a CloudTest server");

        // if it fails, the server responds with 200!
        return FormValidation.error("Username/password was incorrect");
    }

    private HttpClient createClient() {
        HttpClient hc = new HttpClient();
        Jenkins j = Jenkins.getInstance();
        ProxyConfiguration jpc = j!=null ? j.proxy : null;
        if(jpc != null) {
            hc.getHostConfiguration().setProxy(jpc.name, jpc.port);
            if(jpc.getUserName() != null)
                hc.getState().setProxyCredentials(AuthScope.ANY,new UsernamePasswordCredentials(jpc.getUserName(),jpc.getPassword()));
        }
        return hc;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CloudTestServer> {

        @CopyOnWrite
        private volatile List<CloudTestServer> servers;

        public DescriptorImpl() {
            load();
            if (servers==null)  servers=new ArrayList<CloudTestServer>();
        }

        @Override
        public String getDisplayName() {
            return "CloudTest Server";
        }

        public List<CloudTestServer> getServers() {
            return servers;
        }

        public void setServers(Collection<? extends CloudTestServer> servers) {
            this.servers = new ArrayList<CloudTestServer>(servers);
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this,json);
            return true;
        }

        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password) throws IOException {
            return new CloudTestServer(url,username,Secret.fromString(password)).validate();
        }
    }
}
