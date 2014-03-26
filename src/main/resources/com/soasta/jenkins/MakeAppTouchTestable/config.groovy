/*
 * Copyright (c) 2012-2014, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.MakeAppTouchTestable;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Input Type",field:"inputType") {
    f.select(name:"inputType")
}
f.entry(title:"Input File",field:"projectFile") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Launch URL (optional)",field:"launchURL") {
        f.textbox()
    }
    f.entry(title:"Target (iOS only)",field:"target") {
        f.textbox()
    }
    f.entry(title:"Back up modified files",field:"backupModifiedFiles") {
        f.checkbox()
    }
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
    f.entry(title:"Java Options",field:"javaOptions") {
        f.expandableTextbox()
    }
}
