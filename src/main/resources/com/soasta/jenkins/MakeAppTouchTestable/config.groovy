/*
 * Copyright (c) 2012, CloudBees, Inc.
 * Copyright (c) 2012-2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.MakeAppTouchTestable;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"Project Directory",field:"projectFile") {
    f.textbox()
}
f.entry(title:"Target (iOS only)",field:"target") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Launch URL (optional)",field:"launchURL") {
        f.textbox()
    }
    f.entry(title:"Back up modified files",field:"backupModifiedFiles") {
        f.checkbox()
    }
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
