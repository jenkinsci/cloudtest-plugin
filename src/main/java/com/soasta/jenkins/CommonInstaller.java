/*
 * Copyright (c) 2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.net.URL;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.VersionNumber;

/*******************************************************************************************************************************
 * START (Jenkins User-Agent change related imports)
 *******************************************************************************************************************************/
import hudson.Functions;
import hudson.ProxyConfiguration;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import hudson.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
/*******************************************************************************************************************************
 * END (Jenkins User-Agent change related imports)
 *******************************************************************************************************************************/

public class CommonInstaller extends DownloadFromUrlInstaller
{
  private final CloudTestServer server;
  private final VersionNumber buildNumber;
  private final Installers installerType;

  private CommonInstaller(CloudTestServer server, Installers installerType, VersionNumber buildNumber) {
      super(installerType.getCTInstallerType()+buildNumber);
      this.server = server;
      this.installerType = installerType;
      this.buildNumber = buildNumber;
  }

  CommonInstaller(CloudTestServer server, Installers installFileType) throws IOException {
      this(server, installFileType, server.getBuildNumber());
  }

  CloudTestServer getServer() {
    return server;
  }
  
  VersionNumber getBuildNumber() {
    return buildNumber;
  }
  
  Installers getInstallerType() {
    return installerType;
  }
  
