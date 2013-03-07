/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.iOSSimulatorLauncher;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"App Directory",field:"app") {
    f.textbox()
}
f.advanced {
    f.entry(title:"SDK version",field:"sdk") {
        f.textbox()
    }
    f.entry(title:"Device family",field:"family") {
        f.select(name:"family")
    }
    f.entry(title:"TouchTest Agent URL",field:"agentURL") {
        f.textbox()
    }
}
