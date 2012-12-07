package com.soasta.jenkins;

import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
class SCommandInstallation extends ToolInstallation {
    public SCommandInstallation(String name) {
        super(name, null, new ArrayList<ToolProperty<?>>());
    }
}