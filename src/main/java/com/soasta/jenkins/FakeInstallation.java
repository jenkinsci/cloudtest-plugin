package com.soasta.jenkins;

import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
class FakeInstallation extends ToolInstallation {
    public FakeInstallation(String name) {
        super(name, null, new ArrayList<ToolProperty<?>>());
    }
}