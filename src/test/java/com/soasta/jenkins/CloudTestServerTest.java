package com.soasta.jenkins;

import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class CloudTestServerTest {
    @Test
    public void testValidate() throws Exception {
        FormValidation f = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def")).validate();
        assertThat(f.kind, is(Kind.ERROR));

//        f = new CloudTestServer("http://testdrive.soasta.com/", "abc", Secret.fromString("def")).validate();
//        assertThat(f.kind, is(Kind.OK));
    }

    static {
        try {
            Field f = Secret.class.getDeclaredField("SECRET");
            f.setAccessible(true);
            f.set(null,"bogus");
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}