  /*******************************************************************************************************************************
   * START (Jenkins User-Agent related code changes)
   *******************************************************************************************************************************/
  /**
   * The following code changes the file download request for installer files 
   * (iOSAppInstaller, MATT, or SCommand) to have a User-Agent of 
   * "Jenkins/<Jenkins version #>" instead of "Java/<Java version #>".
   * This allow this call to correctly identify itself as coming from the Jenkins 
   * plugin.
   * 
   * WARNING: This code was taken from Jenkins (1.544) src, FilePath.java
   * and DownloadFromUrlInstaller.java.
   * A few changes were made to logging and to make the code work.  May be
   * vulnerable to Jenkins base code changes in the future.  Issues related
   * to this may crop up in the future concerning any type of installer 
   * downloading because of this.  This addition is because of Bug 71924.
   * 
   * TODO: Remove code when Jenkins will properly identify itself in the 
   * User-Agent.  All the code below is tied to being able to add to the
   * URLConnection that the User-Agent is, instead, "Jenkins/..." and not
   * "Java/...".
   */
  public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
    try
    {
      return super.performInstallation(tool, node, log);
    }
    catch (IOException e)
    {
      FilePath expected = preferredLocation(tool, node);
      Installable inst = getInstallable();
      
      if(installIfNecessaryFrom(expected, new URL(inst.url), log, "Unpacking " + inst.url + " to " + expected + " on " + node.getDisplayName())) {
        expected.child(".timestamp").delete(); // we don't use the timestamp
        FilePath base = findPullUpDirectory(expected);
        if(base!=null && base!=expected)
            base.moveAllChildrenTo(expected);
        // leave a record for the next up-to-date check
        expected.child(".installedFrom").write(inst.url,"UTF-8");
        expected.act(new ChmodRecAPlusX());
      }
      
      return expected;
    }
  }
  
  /**
   * Sets execute permission on all files, since unzip etc. might not do this.
   * Hackish, is there a better way?
   */
  static class ChmodRecAPlusX implements FileCallable<Void> {
      private static final long serialVersionUID = 1L;
      public Void invoke(File d, VirtualChannel channel) throws IOException {
          if(!Functions.isWindows())
              process(d);
          return null;
      }
      private void process(File f) {
          if (f.isFile()) {
              f.setExecutable(true, false);
          } else {
              File[] kids = f.listFiles();
              if (kids != null) {
                  for (File kid : kids) {
                      process(kid);
                  }
              }
          }
      }
  }
  
  /**
   * (From FilePath.java)
   * Given a zip file, extracts it to the given target directory, if necessary.
   *
   * <p>
   * This method is a convenience method designed for installing a binary package to a location
   * that supports upgrade and downgrade. Specifically,
   *
   * <ul>
   * <li>If the target directory doesn't exist {@linkplain #mkdirs() it'll be created}.
   * <li>If the timestamp left in the directory doesn't match with the timestamp of the current archive file,
   *     the directory contents will be discarded and the archive file will be re-extracted.
   * <li>If the connection is refused but the target directory already exists, it is left alone.
   * </ul>
   *
   * @param archive
   *      The resource that represents the zip file. This URL must support the "Last-Modified" header.
   *      (Most common usage is to get this from {@link ClassLoader#getResource(String)})
   * @param listener
   *      If non-null, a message will be printed to this listener once this method decides to
   *      extract an archive.
   * @return
   *      true if the archive was extracted. false if the extraction was skipped because the target directory
   *      was considered up to date.
   * @since 1.299
   */
   private boolean installIfNecessaryFrom(FilePath expected, URL archive, TaskListener listener, String message) throws IOException, InterruptedException {
      try {
          FilePath timestamp = expected.child(".timestamp");
          URLConnection con;
          try {
              con = ProxyConfiguration.open(archive);
              // Jira Bug JENKINS-21033: Changing the User-Agent from "Java/<Java version #>" to "Jenkins/<Jenkins version #>"
              con.setRequestProperty("User-Agent", "Jenkins/" + Jenkins.getVersion().toString());
              LOGGER.log(Level.INFO, "Setting User-Agent for download to " + con.getRequestProperty("User-Agent") +
                " for file " + archive.getPath());
              if (timestamp.exists()) {
                  con.setIfModifiedSince(timestamp.lastModified());
              }
              con.connect();
          } catch (IOException x) {
              if (expected.exists()) {
                  // Cannot connect now, so assume whatever was last unpacked is still OK.
                  if (listener != null) {
                      LOGGER.log(Level.INFO, "Skipping installation of " + archive + " to " + expected.getRemote() + ": " + x);
                  }
                  return false;
              } else {
                  throw x;
              }
          }

          LOGGER.log(Level.INFO, "Connection response code is: " + ((HttpURLConnection)con).getResponseCode());
          if (con instanceof HttpURLConnection
                  && ((HttpURLConnection)con).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
              return false;
          }
          
          long sourceTimestamp = con.getLastModified();

          if(expected.exists()) {
              LOGGER.log(Level.INFO, "Not creating a new " + expected.getRemote() + " as one already exists.");
              if(timestamp.exists() && sourceTimestamp ==timestamp.lastModified())
                  return false;   // already up to date
              expected.deleteContents();
          } else {
            expected.mkdirs();
          }

          if(listener!=null)
            LOGGER.log(Level.INFO, message);

          if (expected.isRemote()) {
            LOGGER.log(Level.INFO, "Treating this as a remote request.");
            // First try to download from the slave machine.
            try {
                expected.act(new Unpack(archive));
                timestamp.touch(sourceTimestamp);
                return true;
            } catch (IOException x) {
                if (listener != null) {
                    listener.error("Failed to download " + archive + " because of " + x.getLocalizedMessage() + "; will retry.");
                }
            }
          }
          
          expected.act(new Unpack(archive));
          timestamp.touch(sourceTimestamp);
          return true;
      } catch (IOException e) {
          throw new IOException("Failed to install "+archive+" to "+ expected.getRemote(),e);
      }
  }
  
  private static final class Unpack implements FileCallable<Void> {
    private static final long serialVersionUID = 1L;
    
    private final URL archive;
    Unpack(URL archive) {
        this.archive = archive;
    }
    public Void invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
        URLConnection con = archive.openConnection();
        // Jira Bug JENKINS-21033: Changing the User-Agent from "Java/<Java version #>" to "Jenkins/<Jenkins version #>"
        con.setRequestProperty("User-Agent", "Jenkins/" + Jenkins.getVersion().toString());
        InputStream in = con.getInputStream();
        try {
            CountingInputStream cis = new CountingInputStream(in);
            try {
                LOGGER.log(Level.INFO, "Invoke called for Unpack class to unpack to " + dir.getAbsolutePath());
                if (archive.toExternalForm().endsWith(".zip")) {
                  LOGGER.log(Level.INFO, "Archive unzipped as it ends with '.zip'.  Starting unzip.");
                  unzip(dir, cis);
                }
            } catch (IOException x) {
                throw new IOException(String.format("Failed to unpack %s (%d bytes read)", archive, cis.getByteCount()), x);
            }
        } finally {
            in.close();
        }
        return null;
    }
    
    private static void unzip(File dir, InputStream in) throws IOException {
      File tmpFile = File.createTempFile("tmpzip", null); // uses java.io.tmpdir
      try {
          IOUtils.copy(in, tmpFile);
          unzip(dir,tmpFile);
      }
      finally {
          tmpFile.delete();
      }
    }
    
    static private void unzip(File dir, File zipFile) throws IOException {
        dir = dir.getAbsoluteFile();    // without absolutization, getParentFile below seems to fail
        ZipFile zip = new ZipFile(zipFile);
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> entries = zip.getEntries();

        try {
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                File f = new File(dir, e.getName());
                if (e.isDirectory()) {
                    f.mkdirs();
                } else {
                    File p = f.getParentFile();
                    if (p != null) {
                        p.mkdirs();
                    }
                    InputStream input = zip.getInputStream(e);
                    try {
                        IOUtils.copy(input, f);
                    } finally {
                        input.close();
                    }
                    try {
                        FilePath target = new FilePath(f);
                        int mode = e.getUnixMode();
                        if (mode!=0)    // Ant returns 0 if the archive doesn't record the access mode
                            target.chmod(mode);
                    } catch (InterruptedException ex) {
                        LOGGER.log(Level.WARNING, "unable to set permissions", ex);
                    }
                    f.setLastModified(e.getTime());
                }
            }
        } finally {
            zip.close();
        }
    }
  }
  /*******************************************************************************************************************************
   * END (Jenkins User-Agent related code changes)
   *******************************************************************************************************************************/

  private final static Logger LOGGER = Logger.getLogger(CommonInstaller.class.getName());
}
