package com.soasta.jenkins;

import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.inject.Inject;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class CloudTestServerTest extends HudsonTestCase {
    @Inject
    CloudTestServer.DescriptorImpl descriptor;
    private CloudTestServer aServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aServer = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def"), "utest", "Unit Test Server");
    }

    public void testValidate() throws Exception {
        FormValidation f = aServer.validate();
        assertThat(f.kind, is(Kind.ERROR));

//        f = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def")).validate();
//        assertThat(f.kind, is(Kind.OK));
    }

    public void testBuildNumber() throws IOException {
        VersionNumber b = aServer.getBuildNumber();
        System.out.println(b);
        assertTrue(b.compareTo(new VersionNumber("5"))>=0);
    }

    public void testMissingID() throws IOException {
        CloudTestServer s = new CloudTestServer("http://foo/", "joe", Secret.fromString("secret"), null, "Name");
        assertNotNull("ID should have been automatically generated.", s.getId());
        assertTrue("ID should be non-empty.", s.getId().trim().length() > 0);
    }

    public void testMissingName() throws IOException {
        CloudTestServer s = new CloudTestServer("http://foo/", "joe", Secret.fromString("secret"), "id", null);
        assertNotNull("Name should have been automatically generated.", s.getName());
        assertEquals("Incorrect name.", s.getUrl() + " (" + s.getUsername() + ")", s.getName());
    }

    public void testConfigRoundtrip() throws Exception {
        jenkins.getInjector().injectMembers(this);
        CloudTestServer before = new CloudTestServer("http://abc/", "def", Secret.fromString("ghi"), "testid", "Test Name");
        descriptor.setServers(Collections.singleton(before));
        configRoundtrip();
        assertEqualDataBoundBeans(before, descriptor.getServers().get(0));
    }
}