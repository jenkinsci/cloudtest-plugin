/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VersionNumber;
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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            return FormValidation.ok("Success!");

        if (!post.getResponseBodyAsString().contains("SOASTA"))
            return FormValidation.error(getUrl()+" doesn't look like a CloudTest server");

        // if it fails, the server responds with 200!
        return FormValidation.error("Invalid credentials.");
    }

    /**
     * Retrieves the build number of this CloudTest server.
     */
    public VersionNumber getBuildNumber() {
        final String[] v = new String[1];
        try {
            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
            sp.parse(ProxyConfiguration.open(url).getInputStream(),new DefaultHandler() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
                    if (systemId.endsWith(".dtd"))
                        return new InputSource(new StringReader(""));
                    return null;
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (qName.equals("meta")) {
                        if ("buildnumber".equals(attributes.getValue("name"))) {
                            v[0] = attributes.getValue("content");
                            throw new SAXException("found");
                        }
                    }
                }
            });
            LOGGER.warning("Build number not found in "+url);
        } catch (SAXException e) {
            if (v[0]!=null)
                return new VersionNumber(v[0]);
            LOGGER.log(Level.WARNING, "Failed to load "+url, e);
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load "+url, e);
        }
        return null;
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

    public static CloudTestServer get(String url) {
        List<CloudTestServer> servers = Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getServers();
        for (CloudTestServer s : servers) {
            if (s.getUrl().toExternalForm().equals(url))
                return s;
        }
        // if we can't find any, fall back to the default one
        if (!servers.isEmpty())     return servers.get(0);
        return null;
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
            setServers(req.bindJSONToList(CloudTestServer.class,json.get("servers")));
            save();
            return true;
        }

        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password) throws IOException {
            return new CloudTestServer(url,username,Secret.fromString(password)).validate();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CloudTestServer.class.getName());
}
