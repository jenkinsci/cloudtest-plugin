/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
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

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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

    private final String id;
    private final String name;

    private transient boolean generatedIdOrName;

    @DataBoundConstructor
    public CloudTestServer(String url, String username, Secret password, String id, String name) throws MalformedURLException {
        // normalization
        // TODO: can the service be running outside the /concerto/ URL?
        if (!url.endsWith("/")) url+='/';
        if (!url.endsWith("/concerto/"))
            url+="concerto/";
        this.url = new URL(url);
        this.username = username;
        this.password = password;

        if (id == null || id.isEmpty()) {
            this.id = UUID.randomUUID().toString();
            generatedIdOrName = true;
        }
        else {
            this.id = id;
        }

        if (name == null || name.isEmpty()) {
            this.name = url + " (" + username + ")";
            generatedIdOrName = true;
        }
        else {
            this.name = name;
        }
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean hasGeneratedNameOrId() {
        return generatedIdOrName;
    }

    public Object readResolve() throws IOException {
        if (id != null &&
            id.trim().length() > 0 &&
            name != null &&
            name.trim().length() > 0)
            return this;

        LOGGER.info("Re-creating object to generate a new server ID and name.");

        return new CloudTestServer(url.toExternalForm(), username, password, id, name);
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
     * Postcondition: The build number returned is never null.
     */
    public VersionNumber getBuildNumber() throws IOException {
        final String[] v = new String[1];
        try {
            HttpClient hc = createClient();
            
            GetMethod get = new GetMethod(url.toExternalForm());
            hc.executeMethod(get);
            
            if (get.getStatusCode() != 200) {
                throw new IOException(get.getStatusLine().toString());
            }

            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
            sp.parse(get.getResponseBodyAsStream(), new DefaultHandler() {
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
            LOGGER.warning("Build number not found in " + url);
        } catch (SAXException e) {
            if (v[0] != null)
                return new VersionNumber(v[0]);

            LOGGER.log(Level.WARNING, "Failed to load " + url, e);
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }

        // If we reach this point, then we failed to extract the build number.
        throw new IOException("Failed to extract build number from \'" +
          this.getDescriptor().getDisplayName() + "\': <" + url + ">.");
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
        
        // CloudTest servers will reject the default Java user agent.
        hc.getParams().setParameter(HttpMethodParams.USER_AGENT, "Jenkins/" + Jenkins.getVersion().toString());
        
        return hc;
    }

    public static CloudTestServer getByURL(String url) {
        List<CloudTestServer> servers = Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getServers();
        for (CloudTestServer s : servers) {
            if (s.getUrl().toExternalForm().equals(url))
                return s;
        }
        // if we can't find any, fall back to the default one
        if (!servers.isEmpty())
            return servers.get(0);
        return null;
    }

    public static CloudTestServer getByID(String id) {
        List<CloudTestServer> servers = Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getServers();
        for (CloudTestServer s : servers) {
            if (s.getId().equals(id))
                return s;
        }
        // if we can't find any, fall back to the default one
        if (!servers.isEmpty())
            return servers.get(0);
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CloudTestServer> {

        @CopyOnWrite
        private volatile List<CloudTestServer> servers;

        public DescriptorImpl() {
            load();
            if (servers == null) {
                servers = new ArrayList<CloudTestServer>();
            } else {
                for (CloudTestServer s : servers) {
                    if (s.generatedIdOrName) {
                        LOGGER.info("Persisting generated server IDs and/or names.");
                        save();

                        // Calling save() once covers all servers,
                        // so we can stop looping.
                        break;
                    }
                }
            }
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

        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password, @QueryParameter String id, @QueryParameter String name) throws IOException {
            return new CloudTestServer(url,username,Secret.fromString(password), id, name).validate();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CloudTestServer.class.getName());
}
