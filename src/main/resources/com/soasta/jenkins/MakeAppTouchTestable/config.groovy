/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.MakeAppTouchTestable;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"XCode Project File",field:"projectFile") {
    f.textbox()
}
f.entry(title:"Target",field:"target") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Launch URL (optional)",field:"launchURL") {
        f.textbox()
    }
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
