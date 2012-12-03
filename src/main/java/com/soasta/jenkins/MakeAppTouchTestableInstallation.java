package com.soasta.jenkins;

import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
class MakeAppTouchTestableInstallation extends ToolInstallation {
    public MakeAppTouchTestableInstallation(String name) {
        super(name, null, new ArrayList<ToolProperty<?>>());
    }
}
