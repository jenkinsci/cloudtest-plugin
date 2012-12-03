package com.soasta.jenkins;

import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.inject.Inject;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class CloudTestServerTest extends HudsonTestCase {
    @Inject
    CloudTestServer.DescriptorImpl descriptor;

    public void testValidate() throws Exception {
        FormValidation f = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def")).validate();
        assertThat(f.kind, is(Kind.ERROR));

//        f = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def")).validate();
//        assertThat(f.kind, is(Kind.OK));
    }

    public void testConfigRoundtrip() throws Exception {
        jenkins.getInjector().injectMembers(this);
        CloudTestServer before = new CloudTestServer("http://abc/", "def", Secret.fromString("ghi"));
        descriptor.setServers(Collections.singleton(before));
        configRoundtrip();
        assertEqualDataBoundBeans(descriptor.getServers().get(0),before);
    }
}