/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.jsoup.Jsoup;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.soasta.jenkins.httpclient.GenericSelfClosingHttpClient;
import com.soasta.jenkins.httpclient.HttpClientSettings;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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

    private final String apitoken;
    
    private final String username;
    private final Secret password;

    private final String id;
    private final String name;
    private final String keyStoreLocation;
    private final Secret keyStorePassword;
    private final boolean trustSelfSigned;
    
    private static final String REPOSITORY_SERVICE_BASE_URL = "/services/rest/RepositoryService/v1/Tokens";

    private transient boolean generatedIdOrName;
    
    @DataBoundConstructor
    public CloudTestServer(String url, String username, Secret password, String id, String name, String apitoken, String keyStoreLocation, Secret keyStorePassword, boolean trustSelfSigned) throws MalformedURLException {
        
      this.keyStoreLocation = keyStoreLocation;
      this.keyStorePassword = keyStorePassword;
      this.trustSelfSigned = trustSelfSigned;
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
        
        if (apitoken == null || apitoken.isEmpty()) {
            this.apitoken = "";
        }
        else {
            this.apitoken = apitoken;
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

    public String getKeyStoreLocation()
    {
      return keyStoreLocation;
    }

    public Secret getKeyStorePassword()
    {
      return keyStorePassword;
    }

    public boolean isTrustSelfSigned()
    {
      return trustSelfSigned;
    }

    public String getName() {
        return name;
    }
    
    public String getApitoken() {
        return apitoken;
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
        return new CloudTestServer(url, username, password, id, name, apitoken, keyStoreLocation, keyStorePassword, trustSelfSigned);
    }

    public FormValidation validate() throws IOException {
      try
      {
        GenericSelfClosingHttpClient client = createClient();

        // to validate the credentials we will request a token from the repository. 
        
        JSONObject obj =  new JSONObject();
        
        if(apitoken.trim().isEmpty() && !username.trim().isEmpty() && password != null) {
          obj.put("userName", username);
          obj.put("password", password.getPlainText());
        }
        else if(!apitoken.trim().isEmpty() && username.trim().isEmpty() && password == null) {
            if(apitoken.length() != 36)
              throw new IOException("Invalid API Token");
            else
               obj.put("apiToken", apitoken);
        }
        
        HttpPut put = new HttpPut(url + REPOSITORY_SERVICE_BASE_URL);
        StringEntity jsonEntity = new StringEntity(obj.toString(), "UTF-8");
        jsonEntity.setContentType("application/json");
        put.setEntity(jsonEntity);
        client.sendRequest(put); 
        return FormValidation.ok("Success!");
      }
      catch (Exception e)
      {
        LOGGER.log(Level.SEVERE, "Failed to valdiate",  e);
        return FormValidation.error(e.getMessage());
      } 
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
     

        GenericSelfClosingHttpClient client = createClient();
        
        HttpGet get = new HttpGet(url);
        String responseBody = client.sendRequest(get);
        
        Document doc = Jsoup.parse(responseBody);
        Elements elements = doc.select("meta[name=buildnumber]");
        
        if (elements != null && elements.size() >= 1)
        {
          String buildNumber = elements.get(0).attr("content");
          
          if (buildNumber != null)
          {
            return new VersionNumber(buildNumber);
          }  
        }
        throw new Error("failed to find build number");
    }

    private GenericSelfClosingHttpClient createClient() throws IOException {
        
        return new GenericSelfClosingHttpClient(new HttpClientSettings()
                                                .setKeyStore(HttpClientSettings.loadKeyStore(keyStoreLocation, keyStorePassword.getPlainText()))
                                                .setKeyStorePassword(keyStorePassword == null || keyStorePassword.getPlainText().isEmpty() ?  null : keyStorePassword.getPlainText())
                                                .setUrl(url)
                                                .setTrustSelfSigned(trustSelfSigned)); 
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
        boolean setUsername;
        boolean setApiToken;

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

        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password, @QueryParameter String id, @QueryParameter String name, @QueryParameter String apitoken,
          @QueryParameter String keyStoreLocation, @QueryParameter String keyStorePassword, @QueryParameter boolean trustSelfSigned) throws IOException {
            return new CloudTestServer(url,username,Secret.fromString(password), id, name, apitoken, keyStoreLocation, Secret.fromString(keyStorePassword), trustSelfSigned).validate();
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
            if(setApiToken == true && (value == null || value.trim().isEmpty())) {
                setUsername = false;
                return FormValidation.ok();
            } else if(setApiToken == false && (value == null || value.trim().isEmpty())) {
                setUsername = false;
                return FormValidation.error("Username/Password or API Token Required.");
            } else if(setApiToken == false && (value != null || !(value.trim().isEmpty()))) {
                setUsername = true;
                return FormValidation.ok();
            } else {
                setUsername = true;
                return FormValidation.error("Cannot use both Username/Password and API Token");
            }
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            if(setApiToken == true && setUsername == false && (value == null || value.trim().isEmpty())) {
                return FormValidation.ok();
            } else if(setApiToken == false && setUsername == true && (value == null || value.trim().isEmpty())) {
                return FormValidation.error("Password Required.");
            } else if(setApiToken == false && setUsername == true && (value != null || !(value.trim().isEmpty()))) {
                return FormValidation.ok();
            } else if(setApiToken == false && setUsername == false) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("Cannot use both Username/Password and API Token");
            }
        }
        
        public FormValidation doCheckApitoken(@QueryParameter String value) {
            if(setUsername == true && (value == null || value.trim().isEmpty())) {
                setApiToken = false;
                return FormValidation.ok();
            } else if(setUsername == false && (value == null || value.trim().isEmpty())) {
                setApiToken = false;
                return FormValidation.error("Username/Password or API Token Required.");
            } else if(setUsername == false && (value != null || !(value.trim().isEmpty()))) {
                if(value.length() != 32)
                  return FormValidation.error("Invalid API Token");
                else {
                    setApiToken = true;
                    return FormValidation.ok();
                }
            } else {
                setApiToken = true;
                return FormValidation.error("Cannot use both Username/Password and API Token");
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
