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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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
     * URL like "http://touchtestlite.soasta.com/concerto/"
     */
    private final String url;

    private final String username;
    private final Secret password;

    private final String id;
    private final String name;
    
    private static final String REPOSITORY_SERVICE_BASE_URL = "/services/rest/RepositoryService/v1/Tokens";

    private transient boolean generatedIdOrName;

    @DataBoundConstructor
    public CloudTestServer(String url, String username, Secret password, String id, String name) throws MalformedURLException {
        if (url == null || url.isEmpty()) {
            // This is not really a valid case, but we have to store something.
            this.url = null;
        }
        else {
            // normalization
            // TODO: can the service be running outside the /concerto/ URL?
            if (!url.endsWith("/")) url+='/';
            if (!url.endsWith("/concerto/"))
                url+="concerto/";
            this.url = url;
        }
        
        if (username == null || username.isEmpty()) {
          this.username = "";
        }
        else {
          this.username = username;
        }
        
        if (password == null || password.getPlainText() == null || password.getPlainText().isEmpty()) {
          this.password = null;
        }
        else {
          this.password = password;
        }

        // If the ID is empty, auto-generate one.
        if (id == null || id.isEmpty()) {
            this.id = UUID.randomUUID().toString();

            // This is probably a configuration created using
            // an older version of the plug-in (before ID and name
            // existed).  Set a flag so we can write the new
            // values after initialization (see DescriptorImpl).
            generatedIdOrName = true;
        }
        else {
            this.id = id;
        }

        // If the name is empty, default to URL + user name.
        if (name == null || name.isEmpty()) {
          if (this.url == null) {
            this.name = "";
          }
          else {
            this.name = url + " (" + username + ")";

            // This is probably a configuration created using
            // an older version of the plug-in (before ID and name
            // existed).  Set a flag so we can write the new
            // values after initialization (see DescriptorImpl).
            generatedIdOrName = true;
          }
        }
        else {
            this.name = name;
        }
    }

    public String getUrl() {
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

    public Object readResolve() throws IOException {
        if (id != null &&
            id.trim().length() > 0 &&
            name != null &&
            name.trim().length() > 0)
            return this;

        // Either the name or ID is missing.
        // This means the config is based an older version the plug-in.

        // The constructor handles this, but XStream doesn't go
        // through the same code path (as far as I can tell).  Instead,
        // we create a new CloudTestServer object, which will include an
        // auto-generated name and ID, and return that instead.

        // When Jenkins is finished loading everything, we'll go back
        // and write the auto-generated values to disk, so this logic
        // should only execute once.  See DescriptorImpl constructor.
        LOGGER.info("Re-creating object to generate a new server ID and name.");
        return new CloudTestServer(url, username, password, id, name);
    }

    public FormValidation validate() throws IOException {
        HttpClient hc = createClient();

        // to validate the credentials we will request a token from the repository. 
        
        JSONObject obj =  new JSONObject();
        obj.put("userName", username);
        obj.put("password", password.getPlainText());
        
        PutMethod put = new PutMethod(url + REPOSITORY_SERVICE_BASE_URL);
        
        StringRequestEntity requestEntity = new StringRequestEntity(
          obj.toString(),
          "application/json",
          "UTF-8");
        
        put.setRequestEntity(requestEntity);
        int statusCode = hc.executeMethod(put);
        LOGGER.info("Status code got back for URL: " + (url + REPOSITORY_SERVICE_BASE_URL) + " STATUS : " + statusCode );
        
        switch (statusCode)
        {
            case 200:
              return FormValidation.ok("Success!");
            case 404:
              return FormValidation.error("[404] Could not find the server");
            case 401:
              return FormValidation.error("[401] Invalid Credentials");
            default: 
              return FormValidation.error("Unknown error, Http Code " + statusCode);
        }
        /* PostMethod post = new PostMethod(url + "Login");
        post.addParameter("userName",getUsername());
        
        if (getPassword() != null) {
          post.addParameter("password",getPassword().getPlainText());
        } else {
          post.addParameter("password","");
        }

        hc.executeMethod(post);

        // if the login succeeds, we'll see a redirect
        Header loc = post.getResponseHeader("Location");
        if (loc!=null && loc.getValue().endsWith("/Central"))
            return FormValidation.ok("Success!");

        if (!post.getResponseBodyAsString().contains("SOASTA"))
            return FormValidation.error(getUrl()+" doesn't look like a CloudTest server");

        // if it fails, the server responds with 200!
        return FormValidation.error("Invalid credentials."); */
    }

    /**
     * Retrieves the build number of this CloudTest server.
     * Postcondition: The build number returned is never null.
     */
    public VersionNumber getBuildNumber() throws IOException {
        if (url == null) {
            // User didn't enter a value in the Configure Jenkins page.
            // Nothing we can do.
            throw new IllegalStateException("No URL has been configured for this CloudTest server.");
        }

        final String[] v = new String[1];
        try {
            HttpClient hc = createClient();
            
            GetMethod get = new GetMethod(url);
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
            if (s.getUrl().equals(url))
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
                // If any of the servers that we loaded was
                // missing a name or ID, and had to auto-generate
                // it, then persist the auto-generated values now.
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

        public FormValidation doCheckName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else if (!isValidURL(value)) {
                return FormValidation.error("Invalid URL syntax (did you mean http://" + value + " ?");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUsername(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        private static boolean isValidURL(String url) {
            try {
                new URL(url);
                return true;
            }
            catch (MalformedURLException e) {
                return false;
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CloudTestServer.class.getName());
}
